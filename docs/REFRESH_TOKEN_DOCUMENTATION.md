# Refresh Token Documentation

This document explains the refresh token functionality implemented in the Mondi jewelry store application.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Database Schema](#database-schema)
4. [API Endpoints](#api-endpoints)
5. [Token Lifecycle](#token-lifecycle)
6. [Client Integration](#client-integration)
7. [Security Considerations](#security-considerations)
8. [Testing](#testing)

## Overview

The application now supports refresh tokens for both local authentication (email/password) and Google OAuth authentication. Refresh tokens provide a secure way to obtain new access tokens without requiring users to re-authenticate.

### Key Features

- Automatic refresh token generation for all sign-in methods
- Refresh token rotation (new token issued on each refresh)
- 7-day refresh token validity period
- 30-minute access token validity period
- Automatic token revocation
- Support for revoking all user tokens (useful for logout from all devices)

## Architecture

### Core Components

1. **RefreshToken Entity**: Database model for storing refresh tokens
2. **RefreshTokenRepository**: Data access layer for refresh tokens
3. **RefreshTokenService**: Business logic for token generation, validation, and revocation
4. **RefreshTokenController**: REST API endpoint for token refresh
5. **Updated Authentication Services**: Both `AuthenticationService` and `GoogleOAuthService` now generate refresh tokens

### Token Flow

```
1. User authenticates (email/password or Google OAuth)
2. Backend generates:
   - Access token (JWT, expires in 30 minutes)
   - Refresh token (UUID, expires in 7 days)
3. Frontend stores both tokens securely
4. When access token expires:
   a. Frontend sends refresh token to /v1/auth/refresh
   b. Backend validates refresh token
   c. Backend generates new access token
   d. Backend rotates refresh token (revokes old, generates new)
   e. Backend returns both new tokens
5. Frontend updates stored tokens
```

## Database Schema

### Refresh Tokens Table

The refresh token functionality adds a new `refresh_tokens` table:

```sql
CREATE TABLE refresh_tokens
(
    id              BIGSERIAL PRIMARY KEY,
    token           text NOT NULL UNIQUE,
    user_id         bigint NOT NULL REFERENCES users ON DELETE CASCADE,
    expires_at      timestamp WITH TIME ZONE NOT NULL,
    revoked_at      timestamp WITH TIME ZONE,
    created_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id      text NOT NULL,
    updated_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id      text NOT NULL,
    version         bigint DEFAULT 0 NOT NULL
);
```

### Fields Description

| Field | Type | Description |
|-------|------|-------------|
| `id` | `bigserial` | Primary key |
| `token` | `text` | Unique refresh token string (UUID) |
| `user_id` | `bigint` | Foreign key to users table |
| `expires_at` | `timestamp` | Token expiration timestamp (7 days from creation) |
| `revoked_at` | `timestamp` | Token revocation timestamp (null if not revoked) |

### Indexes

- Unique index on `token` for fast lookup
- Index on `user_id` for finding all user tokens
- Index on `expires_at` for cleanup queries

## API Endpoints

### 1. Login with Email/Password

**Endpoint:** `POST /v1/auth/login`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "bearerToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 2. Login with Google OAuth

**Endpoint:** `POST /v1/auth/oauth/google`

**Request:**
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE4MmU0M..."
}
```

**Response:**
```json
{
  "bearerToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 3. Refresh Access Token

**Endpoint:** `POST /v1/auth/refresh`

**Description:** Exchange a refresh token for a new access token and refresh token.

**Request:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (Success - 200 OK):**
```json
{
  "bearerToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "660e8400-e29b-41d4-a716-446655440111"
}
```

**Error Responses:**

**400 Bad Request** - Invalid, expired, or revoked refresh token
```json
{
  "message": "Invalid refresh token"
}
```

```json
{
  "message": "Refresh token has expired"
}
```

```json
{
  "message": "Refresh token has been revoked"
}
```

## Token Lifecycle

### Access Token

- **Type**: JWT (JSON Web Token)
- **Validity**: 30 minutes
- **Storage**: Memory or secure storage (not localStorage)
- **Usage**: Include in `Authorization` header for API requests
- **Format**: `Bearer {access_token}`

### Refresh Token

- **Type**: UUID (Universally Unique Identifier)
- **Validity**: 7 days
- **Storage**: Secure HTTP-only cookie or secure storage
- **Usage**: Send to `/v1/auth/refresh` when access token expires
- **Rotation**: New refresh token issued on each refresh

### Token States

1. **Active**: Token is valid and can be used
2. **Expired**: Token has passed its expiration time
3. **Revoked**: Token has been explicitly revoked (logout, security event)

## Client Integration

### Web Application (React/Vue/Angular)

#### Token Storage

```javascript
// Store tokens securely
const storeTokens = (bearerToken, refreshToken) => {
  // Store access token in memory (preferred) or sessionStorage
  sessionStorage.setItem('accessToken', bearerToken);

  // Store refresh token in HTTP-only cookie (recommended)
  // Or use secure storage
  localStorage.setItem('refreshToken', refreshToken);
};
```

#### Automatic Token Refresh

```javascript
// Axios interceptor for automatic token refresh
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/v1'
});

// Request interceptor - add access token
api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If 401 and haven't retried yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Get refresh token
        const refreshToken = localStorage.getItem('refreshToken');

        // Request new tokens
        const response = await axios.post(
          'http://localhost:8080/v1/auth/refresh',
          { refreshToken }
        );

        const { bearerToken, refreshToken: newRefreshToken } = response.data;

        // Store new tokens
        storeTokens(bearerToken, newRefreshToken);

        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${bearerToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed - redirect to login
        localStorage.removeItem('refreshToken');
        sessionStorage.removeItem('accessToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

#### Login Flow

```javascript
const login = async (email, password) => {
  try {
    const response = await axios.post('http://localhost:8080/v1/auth/login', {
      email,
      password
    });

    const { bearerToken, refreshToken } = response.data;

    // Store tokens
    storeTokens(bearerToken, refreshToken);

    // Redirect to dashboard
    window.location.href = '/dashboard';
  } catch (error) {
    console.error('Login failed:', error);
  }
};
```

#### Logout Flow

```javascript
const logout = () => {
  // Clear tokens
  localStorage.removeItem('refreshToken');
  sessionStorage.removeItem('accessToken');

  // Redirect to login
  window.location.href = '/login';
};
```

### Mobile Application (Android/iOS)

#### Android (Kotlin)

```kotlin
// Token storage using SharedPreferences
class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}

// Retrofit interceptor for automatic refresh
class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val api: AuthApi
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenManager.getAccessToken()

        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(authenticatedRequest)

        // Handle 401 - token expired
        if (response.code == 401) {
            response.close()

            // Try to refresh token
            val refreshToken = tokenManager.getRefreshToken() ?: return response

            val refreshResponse = api.refreshToken(
                RefreshTokenRequest(refreshToken)
            ).execute()

            if (refreshResponse.isSuccessful) {
                val tokens = refreshResponse.body()!!
                tokenManager.saveTokens(tokens.bearerToken, tokens.refreshToken)

                // Retry original request
                val newRequest = request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.bearerToken}")
                    .build()

                return chain.proceed(newRequest)
            } else {
                // Refresh failed - logout
                tokenManager.clearTokens()
            }
        }

        return response
    }
}
```

#### iOS (Swift)

```swift
// Token storage using Keychain
class TokenManager {
    func saveTokens(accessToken: String, refreshToken: String) {
        KeychainHelper.save(key: "access_token", data: accessToken)
        KeychainHelper.save(key: "refresh_token", data: refreshToken)
    }

    func getAccessToken() -> String? {
        return KeychainHelper.load(key: "access_token")
    }

    func getRefreshToken() -> String? {
        return KeychainHelper.load(key: "refresh_token")
    }

    func clearTokens() {
        KeychainHelper.delete(key: "access_token")
        KeychainHelper.delete(key: "refresh_token")
    }
}

// URLSession interceptor for automatic refresh
class AuthInterceptor {
    let tokenManager = TokenManager()

    func makeRequest(url: URL, completion: @escaping (Data?, Error?) -> Void) {
        var request = URLRequest(url: url)

        if let token = tokenManager.getAccessToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let httpResponse = response as? HTTPURLResponse,
               httpResponse.statusCode == 401 {
                // Token expired - try to refresh
                self.refreshToken { success in
                    if success {
                        // Retry original request
                        self.makeRequest(url: url, completion: completion)
                    } else {
                        // Refresh failed - logout
                        self.tokenManager.clearTokens()
                        completion(nil, NSError(domain: "Auth", code: 401))
                    }
                }
            } else {
                completion(data, error)
            }
        }.resume()
    }

    private func refreshToken(completion: @escaping (Bool) -> Void) {
        guard let refreshToken = tokenManager.getRefreshToken() else {
            completion(false)
            return
        }

        let url = URL(string: "http://localhost:8080/v1/auth/refresh")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body = ["refreshToken": refreshToken]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let data = data,
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: String],
               let accessToken = json["bearerToken"],
               let refreshToken = json["refreshToken"] {
                self.tokenManager.saveTokens(
                    accessToken: accessToken,
                    refreshToken: refreshToken
                )
                completion(true)
            } else {
                completion(false)
            }
        }.resume()
    }
}
```

## Security Considerations

### Best Practices

1. **Token Storage**
   - **Access Token**: Store in memory or sessionStorage (never localStorage)
   - **Refresh Token**: Store in HTTP-only cookies or secure storage
   - Never expose tokens in URLs or logs

2. **Token Rotation**
   - Refresh tokens are automatically rotated on each use
   - Old refresh token is revoked when new one is issued
   - Prevents token reuse attacks

3. **Token Expiration**
   - Access tokens expire in 30 minutes
   - Refresh tokens expire in 7 days
   - Expired tokens cannot be used

4. **HTTPS Only**
   - Always use HTTPS in production
   - Prevents token interception

5. **Token Revocation**
   - Implement logout to revoke refresh tokens
   - Consider revoking all user tokens on password change
   - Monitor for suspicious token usage

### Security Features

- **Unique Token Generation**: UUIDs prevent token prediction
- **Database Storage**: Tokens stored securely in database
- **Automatic Cleanup**: Expired tokens can be purged
- **Audit Trail**: Created/updated timestamps for tracking
- **Cascade Deletion**: Tokens deleted when user is deleted

### Recommended: Logout Endpoint

Consider implementing a logout endpoint that revokes refresh tokens:

```kotlin
@PostMapping("/v1/auth/logout")
fun logout(@RequestHeader("Authorization") authHeader: String) {
    // Extract user from access token
    val token = authHeader.replace("Bearer ", "")
    val email = jwtService.extractEmail(token)
    val user = userRepository.findByEmail(email)

    // Revoke all user's refresh tokens
    if (user != null) {
        refreshTokenService.revokeAllUserTokens(user)
    }

    return ResponseEntity.ok().build()
}
```

## Testing

### Unit Tests

Run the refresh token service tests:

```bash
./gradlew test --tests "com.mondi.machine.auths.refresh.RefreshTokenServiceTest"
```

### Integration Testing

1. **Login and Get Tokens**
```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

