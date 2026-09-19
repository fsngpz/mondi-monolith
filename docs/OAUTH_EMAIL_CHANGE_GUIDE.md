# OAuth User Email Change: Implementation Guide

## Executive Summary

Changing email addresses for OAuth-authenticated users is **possible but complex** and requires careful consideration of security, identity management, and user experience. This document outlines the challenges, standard approaches, and implementation recommendations for the Mondi application.

---

## Current Implementation Analysis

### User Entity Structure

**File:** `src/main/kotlin/com/mondi/machine/auths/users/User.kt`

```kotlin
class User(
    val email: String,              // Immutable (val)
    val password: String?,
    val provider: OAuthProvider,    // LOCAL or GOOGLE
    val providerId: String?         // OAuth provider's user ID
)
```

**Key Observations:**
1. ✅ **Good:** User lookup is by `provider` + `providerId`, not by email (line 115 in GoogleOAuthService.kt)
2. ⚠️ **Challenge:** Email is immutable (`val`), requiring database-level change or entity recreation
3. ✅ **Good:** OAuth and local accounts are separate (prevents conflicts)

### Current OAuth Flow

```
Google Login → Verify Token → Extract email + providerId →
Find by (GOOGLE, providerId) → Return existing user OR create new user
```

**Important:** The system identifies OAuth users by `providerId`, not email. This is the correct approach.

---

## The Core Question: Should OAuth User Emails Be Changeable?

### Option 1: No Email Changes (Recommended for OAuth Users)

**Rationale:**
- OAuth providers (Google, Facebook) manage user identity
- Email is the identity anchor from the OAuth provider
- Changing email could break synchronization with OAuth provider
- If user changes email at Google, they should re-authenticate

**When to Use:**
- Default approach for most applications
- When OAuth provider is the source of truth
- When email changes should be managed by the provider

**Implementation:**
```kotlin
// Prevent email changes for OAuth users
fun updateEmail(userId: String, newEmail: String) {
    val user = getUser(userId)

    require(user.provider == OAuthProvider.LOCAL) {
        "Cannot change email for OAuth accounts. " +
        "Please update your email at your OAuth provider (${user.provider.name})."
    }

    // Proceed with email change for local accounts only
}
```

---

### Option 2: Allow Email Changes with Restrictions (Hybrid Approach)

**Use Cases:**
- User married and changed last name (Gmail address change)
- User switched organizations (corporate email → personal)
- User wants primary contact email different from OAuth email
- OAuth provider doesn't support email changes

**Risks:**
1. **Identity Confusion:** Email no longer matches OAuth provider
2. **Duplicate Accounts:** User logs in with new email from OAuth → creates new account
3. **Security:** Email is often used for account recovery
4. **Audit Trail:** Breaks email-based tracking

---

## Standard Implementation Approaches

### Approach 1: Separate Display Email (Recommended)

Store OAuth email separately and allow a user-editable "contact email".

**Database Schema:**
```sql
-- Migration
ALTER TABLE users
ADD COLUMN oauth_email VARCHAR(255),
ADD COLUMN contact_email VARCHAR(255);

-- Populate oauth_email with existing email for OAuth users
UPDATE users
SET oauth_email = email
WHERE provider != 'LOCAL';

-- Add constraint
ALTER TABLE users
ADD CONSTRAINT check_oauth_email_required
CHECK (
    (provider = 'LOCAL' AND oauth_email IS NULL) OR
    (provider != 'LOCAL' AND oauth_email IS NOT NULL)
);
```

**Updated Entity:**
```kotlin
@Entity
@Table(name = "users")
class User(
    val email: String,                   // For LOCAL: user-chosen email
                                         // For OAUTH: immutable OAuth email
    val password: String?,
    val provider: OAuthProvider,
    val providerId: String?
) : AuditableBaseEntity<String>() {
    var contactEmail: String? = null     // Optional: user-preferred contact email
    var oauthEmail: String? = null       // For audit: original OAuth email

    // Get the email to use for communications
    val primaryEmail: String
        get() = contactEmail ?: email
}
```

