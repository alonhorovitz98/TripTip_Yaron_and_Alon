# Firebase Setup Instructions

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select existing project
3. Enter project name: **TripTip**
4. Follow the setup wizard (disable Google Analytics if not needed)

## Step 2: Add Android App to Firebase

1. In Firebase Console, click the Android icon (or "Add app")
2. Enter package name: `com.example.triptip_yaron_and_alon`
3. Enter app nickname: **TripTip Android** (optional)
4. Enter SHA-1 (optional for now, needed for Auth later)
5. Click "Register app"

## Step 3: Download google-services.json

1. Download `google-services.json` file
2. **Place it in:** `app/` directory (same level as `build.gradle.kts`)
3. **DO NOT commit this file** (it's in .gitignore)

## Step 4: Enable Firebase Services

### Authentication
1. Go to Firebase Console → Authentication
2. Click "Get started"
3. Enable "Email/Password" sign-in method
4. Click "Save"

### Firestore Database
1. Go to Firebase Console → Firestore Database
2. Click "Create database"
3. Start in **test mode** (we'll add security rules later)
4. Choose a location (closest to your users)
5. Click "Enable"

### Storage
1. Go to Firebase Console → Storage
2. Click "Get started"
3. Start in **test mode** (we'll add security rules later)
4. Click "Done"

**Option 2: Skip Storage (Use Alternative)**
If you want to skip Firebase Storage, we can use one of these alternatives:
- **Imgur API** (free, no account needed for anonymous uploads)
- **Cloudinary** (free tier: 25GB storage, 25GB bandwidth/month)
- **Store images as base64 in Firestore** (limited to 1MB per document - not recommended for large images)
- **Local storage only** (images won't sync across devices)

See **Image Storage Alternatives** section below for implementation details.

## Step 5: Add Security Rules (Later - Step 14)

We'll add proper security rules in Step 14. For now, test mode is fine for development.

### Firestore Rules (to be added later):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /posts/{postId} {
      allow read: if true;
      allow create: if request.auth != null && 
                      request.resource.data.userId == request.auth.uid;
      allow update, delete: if request.auth != null && 
                              resource.data.userId == request.auth.uid;
    }
    match /trips/{tripId} {
      allow read, write: if request.auth != null && 
                           resource.data.userId == request.auth.uid;
    }
  }
}
```

### Storage Rules (to be added later):
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /post_images/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    match /profile_images/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Step 6: Verify Setup

After adding `google-services.json`:
1. Sync Gradle files in Android Studio
2. Build the project
3. Check for any Firebase-related errors

## Notes

- **google-services.json** is in `.gitignore` - each developer needs their own copy
- For production, add proper security rules
- SHA-1 is needed for certain Auth features (can be added later)

## Getting SHA-1 (Optional - for later)

```bash
# Windows (PowerShell)
cd android
.\gradlew signingReport

# Or using keytool
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

The SHA-1 will be shown in the output. Add it to Firebase Console → Project Settings → Your Android App.