2. **Use Access Token**
```bash
curl -X GET http://localhost:8080/v1/account/profiles \
  -H "Authorization: Bearer {access_token}"
```

3. **Refresh Tokens**
```bash
curl -X POST http://localhost:8080/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"{refresh_token}"}'
```

4. **Test Expired Token**
- Wait for access token to expire (30 minutes)
- Try to use expired access token (should return 401)
- Use refresh token to get new access token
- Retry request with new access token

### Manual Testing Scenarios

1. **Normal Flow**: Login → Use API → Refresh → Use API
2. **Expired Access Token**: Login → Wait 30 min → Refresh → Use API
3. **Expired Refresh Token**: Login → Wait 7 days → Try to refresh (should fail)
4. **Revoked Token**: Login → Revoke token → Try to refresh (should fail)
5. **Invalid Token**: Try to refresh with random string (should fail)

## Troubleshooting

### Common Issues

#### Issue 1: "Invalid refresh token"

**Causes:**
- Token doesn't exist in database
- Token was already used (and rotated)
- Token string is malformed

**Solution:**
- Ensure token is stored correctly
- Use the latest refresh token after each refresh
- Check for typos in token string

#### Issue 2: "Refresh token has expired"

**Cause:**
- Token is older than 7 days

**Solution:**
- User must login again
- Consider adjusting token validity period

#### Issue 3: "Refresh token has been revoked"

**Causes:**
- User logged out
- Token was manually revoked
- Security event triggered revocation

**Solution:**
- User must login again

#### Issue 4: Access token expires too quickly

**Solution:**
- Implement automatic token refresh in client
- Consider adjusting access token expiration time in `JwtService.kt`

---

**Last Updated:** 2026-01-23
**Version:** 1.0.0