**Service Implementation:**
```kotlin
@Service
class UserEmailService(
    private val userRepository: UserRepository
) {

    /**
     * Update contact email for OAuth users.
     * This does NOT change the OAuth identity email.
     */
    @Transactional
    fun updateContactEmail(userId: String, newContactEmail: String): User {
        val user = getUser(userId)

        // Validate email format
        require(isValidEmail(newContactEmail)) {
            "Invalid email format"
        }

        // Check if email is already used
        val existingUser = userRepository.findByEmail(newContactEmail)
        require(existingUser == null || existingUser.id == userId) {
            "Email '$newContactEmail' is already in use"
        }

        // For OAuth users: update contact email only
        if (user.provider != OAuthProvider.LOCAL) {
            user.contactEmail = newContactEmail

            // Log the change for audit
            logger.info(
                "OAuth user ${user.id} changed contact email " +
                "from '${user.email}' to '$newContactEmail'. " +
                "OAuth identity email remains '${user.email}'"
            )
        } else {
            // For LOCAL users: can change primary email
            // Note: This requires changing the immutable field
            throw UnsupportedOperationException(
                "Email change for local users requires different implementation"
            )
        }

        return userRepository.save(user)
    }
}
```

**Pros:**
- ✅ Maintains OAuth identity integrity
- ✅ User can specify preferred contact email
- ✅ No risk of duplicate account creation
- ✅ Clear separation of concerns

**Cons:**
- ⚠️ More complex data model
- ⚠️ Need to update all email-sending logic to use `primaryEmail`

---

### Approach 2: Email Change with Re-verification (Advanced)

Allow email changes but require re-authentication with OAuth provider.

**Implementation Flow:**
```
1. User requests email change
2. System marks account as "pending email verification"
3. User must re-authenticate with OAuth provider
4. System checks if new OAuth token has different email
5. If yes: update email + providerId
6. If no: reject change (OAuth provider hasn't updated)
```

**Service Implementation:**
```kotlin
@Service
class OAuthEmailChangeService(
    private val userRepository: UserRepository,
    private val googleOAuthService: GoogleOAuthService
) {

    @Transactional
    fun initiateEmailChange(userId: String, newEmail: String) {
        val user = getUser(userId)

        require(user.provider != OAuthProvider.LOCAL) {
            "This endpoint is for OAuth users only"
        }

        // Store pending email change request
        user.pendingEmailChange = newEmail
        user.pendingEmailChangeRequestedAt = OffsetDateTime.now()
        userRepository.save(user)

        // Return instruction to user
        throw EmailChangeRequiresReauthException(
            "To change your email, please sign out and sign in again with " +
            "your updated ${user.provider.name} account."
        )
    }

    @Transactional
    fun processEmailChangeOnLogin(
        user: User,
        oauthEmail: String,
        providerId: String
    ): User {
        // Check if there's a pending email change
        if (user.pendingEmailChange != null) {

            // Check if OAuth email matches requested change
            if (oauthEmail == user.pendingEmailChange) {

                // Verify no other user has this email
                val existingUser = userRepository.findByEmail(oauthEmail)
                if (existingUser != null && existingUser.id != user.id) {
                    throw IllegalStateException(
                        "Email '$oauthEmail' is already associated with another account"
                    )
                }

                // Create new user entity with updated email
                // (Required because email is immutable)
                val updatedUser = User(
                    email = oauthEmail,
                    password = null,
                    provider = user.provider,
                    providerId = providerId
                )
                updatedUser.id = user.id
                updatedUser.username = user.username
                updatedUser.mobile = user.mobile
                updatedUser.emailVerifiedAt = OffsetDateTime.now()
                updatedUser.pendingEmailChange = null

                userRepository.save(updatedUser)

                logger.info(
                    "OAuth user ${user.id} successfully changed email " +
                    "from '${user.email}' to '$oauthEmail'"
                )

                return updatedUser
            }
        }

        return user
    }
}
```

**Pros:**
- ✅ Email stays synchronized with OAuth provider
- ✅ Secure (requires OAuth provider verification)
- ✅ True email change (not just contact email)

**Cons:**
- ❌ Complex implementation
- ❌ Requires entity recreation (email is immutable)
- ❌ User must have already changed email at OAuth provider
- ❌ May confuse users

---

### Approach 3: Account Linking/Merging

Allow users to link multiple OAuth accounts or merge accounts.

**Use Case:**
```
User has:
1. Account with old-email@gmail.com (GOOGLE OAuth)
2. Wants to use new-email@gmail.com (different Google account)

Solution: Link both accounts to same user profile
```

