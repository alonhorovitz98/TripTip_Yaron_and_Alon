# TripTip — shared social setup (Instagram-style)

Everyone who clones this repo uses **one shared Firebase project** (`triptip-97085`).  
Posts, likes, comments, and photos sync in real time — like a small Instagram for the class.

## For teammates (clone & run)

1. **Clone the repo**
   ```bash
   git clone <repo-url>
   cd TripTip_Yaron_and_Alon
   ```

2. **Firebase config is included**  
   `app/google-services.json` is committed so all clones point at the **same** cloud backend.  
   Do **not** create your own Firebase project unless you intentionally want a private sandbox.

3. **API keys (optional, for maps/places)**  
   Copy `local.properties.example` → `local.properties` and add:
   - `GOOGLE_PLACES_API_KEY` — for nearby places & location search

4. **Open in Android Studio** → Sync Gradle → Run on emulator or device.

5. **Register a new account** (each person uses their own email/password).  
   You do **not** share login credentials — only the Firebase backend.

6. **Use the Feed tab** (first tab) to see **everyone’s** posts.  
   **My Posts** shows only what **you** published.

## For the project owner (one-time Firebase setup)

1. Open [Firebase Console](https://console.firebase.google.com) → project **triptip-97085**.

2. **Authentication** → Sign-in method → enable **Email/Password**.

3. **Firestore** → Create database (production mode is fine once rules are deployed).

4. **Deploy security rules** (from repo root, with [Firebase CLI](https://firebase.google.com/docs/cli)):
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase use triptip-97085
   firebase deploy --only firestore:rules,storage
   ```
   Or paste `firestore.rules` and `storage.rules` manually in the Console.

5. **Storage** → ensure a default bucket exists (same project).

6. When adding a new Android app signing key (release builds), add its SHA-1 in  
   Project settings → Your apps → Android app.

## How the social feed works

| Tab | What you see |
|-----|----------------|
| **Feed** | All posts from all TripTip users (newest first), live from Firestore |
| **My Posts** | Only your posts |
| **Nearby** | Places near you (Google Places), not a post feed |

When someone publishes a post:
1. Image uploads to Firebase Storage (if any)
2. Post document is written to Firestore `posts`
3. Every other device’s **Feed** tab updates automatically

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Friend’s posts don’t appear | Open **Feed**, not My Posts. Confirm both apps use the same `google-services.json` (`project_id`: `triptip-97085`). |
| Orange banner “Can’t sync posts” | Log in, check internet, deploy Firestore rules (see above). |
| Post publish fails (permissions) | Deploy `firestore.rules` and `storage.rules`. |
| Post with photo fails | Deploy storage rules; check Firebase Storage is enabled. |
| Verify cloud write | Firebase Console → Firestore → `posts` — document should appear after publish. |

## What this app is (and isn’t)

- **Is:** Shared global feed — any registered user sees everyone’s public posts.
- **Isn’t:** Follow/friends graph (no “only people I follow” yet). That would need new Firestore collections and UI.
