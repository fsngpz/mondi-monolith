# Profile PATCH API - Optimistic Locking Failure Fix

## Problem Statement

### Error Message
```json
{
    "type": "ObjectOptimisticLockingFailureException",
    "message": "Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect): [com.mondi.machine.accounts.profiles.Profile#2]"
}
```

### Symptoms
- ❌ PATCH `/v1/account/profiles` endpoint fails with `ObjectOptimisticLockingFailureException`
- ❌ **Requires service restart to temporarily fix**
- ❌ Error persists after first failure until restart
- ❌ Issue occurs even without concurrent requests

### Impact
- **Severity:** HIGH
- **User Experience:** Broken - users cannot update their profiles
- **Workaround:** Service restart (temporary, not production-viable)

---

## Root Cause Analysis

### Current Implementation Flow

**File:** `src/main/kotlin/com/mondi/machine/accounts/profiles/ProfileService.kt`

```kotlin
// Line 94-108: patch() method
suspend fun patch(id: Long, request: ProfileRequest, profilePicture: MultipartFile?): Profile {
    val jsonNode = objectMapper.convertValue<JsonNode>(request)
    val profile = get(id)                              // ⚠️ Load #1
    val body = profile.toRequest()
    val reader = objectMapper.readerForUpdating(body)
    val newRequest = jsonNode.uploadProfilePicture(id, profilePicture)
    val mergedInstance = reader.readValue<ProfileRequest>(newRequest)
    return update(id, mergedInstance)                   // ⚠️ Calls update()
}

// Line 65-84: update() method
fun update(id: Long, request: ProfileRequest): Profile {
    requireNotNull(request.name) { "field 'name' cannot be null" }
    val normalizedMobile = MobileNumberValidator.validateAndNormalize(request.mobile)

    val profile = get(id).apply {                       // ⚠️ Load #2 (REDUNDANT!)
        this.name = request.name
        this.profilePictureUrl = request.profilePictureKey
        this.user.mobile = normalizedMobile            // ⚠️ Updates User
        this.user.membershipSince = request.membershipSince
    }

    userRepository.save(profile.user)                   // ⚠️ Save User first (version++)
    return repository.save(profile)                     // ⚠️ Save Profile (STALE VERSION!)
}
```

### The Problems

#### Problem 1: Double Entity Loading
```
patch() method:
  ├─ get(id) at line 98     → Loads Profile with version=N
  └─ update(id) at line 108
      └─ get(id) at line 73 → Loads Profile AGAIN with version=N
```

**Why it's bad:**
- Wastes database queries
- Creates two separate entity instances
- First instance becomes **detached** after second load

#### Problem 2: Detached Entity
```
Profile from patch():98 becomes DETACHED when update():73 loads a new instance
└─> Hibernate loses track of the first instance
    └─> Causes session/cache issues
        └─> Requires restart to clear
```

#### Problem 3: Cascade Version Conflicts

**Entity Relationships:**
```kotlin
@Entity
class Profile(
    @OneToOne
    @MapsId  // ⚠️ Profile.id = User.id
    @JoinColumn(name = "id")
    val user: User
) : AuditableBaseEntity<String>()  // Has @Version field
```

**The Cascade:**
```
1. profile.user.mobile = "new value"     // Modify User
2. userRepository.save(profile.user)      // Save User → version: 5 → 6
3. repository.save(profile)               // Try to save Profile with stale version
   └─> Profile thinks User version is 5
   └─> But User version is actually 6
   └─> OptimisticLockingFailureException!
```

#### Problem 4: Version Field is Immutable
```kotlin
@Entity
abstract class BaseEntity {
    @Version
    val version: Long = 0  // ⚠️ Immutable (val)
}
```

When Hibernate tries to update the version, it may not properly track changes due to the `val` keyword.

---

## Why Restart Fixes It Temporarily

```
Service Restart
  └─> Clears Hibernate Session Cache
      └─> Clears EntityManager Cache
          └─> Removes detached entities
              └─> Fresh database loads work... temporarily
                  └─> Until the issue recurs on next PATCH
```

**This is NOT a real fix** - it's a symptom of cached/detached entities.

---

## Solution Strategy

### Approach 1: Remove Double Loading (Recommended)