**Database Schema:**
```sql
-- New table for linked accounts
CREATE TABLE user_linked_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    linked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_primary BOOLEAN DEFAULT FALSE,
    UNIQUE(provider, provider_id)
);

-- Primary user account remains in users table
-- Additional OAuth accounts go into user_linked_accounts
```

**Implementation:**
```kotlin
@Entity
@Table(name = "user_linked_accounts")
class UserLinkedAccount(
    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,

    @Enumerated(EnumType.STRING)
    val provider: OAuthProvider,

    val providerId: String,
    val email: String,
    var isPrimary: Boolean = false
)

@Service
class AccountLinkingService(
    private val userRepository: UserRepository,
    private val linkedAccountRepository: UserLinkedAccountRepository
) {

    @Transactional
    fun linkOAuthAccount(
        currentUser: User,
        oauthProvider: OAuthProvider,
        oauthProviderId: String,
        oauthEmail: String
    ) {
        // Check if OAuth account is already linked to another user
        val existingLink = linkedAccountRepository
            .findByProviderAndProviderId(oauthProvider, oauthProviderId)

        require(existingLink == null) {
            "This ${oauthProvider.name} account is already linked to another user"
        }

        // Create link
        val linkedAccount = UserLinkedAccount(
            user = currentUser,
            provider = oauthProvider,
            providerId = oauthProviderId,
            email = oauthEmail,
            isPrimary = false
        )

        linkedAccountRepository.save(linkedAccount)

        logger.info(
            "User ${currentUser.id} linked ${oauthProvider.name} " +
            "account with email '$oauthEmail'"
        )
    }

    fun findUserByOAuthAccount(
        provider: OAuthProvider,
        providerId: String
    ): User? {
        // First check main user table
        val mainUser = userRepository.findByProviderAndProviderId(provider, providerId)
        if (mainUser != null) return mainUser

        // Then check linked accounts
        val linkedAccount = linkedAccountRepository
            .findByProviderAndProviderId(provider, providerId)

        return linkedAccount?.user
    }
}
```

**Pros:**
- ✅ User can authenticate with multiple OAuth accounts
- ✅ Solves "new email" problem by linking accounts
- ✅ Maintains OAuth identity integrity
- ✅ Flexible for future multi-provider support

**Cons:**
- ❌ Significant schema changes
- ❌ Complex to implement
- ❌ Need to update OAuth authentication flow

---

## Handling Immutable Email Field

### Current Issue

```kotlin
class User(
    val email: String,  // val = immutable
    ...
)
```

### Solution Options

#### Option A: Make Email Mutable (Breaking Change)

```kotlin
class User(
    var email: String,  // Changed to var
    ...
)
```

**Migration Required:**
```sql
-- No SQL change needed, but application behavior changes
-- Add unique constraint if not exists
ALTER TABLE users
ADD CONSTRAINT users_email_unique UNIQUE (email);
```

**Risks:**
- Changes fundamental assumption about user identity
- Existing code may assume email never changes
- May break foreign key relationships or logs

#### Option B: Entity Recreation Pattern

```kotlin
fun changeEmail(oldUser: User, newEmail: String): User {
    // Create new entity with same ID but new email
    val newUser = User(
        email = newEmail,
        password = oldUser.password,
        provider = oldUser.provider,
        providerId = oldUser.providerId
    )

    // Copy ID to maintain identity
    newUser.id = oldUser.id
    newUser.username = oldUser.username
    newUser.mobile = oldUser.mobile
    newUser.emailVerifiedAt = null  // Require re-verification

    // Copy relationships
    newUser.roles = oldUser.roles
    newUser.profile = oldUser.profile

    return userRepository.save(newUser)
}
```

**Pros:**
- ✅ Works with immutable email
- ✅ JPA will UPDATE existing record (same ID)

**Cons:**
- ⚠️ Detached entity issues
- ⚠️ Must manually copy all fields

#### Option C: Database-Level Update (Bypass JPA)

```kotlin
@Repository
interface UserRepository : JpaRepository<User, Long> {

    @Modifying
    @Query("UPDATE User u SET u.email = :newEmail WHERE u.id = :userId")
    fun updateEmail(userId: Long, newEmail: String): Int
}
```

**Pros:**
- ✅ Simple
- ✅ No entity recreation

**Cons:**
- ⚠️ Bypasses JPA validation
- ⚠️ May cause cache issues
- ⚠️ Still requires `var` in entity or manual flush

