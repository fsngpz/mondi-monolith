# Google OAuth Authentication Documentation

This document provides comprehensive instructions on how to set up and use Google OAuth authentication in the Mondi
jewelry store application.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Google Cloud Console Setup](#google-cloud-console-setup)
5. [Application Configuration](#application-configuration)
6. [Database Schema](#database-schema)
7. [API Endpoints](#api-endpoints)
8. [Client Integration](#client-integration)
9. [Testing](#testing)
10. [Security Considerations](#security-considerations)

## Overview

The application now supports Google OAuth 2.0 authentication, allowing users to sign in using their Gmail accounts. This
provides a seamless authentication experience without requiring users to create and remember separate passwords.

### Key Features

- Sign in with Google account
- Automatic user registration for new OAuth users
- JWT token generation for authenticated sessions
- Support for both local (email/password) and OAuth authentication methods
- Profile creation for new OAuth users

## Architecture

The OAuth implementation consists of the following components:

### Core Components

1. **GoogleOAuthService**: Handles Google ID token verification and user authentication
2. **GoogleOAuthController**: REST API endpoint for OAuth authentication
3. **User Entity**: Enhanced with OAuth provider fields (provider, providerId)
4. **UserRepository**: Extended with OAuth user lookup methods

### Authentication Flow

```
1. User clicks "Sign in with Google" on frontend
2. Frontend initiates Google OAuth flow using Google Sign-In library
3. Google authenticates user and returns ID token to frontend
4. Frontend sends ID token to backend API (/v1/auth/oauth/google)
5. Backend verifies ID token with Google
6. Backend creates/finds user and generates JWT token
7. Backend returns JWT token to frontend
8. Frontend stores JWT token for subsequent API calls
```

## Prerequisites

Before implementing Google OAuth, you need:

1. A Google Cloud Platform (GCP) account
2. A registered OAuth 2.0 client in Google Cloud Console
3. The application's Google Client ID

## Google Cloud Console Setup

### Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click "Select a project" → "New Project"
3. Enter project name (e.g., "Mondi Jewelry Store")
4. Click "Create"

### Step 2: Enable Google+ API

1. Navigate to "APIs & Services" → "Library"
2. Search for "Google+ API"
3. Click "Enable"

### Step 3: Configure OAuth Consent Screen

1. Go to "APIs & Services" → "OAuth consent screen"
2. Select "External" user type
3. Fill in the required information:
    - App name: Mondi Jewelry Store
    - User support email: your-email@example.com
    - Developer contact information: your-email@example.com
4. Click "Save and Continue"
5. Add scopes (required):
    - `.../auth/userinfo.email`
    - `.../auth/userinfo.profile`
    - `openid`
6. Click "Save and Continue"
7. Add test users (for development)
8. Click "Save and Continue"

### Step 4: Create OAuth 2.0 Client ID

1. Go to "APIs & Services" → "Credentials"
2. Click "Create Credentials" → "OAuth client ID"
3. Select application type:
    - **Web application** (for web apps)
    - **Android** (for Android apps)
    - **iOS** (for iOS apps)
4. Configure settings:
    - Name: Mondi Web Client
    - Authorized JavaScript origins:
        - http://localhost:3000 (development)
        - https://yourdomain.com (production)
    - Authorized redirect URIs:
        - http://localhost:3000 (development)
        - https://yourdomain.com (production)
5. Click "Create"
6. Copy the **Client ID** (you'll need this for configuration)

## Application Configuration

### Step 1: Add Configuration Properties

Add the following to your `application.yml` or `application.properties`:

#### application.yml

```yaml
google:
  oauth:
    client-id: YOUR_GOOGLE_CLIENT_ID_HERE
```

#### application.properties

```properties
google.oauth.client-id=YOUR_GOOGLE_CLIENT_ID_HERE
```

Replace `YOUR_GOOGLE_CLIENT_ID_HERE` with the Client ID from Google Cloud Console.

### Step 2: Environment-Specific Configuration

For different environments, use Spring profiles:

#### application-dev.yml (Development)

```yaml
google:
  oauth:
    client-id: YOUR_DEV_CLIENT_ID
```

#### application-prod.yml (Production)

```yaml
google:
  oauth:
    client-id: ${GOOGLE_OAUTH_CLIENT_ID}
```

Use environment variables in production:

```bash
export GOOGLE_OAUTH_CLIENT_ID=your-production-client-id
```

### Fields Description

| Field         | Type             | Description                                            |
|---------------|------------------|--------------------------------------------------------|
| `provider`    | `oauth_provider` | Authentication provider ('LOCAL' or 'GOOGLE')          |
| `provider_id` | `text`           | Unique identifier from OAuth provider (Google user ID) |
| `password`    | `text`           | Password (nullable for OAuth users)                    |
| `email`       | `text`           | User email address                                     |
| `username`    | `text`           | User display name                                      |

## API Endpoints

### Authenticate with Google OAuth

**Endpoint:** `POST /v1/auth/oauth/google`

**Description:** Authenticates a user with Google OAuth ID token and returns a JWT token.

#### Request

```http
POST /v1/auth/oauth/google HTTP/1.1
Content-Type: application/json

{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE4MmU0M..."
}
```

#### Request Body

| Field     | Type     | Required | Description                         |
|-----------|----------|----------|-------------------------------------|
| `idToken` | `string` | Yes      | Google ID token from Google Sign-In |

#### Response (Success - 200 OK)

```json
{
  "bearerToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Response Fields

| Field         | Type     | Description                         |
|---------------|----------|-------------------------------------|
| `bearerToken` | `string` | JWT token for authenticated session |

#### Error Responses

**400 Bad Request** - Invalid request

```json
{
  "message": "field 'idToken' cannot be null"
}
```

**401 Unauthorized** - Invalid ID token

```json
{
  "message": "Invalid Google ID token"
}
```

**409 Conflict** - Email already exists with local account

```json
{
  "message": "An account with email 'user@example.com' already exists. Please login with your password."
}
```

### Using the JWT Token

Include the JWT token in subsequent API requests:

```http
GET /v1/account/profiles HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Client Integration

### Web Application (React/Vue/Angular)

#### Step 1: Install Google Sign-In Library

**Using npm:**

```bash
npm install @react-oauth/google
```

**Using script tag:**

```html

<script src="https://accounts.google.com/gsi/client" async defer></script>
```

#### Step 2: Initialize Google Sign-In

**React Example:**

```jsx
import {GoogleOAuthProvider, GoogleLogin} from '@react-oauth/google';

function App() {
    const handleSuccess = async (credentialResponse) => {
        try {
            // Send ID token to backend
            const response = await fetch('http://localhost:8080/v1/auth/oauth/google', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    idToken: credentialResponse.credential
                })
            });

            const data = await response.json();

            // Store JWT token
            localStorage.setItem('token', data.bearerToken);

            // Redirect to home page
            window.location.href = '/';
        } catch (error) {
            console.error('Authentication failed:', error);
        }
    };

    return (
        <GoogleOAuthProvider clientId="YOUR_GOOGLE_CLIENT_ID">
            <GoogleLogin
                onSuccess={handleSuccess}
                onError={() => console.log('Login Failed')}
            />
        </GoogleOAuthProvider>
    );
}
```

**Vanilla JavaScript Example:**

```html
<!DOCTYPE html>
<html>
<head>
    <script src="https://accounts.google.com/gsi/client" async defer></script>
</head>
<body>
<div id="g_id_onload"
     data-client_id="YOUR_GOOGLE_CLIENT_ID"
     data-callback="handleCredentialResponse">
</div>
<div class="g_id_signin" data-type="standard"></div>

<script>
    function handleCredentialResponse(response) {
        fetch('http://localhost:8080/v1/auth/oauth/google', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                idToken: response.credential
            })
        })
                .then(res => res.json())
                .then(data => {
                    localStorage.setItem('token', data.bearerToken);
                    window.location.href = '/';
                })
                .catch(error => console.error('Error:', error));
    }
</script>
</body>
</html>
```

### Android Application

#### Step 1: Add Dependencies

```gradle
dependencies {
    implementation 'com.google.android.gms:play-services-auth:20.7.0'
}
```

#### Step 2: Configure Google Sign-In

```kotlin
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken("YOUR_GOOGLE_CLIENT_ID")
    .requestEmail()
    .build()

val googleSignInClient = GoogleSignIn.getClient(this, gso)
```

#### Step 3: Handle Sign-In

```kotlin
private fun signIn() {
    val signInIntent = googleSignInClient.signInIntent
    startActivityForResult(signInIntent, RC_SIGN_IN)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == RC_SIGN_IN) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            authenticateWithBackend(account.idToken!!)
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
        }
    }
}

private fun authenticateWithBackend(idToken: String) {
    val client = OkHttpClient()
    val json = JSONObject()
    json.put("idToken", idToken)

    val body = RequestBody.create(
        MediaType.parse("application/json"),
        json.toString()
    )

    val request = Request.Builder()
        .url("http://your-api.com/v1/auth/oauth/google")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body()?.string()
            val jsonResponse = JSONObject(responseBody)
            val token = jsonResponse.getString("bearerToken")

            // Store token and navigate to home
            saveToken(token)
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Authentication failed", e)
        }
    })
}
```

### iOS Application

#### Step 1: Add Dependencies

Add to your `Podfile`:

```ruby
pod 'GoogleSignIn'
```

#### Step 2: Configure Google Sign-In

```swift
import GoogleSignIn

let signInConfig = GIDConfiguration(clientID: "YOUR_GOOGLE_CLIENT_ID")

GIDSignIn.sharedInstance.signIn(
    withPresenting: self,
    hint: nil,
    additionalScopes: nil
) { signInResult, error in
    guard let result = signInResult else {
        return
    }

    guard let idToken = result.user.idToken?.tokenString else {
        return
    }

    authenticateWithBackend(idToken: idToken)
}
```

#### Step 3: Handle Authentication

```swift
func authenticateWithBackend(idToken: String) {
    let url = URL(string: "http://your-api.com/v1/auth/oauth/google")!
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")

    let body: [String: Any] = ["idToken": idToken]
    request.httpBody = try? JSONSerialization.data(withJSONObject: body)

    URLSession.shared.dataTask(with: request) { data, response, error in
        guard let data = data else { return }

        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
           let token = json["bearerToken"] as? String {
            // Store token and navigate to home
            self.saveToken(token)
        }
    }.resume()
}
```

## Testing

### Unit Tests

The implementation includes comprehensive unit tests for `GoogleOAuthService`:

#### Test Cases

1. **Dependencies are not null** - Verifies all dependencies are injected
2. **Authenticate with valid token for new user** - Tests user creation flow
3. **Authenticate with valid token for existing user** - Tests existing user login
4. **Authenticate with invalid token** - Tests error handling
5. **Authenticate with existing local account** - Tests conflict scenario

#### Running Tests

```bash
./gradlew test --tests "com.mondi.machine.auths.oauth.GoogleOAuthServiceTest"
```

### Manual Testing with Postman

#### Test Setup

1. Obtain a valid Google ID token:
    - Go to [Google OAuth Playground](https://developers.google.com/oauthplayground/)
    - Select "Google OAuth2 API v2" → "userinfo.email" and "userinfo.profile"
    - Click "Authorize APIs"
    - Exchange authorization code for tokens
    - Copy the `id_token`

2. Test the endpoint:

```http
POST http://localhost:8080/v1/auth/oauth/google
Content-Type: application/json

{
  "idToken": "YOUR_ID_TOKEN_FROM_PLAYGROUND"
}
```

### Integration Testing

Test the complete flow:

1. Start the application
2. Navigate to Swagger UI: http://localhost:8080/swagger-ui.html
3. Find "Google OAuth API" section
4. Click "POST /v1/auth/oauth/google"
5. Enter a valid Google ID token
6. Execute and verify response

## Security Considerations

### Best Practices

1. **HTTPS Only in Production**
    - Always use HTTPS for production environments
    - Google OAuth requires HTTPS for authorized domains

2. **Client ID Protection**
    - Store Client ID in environment variables
    - Never commit sensitive credentials to version control
    - Use different Client IDs for development and production

3. **Token Verification**
    - Backend always verifies ID token with Google
    - Never trust client-side token validation alone

4. **JWT Token Security**
    - JWT tokens expire after 30 minutes
    - Store tokens securely (HttpOnly cookies or secure storage)
    - Implement token refresh mechanism for better UX

5. **CORS Configuration**
    - Configure CORS properly for your frontend domains
    - Don't use wildcard (*) in production

### Environment Variables

For production deployment, use environment variables:

```bash
# .env file
GOOGLE_OAUTH_CLIENT_ID=your-production-client-id
SPRING_AUTH_SECRET=your-jwt-secret
DATABASE_URL=your-database-url
```

### Rate Limiting

Consider implementing rate limiting on OAuth endpoints to prevent abuse:

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: oauth
          uri: http://localhost:8080
          predicates:
            - Path=/v1/auth/oauth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

## Troubleshooting

### Common Issues

#### Issue 1: "Invalid Google ID token"

**Causes:**

- Token has expired
- Token was issued for different Client ID
- Network issues during verification

**Solution:**

- Ensure frontend uses correct Client ID
- Request new token from Google
- Check network connectivity

#### Issue 2: "Email already exists with local account"

**Cause:**

- User previously registered with email/password

**Solution:**

- Inform user to login with password
- Or implement account linking feature

#### Issue 3: "redirect_uri_mismatch"

**Cause:**

- Frontend redirect URI not registered in Google Cloud Console

**Solution:**

- Add the redirect URI to authorized redirect URIs in Google Cloud Console
- Ensure URI matches exactly (including protocol and port)

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.mondi.machine.auths.oauth: DEBUG
```

## Support

For additional help:

1. Check [Google OAuth Documentation](https://developers.google.com/identity/protocols/oauth2)
2. Review [Spring Security OAuth2 Guide](https://spring.io/guides/tutorials/spring-boot-oauth2/)
3. Consult the application's API documentation at `/swagger-ui.html`

---

**Last Updated:** 2026-01-22
**Version:** 1.0.0