**Fix the redundant entity loading by reusing the already-loaded profile.**

#### Implementation

**File:** `src/main/kotlin/com/mondi/machine/accounts/profiles/ProfileService.kt`

```kotlin
/**
 * a function to handle request patch / partial update of instance [Profile].
 *
 * FIXED: Removed double loading of Profile entity.
 * FIXED: Proper entity state management to avoid detached entities.
 *
 * @param id the unique identifier.
 * @param request the [ProfileRequest] of payload.
 * @param profilePicture the profile picture file.
 * @return the [Profile] instance.
 */
@Transactional  // ✅ Add explicit transaction
suspend fun patch(id: Long, request: ProfileRequest, profilePicture: MultipartFile?): Profile {
    // Convert request to JSON for merging
    val jsonNode = objectMapper.convertValue<JsonNode>(request)

    // Load profile ONCE
    val profile = get(id)

    // Convert existing profile to request for merging
    val existingRequest = profile.toRequest()
    val reader = objectMapper.readerForUpdating(existingRequest)

    // Upload profile picture if provided
    val newRequest = jsonNode.uploadProfilePicture(id, profilePicture)

    // Merge the values
    val mergedRequest = reader.readValue<ProfileRequest>(newRequest)

    // ✅ FIX: Update using the already-loaded profile (no re-loading!)
    return updateExistingProfile(profile, mergedRequest)
}

/**
 * Update an already-loaded profile entity.
 * This avoids re-loading and version conflicts.
 *
 * @param profile the already-loaded [Profile] entity.
 * @param request the [ProfileRequest] with updated values.
 * @return the updated [Profile] instance.
 */
private fun updateExistingProfile(profile: Profile, request: ProfileRequest): Profile {
    // Validate required fields
    requireNotNull(request.name) {
        "field 'name' cannot be null"
    }

    // Validate and normalize mobile number
    val normalizedMobile = MobileNumberValidator.validateAndNormalize(request.mobile)

    // Update profile fields
    profile.name = request.name
    profile.profilePictureUrl = request.profilePictureKey

    // Update user fields (within the same entity graph)
    profile.user.mobile = normalizedMobile
    profile.user.membershipSince = request.membershipSince

    // ✅ FIX: Save only profile (cascade will handle user)
    // No need to save user separately - JPA tracks changes
    return repository.save(profile)
}

/**
 * a function to update the instance of [Profile].
 * This is for full updates (PUT), not partial (PATCH).
 *
 * @param id the [Profile] unique identifier.
 * @param request the [ProfileRequest] instance.
 * @return the [Profile] instance.
 */
@Transactional  // ✅ Add explicit transaction
fun update(id: Long, request: ProfileRequest): Profile {
    // Validate the field 'name'
    requireNotNull(request.name) {
        "field 'name' cannot be null"
    }

    // Validate and normalize mobile number
    val normalizedMobile = MobileNumberValidator.validateAndNormalize(request.mobile)

    // Get and update profile
    val profile = get(id)
    profile.name = request.name
    profile.profilePictureUrl = request.profilePictureKey

    // Update user fields
    profile.user.mobile = normalizedMobile
    profile.user.membershipSince = request.membershipSince

    // ✅ FIX: Save only profile (cascade handles user)
    return repository.save(profile)
}
```

**Key Changes:**
1. ✅ Created `updateExistingProfile()` to reuse loaded entity
2. ✅ Removed redundant `get(id)` call in `update()`
3. ✅ Removed `userRepository.save()` - let JPA cascade handle it
4. ✅ Added `@Transactional` annotations
5. ✅ Single save point = single version increment

---

### Approach 2: Fix Version Field Mutability

The `@Version` field should be mutable for proper optimistic locking.

**File:** `src/main/kotlin/com/mondi/machine/utils/BaseEntity.kt`

**Current (Problematic):**
```kotlin
@Version
val version: Long = 0  // ❌ Immutable
```

**Fixed:**
```kotlin
@Version
var version: Long = 0  // ✅ Mutable
```

**Why this matters:**
- Hibernate needs to increment the version on each update
- `val` prevents Hibernate from properly managing the version
- May cause silent failures in version tracking

---

### Approach 3: Proper Entity Relationship Configuration

Ensure Profile and User cascade properly to avoid separate saves.