---

## Security Considerations

### 1. Email Verification

**Always require verification after email change:**

```kotlin
@Transactional
fun changeEmail(userId: String, newEmail: String) {
    val user = getUser(userId)

    // Change email
    updateUserEmail(user, newEmail)

    // Mark as unverified
    user.emailVerifiedAt = null
    userRepository.save(user)

    // Send verification email
    emailService.sendVerificationEmail(user, newEmail)

    // Optionally: invalidate all sessions
    sessionService.invalidateAllUserSessions(userId)
}
```

### 2. Require Re-authentication

```kotlin
fun changeEmail(
    userId: String,
    newEmail: String,
    currentPassword: String?  // For LOCAL users
) {
    val user = getUser(userId)

    // For LOCAL users: verify password
    if (user.provider == OAuthProvider.LOCAL) {
        require(passwordEncoder.matches(currentPassword, user.password)) {
            "Invalid password. Please verify your identity."
        }
    } else {
        // For OAuth users: require recent OAuth login
        val lastOAuthLogin = getLastOAuthLoginTime(userId)
        require(lastOAuthLogin.isAfter(OffsetDateTime.now().minusMinutes(5))) {
            "Please re-authenticate with ${user.provider.name} to change your email"
        }
    }

    // Proceed with email change
    updateUserEmail(user, newEmail)
}
```

### 3. Prevent Email Hijacking

```kotlin
fun changeEmail(userId: String, newEmail: String) {
    // Check if new email is already in use
    val existingUser = userRepository.findByEmail(newEmail)

    if (existingUser != null) {
        if (existingUser.id == userId) {
            throw IllegalArgumentException("This is already your email")
        }

        // Security: Don't reveal if email exists
        throw IllegalArgumentException(
            "Email change failed. Please contact support."
        )
    }

    // Check if email was recently changed by this user
    val recentChanges = emailChangeHistoryRepository
        .findByUserIdAndChangedAtAfter(
            userId,
            OffsetDateTime.now().minusDays(30)
        )

    require(recentChanges.size < 3) {
        "Too many email changes in the past 30 days. " +
        "Please contact support."
    }

    // Proceed with change
    updateUserEmail(user, newEmail)
}
```

### 4. Audit Trail

```kotlin
@Entity
@Table(name = "email_change_history")
class EmailChangeHistory(
    @ManyToOne
    val user: User,
    val oldEmail: String,
    val newEmail: String,
    val changedAt: OffsetDateTime = OffsetDateTime.now(),
    val changedByIp: String?,
    val reason: String?
)

@Service
class EmailChangeService {
    @Transactional
    fun changeEmailWithAudit(
        userId: String,
        newEmail: String,
        ipAddress: String?
    ) {
        val user = getUser(userId)
        val oldEmail = user.email

        // Perform change
        updateUserEmail(user, newEmail)

        // Record in audit log
        val auditEntry = EmailChangeHistory(
            user = user,
            oldEmail = oldEmail,
            newEmail = newEmail,
            changedByIp = ipAddress,
            reason = "User-initiated"
        )
        emailChangeHistoryRepository.save(auditEntry)

        // Send notification to old email
        emailService.sendEmailChangeNotification(oldEmail, newEmail)
    }
}
```

---

## Recommended Implementation for Mondi

### Strategy: Hybrid Approach

Based on the current codebase analysis, here's the recommended approach:

1. **For OAuth Users (GOOGLE):**
   - Add `contactEmail` field for user-preferred email
   - Keep `email` field as immutable OAuth email
   - Use `contactEmail` for all communications if set
   - Block direct `email` field changes

2. **For Local Users (LOCAL):**
   - Allow email changes with password verification
   - Require email re-verification
   - Invalidate sessions after change

### Implementation Steps

#### Step 1: Database Migration

```sql
-- Add contact email field
ALTER TABLE users
ADD COLUMN contact_email VARCHAR(255),
ADD COLUMN pending_email_change VARCHAR(255),
ADD COLUMN pending_email_change_requested_at TIMESTAMP WITH TIME ZONE;

-- Create audit table
CREATE TABLE email_change_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    old_email VARCHAR(255) NOT NULL,
    new_email VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    changed_by_ip VARCHAR(45),
    reason VARCHAR(255),
    success BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_email_change_history_user_id ON email_change_history(user_id);
CREATE INDEX idx_email_change_history_changed_at ON email_change_history(changed_at);
```

#### Step 2: Update User Entity

```kotlin
@Entity
@Table(name = "users")
class User(
    val email: String,  // Keep immutable
    val password: String?,
    val provider: OAuthProvider = OAuthProvider.LOCAL,
    val providerId: String? = null
) : AuditableBaseEntity<String>() {
    var username: String? = null
    var mobile: String? = null
    var contactEmail: String? = null  // NEW
    var pendingEmailChange: String? = null  // NEW
    var pendingEmailChangeRequestedAt: OffsetDateTime? = null  // NEW

    var membershipSince: OffsetDateTime? = null
    var emailVerifiedAt: OffsetDateTime? = null

    /**
     * Get the primary email for communications.
     * For OAuth users: returns contactEmail if set, otherwise OAuth email.
     * For LOCAL users: returns user's registered email.
     */
    val primaryEmail: String
        get() = when (provider) {
            OAuthProvider.LOCAL -> email
            else -> contactEmail ?: email
        }

    /**
     * Check if user can change their primary email.
     * OAuth users cannot change their identity email.
     */
    val canChangeIdentityEmail: Boolean
        get() = provider == OAuthProvider.LOCAL
}
```

#### Step 3: Create Email Change Service

```kotlin
@Service
class EmailChangeService(
    private val userRepository: UserRepository,
    private val emailChangeHistoryRepository: EmailChangeHistoryRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(EmailChangeService::class.java)

    /**
     * Update contact email for OAuth users.
     * This does NOT change the OAuth identity.
     */
    @Transactional
    fun updateContactEmail(
        userId: String,
        newContactEmail: String,
        ipAddress: String?
    ): User {
        val user = userRepository.findById(userId.toLong())
            .orElseThrow { NoSuchElementException("User not found") }

        require(user.provider != OAuthProvider.LOCAL) {
            "This method is for OAuth users only. " +
            "Use changeIdentityEmail() for local accounts."
        }

        validateEmail(newContactEmail)
        checkEmailNotInUse(newContactEmail, userId)

        val oldContactEmail = user.contactEmail ?: user.email
        user.contactEmail = newContactEmail

        val updatedUser = userRepository.save(user)

        // Audit log
        recordEmailChange(
            user = user,
            oldEmail = oldContactEmail,
            newEmail = newContactEmail,
            ipAddress = ipAddress,
            reason = "Contact email update (OAuth user)"
        )

        // Notify user
        emailService.sendContactEmailChangedNotification(oldContactEmail, newContactEmail)

        logger.info(
            "OAuth user ${user.id} updated contact email " +
            "from '$oldContactEmail' to '$newContactEmail'"
        )

        return updatedUser
    }

    /**
     * Change identity email for LOCAL users only.
     * Requires password verification and email re-verification.
     */
    @Transactional
    fun changeIdentityEmail(
        userId: String,
        newEmail: String,
        currentPassword: String,
        ipAddress: String?
    ): User {
        val user = userRepository.findById(userId.toLong())
            .orElseThrow { NoSuchElementException("User not found") }

        require(user.provider == OAuthProvider.LOCAL) {
            "Cannot change identity email for OAuth users. " +
            "Use updateContactEmail() instead."
        }

        // Verify password
        require(passwordEncoder.matches(currentPassword, user.password)) {
            "Invalid password"
        }

        validateEmail(newEmail)
        checkEmailNotInUse(newEmail, userId)
        checkRateLimiting(userId)

        val oldEmail = user.email

        // Mark email as unverified
        user.emailVerifiedAt = null

        // Use database-level update to change immutable field
        userRepository.updateEmailDirectly(userId.toLong(), newEmail)

        // Audit log
        recordEmailChange(
            user = user,
            oldEmail = oldEmail,
            newEmail = newEmail,
            ipAddress = ipAddress,
            reason = "Identity email change (LOCAL user)"
        )

        // Send verification email to new address
        emailService.sendVerificationEmail(newEmail, user)

        // Notify old email
        emailService.sendIdentityEmailChangedNotification(oldEmail, newEmail)

        logger.info(
            "LOCAL user ${user.id} changed identity email " +
            "from '$oldEmail' to '$newEmail'"
        )

        // Refresh entity
        return userRepository.findById(userId.toLong()).get()
    }

    private fun validateEmail(email: String) {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        require(emailRegex.matches(email)) {
            "Invalid email format"
        }
    }

    private fun checkEmailNotInUse(email: String, currentUserId: String) {
        val existingUser = userRepository.findByEmail(email)
        require(existingUser == null || existingUser.id.toString() == currentUserId) {
            "Email already in use"
        }
    }

    private fun checkRateLimiting(userId: String) {
        val recentChanges = emailChangeHistoryRepository
            .findByUserIdAndChangedAtAfter(
                userId.toLong(),
                OffsetDateTime.now().minusDays(30)
            )

        require(recentChanges.size < 3) {
            "Too many email changes in the past 30 days"
        }
    }

    private fun recordEmailChange(
        user: User,
        oldEmail: String,
        newEmail: String,
        ipAddress: String?,
        reason: String
    ) {
        val history = EmailChangeHistory(
            user = user,
            oldEmail = oldEmail,
            newEmail = newEmail,
            changedByIp = ipAddress,
            reason = reason
        )
        emailChangeHistoryRepository.save(history)
    }
}
```