**File:** `src/main/kotlin/com/mondi/machine/accounts/profiles/Profile.kt`

**Current:**
```kotlin
@OneToOne
@MapsId
@JoinColumn(name = "id")
val user: User
```

**Improved:**
```kotlin
@OneToOne(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
@MapsId
@JoinColumn(name = "id")
val user: User
```

**Benefits:**
- Changes to `profile.user` are automatically cascaded
- No need for separate `userRepository.save()`
- Single transaction = single version increment

---

### Approach 4: Add Pessimistic Locking (If Concurrency Expected)

If you anticipate concurrent profile updates, use pessimistic locking.

**File:** `src/main/kotlin/com/mondi/machine/accounts/profiles/ProfileRepository.kt`

```kotlin
@Repository
interface ProfileRepository : JpaRepository<Profile, Long> {

    /**
     * Find profile with pessimistic write lock.
     * Prevents concurrent modifications.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Profile p WHERE p.id = :id")
    fun findByIdWithLock(id: Long): Profile?
}
```

**Usage:**
```kotlin
@Service
class ProfileService(...) {
    suspend fun patch(id: Long, ...): Profile {
        // Use locked query for concurrent environments
        val profile = repository.findByIdWithLock(id)
            ?: throw NoSuchElementException("Profile not found")

        // ... rest of update logic
    }
}
```

---

## Recommended Fix (Step-by-Step)

### Step 1: Make Version Field Mutable

```kotlin
// src/main/kotlin/com/mondi/machine/utils/BaseEntity.kt
@Version
var version: Long = 0  // Changed from val to var
```

### Step 2: Refactor ProfileService

```kotlin
// src/main/kotlin/com/mondi/machine/accounts/profiles/ProfileService.kt

@Service
class ProfileService(
    private val objectMapper: ObjectMapper,
    private val supabaseService: SupabaseService,
    private val repository: ProfileRepository,
    private val userRepository: UserRepository
) {

    fun get(id: Long): Profile {
        return repository.findByIdOrNull(id) ?: throw NoSuchElementException(
            "no profile was found with id '$id'"
        )
    }

    fun create(user: User, profilePictureUrl: String?): Profile {
        val profile = Profile(user).apply {
            this.profilePictureUrl = profilePictureUrl
        }
        return repository.save(profile)
    }

    @Transactional
    fun update(id: Long, request: ProfileRequest): Profile {
        requireNotNull(request.name) {
            "field 'name' cannot be null"
        }

        val normalizedMobile = MobileNumberValidator.validateAndNormalize(request.mobile)

        val profile = get(id)
        profile.name = request.name
        profile.profilePictureUrl = request.profilePictureKey
        profile.user.mobile = normalizedMobile
        profile.user.membershipSince = request.membershipSince

        // Single save - JPA will cascade to user
        return repository.save(profile)
    }

    @Transactional
    suspend fun patch(id: Long, request: ProfileRequest, profilePicture: MultipartFile?): Profile {
        // Convert request to JSON
        val jsonNode = objectMapper.convertValue<JsonNode>(request)

        // Load profile once
        val profile = get(id)

        // Convert to request for merging
        val existingRequest = profile.toRequest()
        val reader = objectMapper.readerForUpdating(existingRequest)

        // Upload profile picture
        val newRequest = jsonNode.uploadProfilePicture(id, profilePicture)

        // Merge values
        val mergedRequest = reader.readValue<ProfileRequest>(newRequest)

        // Validate
        requireNotNull(mergedRequest.name) {
            "field 'name' cannot be null"
        }

        val normalizedMobile = MobileNumberValidator.validateAndNormalize(mergedRequest.mobile)

        // Update fields
        profile.name = mergedRequest.name
        profile.profilePictureUrl = mergedRequest.profilePictureKey
        profile.user.mobile = normalizedMobile
        profile.user.membershipSince = mergedRequest.membershipSince

        // Single save
        return repository.save(profile)
    }

    private suspend fun JsonNode.uploadProfilePicture(
        id: Long,
        profilePicture: MultipartFile?
    ): JsonNode {
        val extension = profilePicture?.originalFilename?.substringAfterLast('.', "")

        val profilePictureKey = profilePicture?.let {
            supabaseService.uploadFile(
                bucketName = BUCKET_USERS,
                fileName = "/profile-picture/user-${id}.${extension}",
                file = it,
                isOverwriteFile = true
            )
        }

        val nodeRequest = objectMapper.convertValue<ProfileRequest>(this)
        val newRequest = ProfileRequest(
            name = nodeRequest.name,
            profilePictureKey = profilePictureKey,
            mobile = nodeRequest.mobile,
            membershipSince = nodeRequest.membershipSince
        )

        return objectMapper.convertValue<JsonNode>(newRequest)
    }
}
```

### Step 3: Update Profile Entity (Optional but Recommended)

```kotlin
// src/main/kotlin/com/mondi/machine/accounts/profiles/Profile.kt

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "profiles")
class Profile(
    @OneToOne(cascade = [CascadeType.MERGE])  // ✅ Add cascade
    @MapsId
    @JoinColumn(name = "id")
    val user: User
) : AuditableBaseEntity<String>() {
    var name: String? = null
    var profilePictureUrl: String? = null

    @OneToMany(mappedBy = "profile")
    @OnDelete(action = OnDeleteAction.CASCADE)
    var transactions: Set<Transaction> = setOf()
}
```

### Step 4: Add Integration Tests

```kotlin
@SpringBootTest
@Transactional
class ProfileServiceTest {

    @Autowired
    lateinit var profileService: ProfileService

    @Autowired
    lateinit var profileRepository: ProfileRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `patch profile should not cause optimistic locking exception`() = runTest {
        // Given: Create a user and profile
        val user = createTestUser()
        val profile = profileService.create(user, null)

        // When: Patch profile multiple times
        val request1 = ProfileRequest(name = "Name 1")
        val updated1 = profileService.patch(profile.id!!, request1, null)

        val request2 = ProfileRequest(name = "Name 2")
        val updated2 = profileService.patch(profile.id!!, request2, null)

        // Then: Should succeed without exception
        assertThat(updated2.name).isEqualTo("Name 2")
        assertThat(updated2.version).isGreaterThan(updated1.version)
    }

    @Test
    fun `patch profile should properly update user fields`() = runTest {
        // Given
        val user = createTestUser()
        val profile = profileService.create(user, null)

        // When
        val request = ProfileRequest(
            name = "Updated Name",
            mobile = "+1234567890"
        )
        val updated = profileService.patch(profile.id!!, request, null)

        // Then: Both profile and user should be updated
        assertThat(updated.name).isEqualTo("Updated Name")
        assertThat(updated.user.mobile).isEqualTo("+1234567890")

        // Verify in database
        val fromDb = profileRepository.findById(profile.id!!).get()
        assertThat(fromDb.name).isEqualTo("Updated Name")
        assertThat(fromDb.user.mobile).isEqualTo("+1234567890")
    }

    @Test
    fun `update should increment version only once`() {
        // Given
        val user = createTestUser()
        val profile = profileService.create(user, null)
        val initialVersion = profile.version

        // When
        val request = ProfileRequest(name = "Updated")
        val updated = profileService.update(profile.id!!, request)

        // Then: Version should increment by 1, not 2
        assertThat(updated.version).isEqualTo(initialVersion + 1)
    }

    private fun createTestUser(): User {
        val user = User(
            email = "test@example.com",
            password = "password",
            provider = OAuthProvider.LOCAL
        )
        return userRepository.save(user)
    }
}
```

---

## Verification Steps

### 1. Manual Testing

```bash
# Test PATCH endpoint
curl -X PATCH http://localhost:8080/v1/account/profiles \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "name=Test User" \
  -F "mobile=+1234567890"

# Should return 200 OK with updated profile

# Call again immediately (test for detached entity issue)
curl -X PATCH http://localhost:8080/v1/account/profiles \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "name=Test User Updated" \
  -F "mobile=+1234567890"

# Should return 200 OK (not 500 with OptimisticLockingException)
```

### 2. Check Database Version

```sql
-- Before update
SELECT id, name, version FROM profiles WHERE id = 2;
-- Result: id=2, name='Old Name', version=5

-- After PATCH
SELECT id, name, version FROM profiles WHERE id = 2;
-- Result: id=2, name='New Name', version=6 (incremented by 1)

-- After another PATCH
SELECT id, name, version FROM profiles WHERE id = 2;
-- Result: id=2, name='Newer Name', version=7 (incremented by 1)
```