#### Step 4: Add Repository Method

```kotlin
@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): User?
    fun findByProviderAndProviderId(provider: OAuthProvider, providerId: String): User?

    /**
     * Update email directly at database level.
     * Required because email field is immutable (val).
     */
    @Modifying
    @Query("UPDATE User u SET u.email = :newEmail WHERE u.id = :userId")
    fun updateEmailDirectly(userId: Long, newEmail: String)
}
```

#### Step 5: Create REST Endpoints

```kotlin
@RestController
@RequestMapping("/v1/users/me/email")
class EmailChangeController(
    private val emailChangeService: EmailChangeService
) {

    /**
     * Update contact email (OAuth users only).
     */
    @PutMapping("/contact")
    fun updateContactEmail(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody @Valid request: UpdateContactEmailRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<EmailChangeResponse> {
        val user = emailChangeService.updateContactEmail(
            userId = userDetails.user.id.toString(),
            newContactEmail = request.contactEmail,
            ipAddress = httpRequest.remoteAddr
        )

        return ResponseEntity.ok(EmailChangeResponse(
            success = true,
            message = "Contact email updated successfully",
            newEmail = user.contactEmail,
            requiresVerification = false
        ))
    }

    /**
     * Change identity email (LOCAL users only).
     */
    @PutMapping("/identity")
    fun changeIdentityEmail(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody @Valid request: ChangeIdentityEmailRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<EmailChangeResponse> {
        val user = emailChangeService.changeIdentityEmail(
            userId = userDetails.user.id.toString(),
            newEmail = request.newEmail,
            currentPassword = request.currentPassword,
            ipAddress = httpRequest.remoteAddr
        )

        return ResponseEntity.ok(EmailChangeResponse(
            success = true,
            message = "Email changed successfully. Please verify your new email.",
            newEmail = user.email,
            requiresVerification = true
        ))
    }
}

data class UpdateContactEmailRequest(
    @field:Email
    val contactEmail: String
)

data class ChangeIdentityEmailRequest(
    @field:Email
    val newEmail: String,

    @field:NotBlank
    val currentPassword: String
)

data class EmailChangeResponse(
    val success: Boolean,
    val message: String,
    val newEmail: String?,
    val requiresVerification: Boolean
)
```

---

## Testing Strategy

### Unit Tests