### 3. Monitor Logs

```kotlin
// Add logging to ProfileService
private val logger = LoggerFactory.getLogger(ProfileService::class.java)

@Transactional
suspend fun patch(id: Long, ...): Profile {
    logger.info("PATCH: Loading profile $id")
    val profile = get(id)
    logger.info("PATCH: Loaded profile $id with version ${profile.version}")

    // ... update logic

    val updated = repository.save(profile)
    logger.info("PATCH: Saved profile $id with new version ${updated.version}")
    return updated
}
```

**Expected Log Output:**
```
INFO  - PATCH: Loading profile 2
INFO  - PATCH: Loaded profile 2 with version 5
INFO  - PATCH: Saved profile 2 with new version 6
```

**Problem Indicators:**
```
INFO  - PATCH: Loading profile 2
INFO  - PATCH: Loaded profile 2 with version 5
INFO  - UPDATE: Loading profile 2  // ❌ Double load!
INFO  - UPDATE: Loaded profile 2 with version 5
ERROR - OptimisticLockingFailureException
```

---

## Additional Best Practices

### 1. Use DTOs for Responses

Avoid exposing entities directly to prevent lazy-loading issues.

```kotlin
data class ProfileResponse(
    val id: Long,
    val name: String?,
    val profilePictureUrl: String?,
    val mobile: String?,
    val membershipSince: OffsetDateTime?,
    val version: Long  // ✅ Include version for debugging
)
```

### 2. Add Retry Logic (Optional)

For genuinely concurrent scenarios:

```kotlin
@Retryable(
    value = [ObjectOptimisticLockingFailureException::class],
    maxAttempts = 3,
    backoff = Backoff(delay = 100)
)
@Transactional
suspend fun patch(id: Long, ...): Profile {
    // Implementation
}
```

### 3. Add Pessimistic Locking for Critical Sections

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Profile p WHERE p.id = :id")
fun findByIdWithLock(id: Long): Profile?
```

### 4. Clear Hibernate Cache After Updates

```kotlin
@Autowired
lateinit var entityManager: EntityManager

fun clearCache() {
    entityManager.flush()
    entityManager.clear()
}
```

---

## Summary of Changes

| Issue | Fix | Priority |
|-------|-----|----------|
| Double entity loading | Remove redundant `get()` call | **HIGH** |
| Immutable version field | Change `val version` to `var version` | **HIGH** |
| Separate User save | Remove `userRepository.save()`, rely on cascade | **HIGH** |
| No transaction boundary | Add `@Transactional` annotations | **MEDIUM** |
| Cascade configuration | Add `CascadeType.MERGE` to Profile.user | **MEDIUM** |
| Missing tests | Add integration tests | **HIGH** |

---

## Testing Checklist

- [ ] Update `BaseEntity.version` from `val` to `var`
- [ ] Refactor `ProfileService.patch()` to avoid double loading
- [ ] Remove `userRepository.save()` call in `update()` method
- [ ] Add `@Transactional` to `patch()` and `update()` methods
- [ ] Run integration tests
- [ ] Test PATCH endpoint manually (multiple consecutive calls)
- [ ] Verify version increments correctly in database
- [ ] Check that no `ObjectOptimisticLockingFailureException` occurs
- [ ] Verify service doesn't need restart after updates
- [ ] Load test with concurrent requests

---

## Rollback Plan

If issues arise after deployment:

1. **Immediate:** Revert to previous version
2. **Investigate:** Check Hibernate logs for detached entity warnings
3. **Add logging:** Trace entity loading and version changes
4. **Consider:** Pessimistic locking if concurrency is the real issue

---

## References

- [Hibernate Optimistic Locking](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic)
- [JPA @Version Documentation](https://docs.oracle.com/javaee/7/api/javax/persistence/Version.html)
- [Detached Entities](https://vladmihalcea.com/a-beginners-guide-to-jpa-hibernate-entity-state-transitions/)

---

**Document Version:** 1.0
**Last Updated:** 2026-01-29
**Author:** Ferdinand Sangap
**Status:** Ready for Implementation