```kotlin
@Test
fun `OAuth user can update contact email`() {
    // Given
    val oauthUser = createOAuthUser(
        email = "user@gmail.com",
        provider = OAuthProvider.GOOGLE
    )

    // When
    val result = emailChangeService.updateContactEmail(
        userId = oauthUser.id.toString(),
        newContactEmail = "preferred@example.com",
        ipAddress = "127.0.0.1"
    )

    // Then
    assertThat(result.email).isEqualTo("user@gmail.com")  // Identity unchanged
    assertThat(result.contactEmail).isEqualTo("preferred@example.com")
    assertThat(result.primaryEmail).isEqualTo("preferred@example.com")
}

@Test
fun `OAuth user cannot change identity email`() {
    // Given
    val oauthUser = createOAuthUser(provider = OAuthProvider.GOOGLE)

    // When/Then
    assertThrows<IllegalArgumentException> {
        emailChangeService.changeIdentityEmail(
            userId = oauthUser.id.toString(),
            newEmail = "new@example.com",
            currentPassword = "password",
            ipAddress = "127.0.0.1"
        )
    }
}

@Test
fun `LOCAL user can change identity email with valid password`() {
    // Given
    val localUser = createLocalUser(
        email = "old@example.com",
        password = "password123"
    )

    // When
    val result = emailChangeService.changeIdentityEmail(
        userId = localUser.id.toString(),
        newEmail = "new@example.com",
        currentPassword = "password123",
        ipAddress = "127.0.0.1"
    )

    // Then
    assertThat(result.email).isEqualTo("new@example.com")
    assertThat(result.emailVerifiedAt).isNull()  // Requires re-verification
}

@Test
fun `email change requires password for LOCAL users`() {
    // Given
    val localUser = createLocalUser(password = "correctPassword")

    // When/Then
    assertThrows<IllegalArgumentException> {
        emailChangeService.changeIdentityEmail(
            userId = localUser.id.toString(),
            newEmail = "new@example.com",
            currentPassword = "wrongPassword",
            ipAddress = "127.0.0.1"
        )
    }
}

@Test
fun `cannot change email to already used address`() {
    // Given
    val user1 = createUser(email = "existing@example.com")
    val user2 = createUser(email = "other@example.com")

    // When/Then
    assertThrows<IllegalArgumentException> {
        emailChangeService.updateContactEmail(
            userId = user2.id.toString(),
            newContactEmail = "existing@example.com",
            ipAddress = "127.0.0.1"
        )
    }
}

@Test
fun `rate limiting prevents too many email changes`() {
    // Given
    val user = createLocalUser()
    repeat(3) {
        emailChangeService.changeIdentityEmail(
            userId = user.id.toString(),
            newEmail = "email$it@example.com",
            currentPassword = "password",
            ipAddress = "127.0.0.1"
        )
    }

    // When/Then
    assertThrows<IllegalArgumentException> {
        emailChangeService.changeIdentityEmail(
            userId = user.id.toString(),
            newEmail = "email4@example.com",
            currentPassword = "password",
            ipAddress = "127.0.0.1"
        )
    }
}
```

---

## User Communication

### Email Templates

#### 1. Contact Email Changed (OAuth Users)

```
Subject: Your Mondi contact email has been updated

Hi [Username],

Your contact email for communications from Mondi has been changed to:
    [New Contact Email]

Your Google sign-in email remains: [OAuth Email]

If you did not make this change, please contact our support team immediately.

Best regards,
The Mondi Team
```

#### 2. Identity Email Changed (Local Users)

```
Subject: Your Mondi email address has been changed

Hi [Username],

Your Mondi account email has been changed from:
    [Old Email]
to:
    [New Email]

For security purposes, you must verify your new email address:
    [Verification Link]

If you did not make this change, please contact support immediately.

Best regards,
The Mondi Team
```

#### 3. Notification to Old Email

```
Subject: [Action Required] Your Mondi email was changed

Hi [Username],

Your Mondi account email was recently changed. If this was you, you can ignore this message.

If you did NOT make this change, your account may be compromised. Please:
1. Contact our support team immediately
2. Reset your password if possible

Date of change: [Timestamp]
IP Address: [IP]

Best regards,
The Mondi Team
```

---

## Conclusion

### Recommended Approach for Mondi

**For OAuth Users (GOOGLE):**
- ✅ Implement `contactEmail` field for user-preferred communications
- ✅ Keep OAuth `email` immutable as identity anchor
- ❌ Do NOT allow changing OAuth identity email

**For Local Users (LOCAL):**
- ✅ Allow email changes with password verification
- ✅ Require email re-verification
- ✅ Implement rate limiting (max 3 changes per 30 days)
- ✅ Maintain audit trail

**Key Principles:**
1. OAuth provider is source of truth for OAuth users
2. Always verify email changes
3. Maintain comprehensive audit trail
4. Send notifications to both old and new emails
5. Require re-authentication for sensitive changes

**Next Steps:**
1. Review and approve this approach
2. Create database migration (Step 1)
3. Update User entity (Step 2)
4. Implement EmailChangeService (Step 3)
5. Add REST endpoints (Step 5)
6. Write comprehensive tests
7. Deploy to staging
8. User acceptance testing

---

**Document Version:** 1.0
**Last Updated:** 2026-01-29
**Author:** Ferdinand Sangap
**Status:** Pending Review
