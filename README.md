# TripTip - Implementation Plan & Task Assignment

## Project Overview

**TripTip** is a social travel application that helps users discover authentic travel content and build complete trip itineraries based on real user-generated posts. Built with MVVM architecture, Repository pattern, Fragment-based navigation, and Firebase.

---

## Architecture Overview

```
UI Layer (Fragments + ViewModels)
    ↓
Repository Layer (Single Source of Truth)
    ↓
Data Sources
    ├── Remote (Firebase: Auth, Firestore, Storage)
    ├── Remote (External APIs: OpenWeatherMap, OpenTripMap)
    └── Local (Room Database - SQLite)
```

### Key Requirements
- ✅ All network operations are asynchronous (Flow, suspend, LiveData)
- ✅ Cache-first strategy: Room first, then Firestore
- ✅ Fragment-based navigation with Safe Args
- ✅ Loading indicators in appropriate places
- ✅ User ownership and authentication
- ✅ Lazy/incremental loading

---

## Current Progress Summary

### ✅ Completed Phases
- **Phase 1-6**: Foundation & Data Layer (Alon) - 100% Complete
- **Phase 7**: Authentication UI (Yaron) - 100% Complete
- **Phase 8**: Feed UI (Yaron) - 100% Complete
- **Phase 9**: Post Details & Create Post (Yaron) - 100% Complete
- **Phase 11**: Profile UI (Yaron) - 80% Complete (EditProfileFragment pending)
- **Phase 12.1-12.2**: API Services & Repository (Alon) - 100% Complete
- **Phase 13**: Trip Builder (Yaron) - 75% Complete (TripDayEditorFragment pending)

### 🚧 In Progress / Pending
- **Phase 10**: My Posts & Edit Post (Yaron) - ViewModels complete, Fragments need UI implementation
- **Phase 12.3**: API Integration in UI (Yaron) - Not started
- **Phase 14**: Final Polish (Alon) - Not started

### 📊 Overall Status
- **Data Layer**: 100% Complete ✅
- **UI Layer**: ~70% Complete 🚧
- **API Integration**: 66% Complete (Services done, UI integration pending)

---

## Recent Fixes (Latest Update)

### ViewModel Fixes (Latest)
**Issues Fixed:**
- ✅ **FeedViewModel**: Fixed `loadMorePosts()` - `getPostsPaginated()` returns `Flow<List<Post>>`, not `Flow<Result<List<Post>>>`
- ✅ **All ViewModels**: Repository constructors verified and correct
- ✅ **PostViewModel**: Methods correctly create `Post` objects before calling repository
- ✅ **TripViewModel**: Methods correctly create `Trip`/`TripDay` objects before calling repository

**Status:** All ViewModels compile correctly and use proper repository interfaces. Pagination in FeedViewModel now works correctly.

---

## Task Assignment (Option 2: Adjusted Split)

### Alon's Tasks: Foundation, Data Layer & APIs (Steps 1-6, 12.1-12.2, 14)

**Responsibilities:**
- Project setup and dependencies
- Domain models and utilities
- Room database (entities, DAOs, database)
- Navigation setup
- Firebase remote data sources
- Repository layer (cache-first strategy)
- External API setup (OpenWeatherMap, OpenTripMap) - **API services and repository only**
- Final polish and requirements verification

**Deliverables:**
- Complete data layer that UI can depend on
- Working repositories with async operations
- Navigation graph with Safe Args
- API services and repository ready for UI integration

**Estimated Time:** ~6-7 days

### Yaron's Tasks: UI Layer & API Integration (Steps 7-11, 12.3, 13)

**Responsibilities:**
- Authentication UI (Login, Register)
- Feed UI with lazy loading
- Post management UI (Create, Edit, Delete, Details, My Posts)
- Profile UI (View, Edit)
- **API integration in UI** (weather and places in PostDetailsFragment)
- Trip Builder UI (List, Builder, Day Editor)

**Deliverables:**
- All user-facing screens
- ViewModels connecting to repositories
- Loading states and error handling
- Complete user flows
- API data displayed in UI

**Estimated Time:** ~6-7 days

### 📝 Notes
- Firebase Storage: Using local file storage fallback for development (see `IMAGE_STORAGE_STRATEGY.md`)
- Compose code removed, Fragment-based navigation ready
- All dependencies configured and synced

---

## Detailed Implementation Steps

### Phase 1: Project Foundation (Alon - Day 1)

- [x] #### Step 1.1: Update Dependencies
**File:** `gradle/libs.versions.toml`, `app/build.gradle.kts`

Add all required libraries:
- Room (2.6.1) for local database
- Retrofit (2.9.0) + OkHttp for API calls
- Firebase BOM (33.0.0) - Auth, Firestore, Storage
- Navigation Component (2.8.2) with Safe Args
- Coil (2.5.0) for image loading
- Material Design components
- Coroutines for async operations

**Test:** Project syncs without errors

---

- [x] #### Step 1.2: Create Package Structure
**Files:** Create all package folders

```
com.example.triptip_yaron_and_alon/
├── data/
│   ├── local/database/ (entities, dao, database)
│   ├── remote/firebase/ (Auth, Firestore, Storage)
│   ├── remote/api/ (Weather, OpenTripMap)
│   └── repository/
├── domain/
│   ├── model/
│   └── mapper/
├── ui/ (auth, feed, post, trip, profile, adapter)
├── navigation/
└── util/
```

**Test:** All packages exist

---

- [ ] #### Step 1.3: Firebase Setup ⏳
**Files:** Firebase Console, `google-services.json`, `build.gradle.kts`
**Status:** ✅ `google-services.json` file added. Verify package name matches and services are enabled.

1. Create Firebase project
2. Add `google-services.json` to `app/`
3. Enable Authentication (Email/Password)
4. Enable Firestore Database
5. Enable Storage
6. Add Firebase plugin to `build.gradle.kts`

**Test:** Firebase connection works

---

- [ ] #### Step 1.4: Git Initialization
**Files:** `.gitignore`, Git branches

1. Initialize Git repository
2. Create `.gitignore`
3. Create branches: `main`, `develop`, `feature/auth`, `feature/posts`, `feature/trips`
4. Initial commit: "Project setup complete"

**Test:** Git repository initialized

---

### Phase 2: Domain Models & Utilities (Alon - Day 1-2)

- [x] #### Step 2.1: Domain Models
**Files:** `domain/model/*.kt`

Create all domain models:
- `User.kt` - id, email, name, profileImageUrl
- `Post.kt` - id, userId, text, imageUrl, createdAt, location, latitude, longitude
- `Trip.kt` - id, userId, title, description, days
- `TripDay.kt` - id, tripId, dayNumber, items
- `TripItem.kt` - id, dayId, postId, order, notes
- `WeatherInfo.kt` - temperature, description, icon, humidity, windSpeed
- `PlaceInfo.kt` - xid, name, description, coordinates, imageUrl, categories

**Test:** Models compile, data classes work

---

- [x] #### Step 2.2: Utilities
**Files:** `util/Result.kt`, `util/Constants.kt`, `util/Extensions.kt`

- `Result.kt` - Sealed class (Success, Error, Loading) ✅
- `Constants.kt` - Collection names, storage paths, API keys ✅
- `Extensions.kt` - Helper functions for error handling, UI ✅

**Test:** Utilities compile and can be imported

---

### Phase 3: Room Database Setup (Alon - Day 2)

- [x] #### Step 3.1: Room Entities
**Files:** `data/local/database/entities/*.kt`

Create entities with `@Entity` annotation:
- `PostEntity` - All post fields + cachedAt timestamp
- `TripEntity` - Trip data
- `TripDayEntity` - Day data
- `TripItemEntity` - Item data
- `UserEntity` - User cache data

**Test:** Entities compile

---

- [x] #### Step 3.2: Room DAOs
**Files:** `data/local/database/dao/*.kt`

Create DAOs with `@Dao` annotation:
- `PostDao`:
  - `getAllPosts(): Flow<List<PostEntity>>`
  - `getPostById(postId): Flow<PostEntity?>`
  - `getPostsByUser(userId): Flow<List<PostEntity>>`
  - `getPostsPaginated(limit, offset): Flow<List<PostEntity>>`
  - `insertAll()`, `insert()`, `update()`, `delete()` (suspend)
- `TripDao`, `TripDayDao`, `UserDao` - Similar pattern

**Test:** DAOs compile, can create database instance

---

- [x] #### Step 3.3: Room Database
**Files:** `data/local/database/TripTipDatabase.kt`

Create `@Database` class:
- Include all entities
- Provide DAOs
- Singleton pattern with `getDatabase(context)`
- Version management

**Test:** Database can be created, DAOs accessible

---

- [x] #### Step 3.4: Mappers
**Files:** `domain/mapper/*.kt`

Create mappers:
- `PostMapper` - `toDomain(entity)`, `toEntity(domain)`
- `TripMapper` - Similar
- `UserMapper` - Similar

**Test:** Mappers convert between Entity and Domain correctly

---

### Phase 4: Navigation Setup (Alon - Day 2)

- [x] #### Step 4.1: Navigation Graph
**Files:** `app/src/main/res/navigation/nav_graph.xml`

Create navigation graph with:
- All fragments defined
- Actions between fragments
- Safe Args arguments:
  - `PostDetailsFragment`: `postId: String`
  - `EditPostFragment`: `postId: String`
  - `TripBuilderFragment`: `tripId: String`, `postId: String?`
  - `TripDayEditorFragment`: `tripId: String`, `dayId: String`
- Start destination: `LoginFragment`

**Test:** Navigation graph compiles, Safe Args classes generated

---

- [x] #### Step 4.2: MainActivity Setup
**Files:** `activity_main.xml`, `MainActivity.kt`

1. Create `activity_main.xml` with NavHostFragment
2. Update `MainActivity.kt`:
   - Setup Navigation Component
   - Check authentication state
   - Navigate to Feed if logged in, Login if not

**Test:** App launches, navigation works

---

- [x] #### Step 4.3: Placeholder Fragments
**Files:** All fragment classes and layouts

Create empty fragments:
- `LoginFragment`, `RegisterFragment`
- `FeedFragment`, `PostDetailsFragment`, `CreatePostFragment`, `EditPostFragment`, `MyPostsFragment`
- `TripListFragment`, `TripBuilderFragment`, `TripDayEditorFragment`
- `ProfileFragment`, `EditProfileFragment`

Each fragment shows its name in a TextView.

**Test:** Can navigate between all screens

---

### Phase 5: Firebase Remote Data Sources (Alon - Day 3)

- [x] #### Step 5.1: Firebase Auth Data Source
**Files:** `data/remote/firebase/FirebaseAuthDataSource.kt`

Create class with async methods:
- `signUp(email, password): Flow<Result<User>>`
- `signIn(email, password): Flow<Result<User>>`
- `signOut(): suspend Result<Unit>`
- `getCurrentUser(): Flow<User?>`
- `updateProfile(name, imageUri): Flow<Result<User>>`

All methods use coroutines, NO blocking calls.

**Test:** Can sign up/in (manual test or unit test)

---

- [x] #### Step 5.2: Firestore Data Source
**Files:** `data/remote/firebase/FirestoreDataSource.kt`

Create class with async methods:
- `getPosts(): Flow<List<Post>>` (uses Firestore snapshots)
- `getPostById(postId): Flow<Post?>`
- `getUserPosts(userId): Flow<List<Post>>`
- `createPost(post): suspend Result<Post>`
- `updatePost(post): suspend Result<Post>`
- `deletePost(postId): suspend Result<Unit>`
- Similar methods for Trips

All methods use coroutines, NO blocking calls.

**Test:** Can create/read posts in Firestore

---

- [x] #### Step 5.3: Firebase Storage Data Source (Local Storage Fallback)
**Files:** `data/remote/firebase/FirebaseStorageDataSource.kt`

**Note:** Using local file storage for development/testing. See `IMAGE_STORAGE_STRATEGY.md` for details.

Create class with async methods:
- `uploadImage(uri, path): Flow<Result<String>>` (returns local file path)
- `deleteImage(imageUrl): suspend Result<Unit>` (deletes local file)

Implementation:
- Save images to app's external files directory
- Return file paths (e.g., `/storage/.../Pictures/posts/user123/image.jpg`)
- Store paths in Firestore as `imageUrl`
- Coil/Picasso can load from file paths directly

All methods use coroutines, NO blocking calls.

**Test:** Can upload image to local storage, get file path back, image loads in UI

---

### Phase 6: Repository Layer (Alon - Day 3-4)

- [x] #### Step 6.1: Auth Repository
**Files:** `data/repository/AuthRepository.kt`

Create repository:
- Combines FirebaseAuthDataSource with Room user caching
- `signUp(email, password): Flow<Result<User>>`
- `signIn(email, password): Flow<Result<User>>`
- `signOut(): suspend Result<Unit>`
- `getCurrentUser(): Flow<User?>` (checks Firebase, caches in Room)
- `updateProfile(name, imageUri): Flow<Result<User>>`
- `isUserLoggedIn(): Flow<Boolean>` (for auto-login)

**Test:** Auth flow works, user cached in Room

---

- [x] #### Step 6.2: Post Repository
**Files:** `data/repository/PostRepository.kt`

Create repository with cache-first strategy:
- `getPosts(): Flow<List<Post>>`
  - Emit cached posts from Room immediately
  - Fetch from Firestore in background (async)
  - Update Room cache
  - Emit updated list
- `getPostsPaginated(page, pageSize): Flow<List<Post>>` (lazy loading)
- `getPostById(postId): Flow<Post?>` (cache-first)
- `getUserPosts(userId): Flow<List<Post>>` (filtered by userId)
- `createPost(post, imageUri): Flow<Result<Post>>`
  - Upload image to Storage (async)
  - Save to Firestore (async)
  - Save to Room cache (async)
- `updatePost(post, imageUri?): Flow<Result<Post>>`
- `deletePost(postId): Flow<Result<Unit>>`

**Test:** Cache-first works, posts load from Room first, then update from Firestore

---

- [x] #### Step 6.3: User Repository
**Files:** `data/repository/UserRepository.kt`

Create repository:
- `getCurrentUser(): Flow<User?>` (from Room cache or Firebase)
- `updateProfile(name, imageUri): Flow<Result<User>>`
  - Update Firebase Auth (async)
  - Update Firestore (async)
  - Update Room cache (async)

**Test:** Profile updates work, cached in Room

---

- [x] #### Step 6.4: Trip Repository (Basic Structure)
**Files:** `data/repository/TripRepository.kt`

Create repository with basic structure:
- `getTrips(userId): Flow<List<Trip>>`
- `getTripById(tripId): Flow<Trip?>`
- `createTrip(trip): Flow<Result<Trip>>`
- `updateTrip(trip): Flow<Result<Trip>>`
- `deleteTrip(tripId): Flow<Result<Unit>>`
- `addDayToTrip(tripId, day): Flow<Result<TripDay>>`
- `addItemToDay(dayId, item): Flow<Result<TripItem>>`
- `reorderItems(dayId, items): Flow<Result<Unit>>`

All methods async, cache-first strategy.

**Test:** Trip operations work, cache-first works

---

### Phase 7: Authentication UI (Yaron - Day 4-5) ✅ COMPLETE

- [x] #### Step 7.1: Auth ViewModel
**Files:** `ui/auth/AuthViewModel.kt`

✅ **Complete** - ViewModel created with all required methods and LiveData.

---

- [x] #### Step 7.2: Login Fragment
**Files:** `fragment_login.xml`, `ui/auth/LoginFragment.kt`

✅ **Complete** - Full UI implementation with loading states, error handling, and navigation.

---

- [x] #### Step 7.3: Register Fragment
**Files:** `fragment_register.xml`, `ui/auth/RegisterFragment.kt`

✅ **Complete** - Full UI implementation with validation, loading states, and navigation.

---

- [x] #### Step 7.4: Auto-Login in MainActivity
**Files:** `MainActivity.kt`

✅ **Complete** - Auto-login implemented with async authentication check.

---

### Phase 8: Feed UI (Yaron - Day 5-6) ✅ COMPLETE

- [x] #### Step 8.1: Feed ViewModel
**Files:** `ui/feed/FeedViewModel.kt`

✅ **Complete** - ViewModel created with all required methods. Fixed pagination to handle `Flow<List<Post>>` correctly.

---

- [x] #### Step 8.2: Feed Fragment
**Files:** `fragment_feed.xml`, `item_post.xml`, `ui/adapter/PostAdapter.kt`, `ui/feed/FeedFragment.kt`

Layout:
- SwipeRefreshLayout
- RecyclerView
- ProgressBar (initial loading)
- Empty state TextView
- Error TextView

Item layout:
- Post text, user name, user image, post image, date

Adapter:
- Use DiffUtil
- Load images with Coil (automatic caching)

Fragment:
- Observe ViewModel LiveData
- Show loading spinner when `isLoading = true`
- Show cached posts immediately
- Implement pull-to-refresh
- Implement lazy loading (load more on scroll)
- Navigate to PostDetailsFragment using Safe Args
- Handle empty/error states

**Test:** Feed displays posts, loading spinner works, pull-to-refresh works, lazy loading works

---

### Phase 9: Post Details & Create Post (Yaron - Day 6-7) ✅ COMPLETE

- [x] #### Step 9.1: Post ViewModel
**Files:** `ui/post/PostViewModel.kt`

✅ **Complete** - ViewModel created with all required methods.

---

- [x] #### Step 9.2: Post Details Fragment
**Files:** `fragment_post_details.xml`, `ui/post/PostDetailsFragment.kt`

✅ **Complete** - Full UI implementation with:
- Post image, text, user info, location display
- Loading states and error handling
- Navigation to TripBuilderFragment
- Weather and places sections prepared for Step 12.3

---

- [x] #### Step 9.3: Create Post Fragment
**Files:** `fragment_create_post.xml`, `ui/post/CreatePostFragment.kt`

✅ **Complete** - Full UI implementation with:
- Image picker using ActivityResultLauncher
- Image preview with Coil
- Post text and location input
- Loading states during upload
- Navigation back to FeedFragment on success
- Error handling

---

### Phase 10: My Posts & Edit Post (Yaron - Day 7-8)

- [x] #### Step 10.1: Update Post ViewModel
**Files:** `ui/post/PostViewModel.kt`

Add methods:
- `getUserPosts(userId: String): LiveData<List<Post>>`
- `updatePost(postId, text, imageUri?): LiveData<Result<Post>>`
- `deletePost(postId: String): LiveData<Result<Unit>>`
- Ownership verification (check userId matches current user)

**Test:** ViewModel methods work, ownership check works

---

- [ ] #### Step 10.2: My Posts Fragment (Placeholder - needs UI implementation)
**Files:** `fragment_my_posts.xml`, `ui/post/MyPostsFragment.kt`

Layout:
- RecyclerView
- ProgressBar
- Empty state TextView
- Error TextView

Fragment:
- Get current user ID
- Load only current user's posts (filtered)
- Show loading spinner
- Display posts with edit/delete buttons
- Navigate to EditPostFragment with Safe Args
- Handle delete confirmation

**Test:** Only shows current user's posts, edit/delete buttons work

---

- [ ] #### Step 10.3: Edit Post Fragment (Placeholder - needs UI implementation)
**Files:** `fragment_edit_post.xml`, `ui/post/EditPostFragment.kt`

Layout:
- Text EditText (pre-filled)
- Image ImageView (current image)
- Change Image Button
- Save Button
- Delete Button
- ProgressBar

Fragment:
- Get postId from Safe Args
- Load post (verify ownership)
- Pre-fill form with existing data
- Show loading spinner during update
- Update post (async: update Firestore → update Room cache)
- Delete post (async: delete from Firestore → delete from Room)
- Navigate back on success
- Verify user owns post before edit/delete

**Test:** Can edit own posts, cannot edit others' posts, delete works

---

### Phase 11: Profile UI (Yaron - Day 8-9)

- [x] #### Step 11.1: Profile ViewModel
**Files:** `ui/profile/ProfileViewModel.kt`

Create ViewModel:
- `user: LiveData<User?>`
- `isLoading: LiveData<Boolean>`
- `error: LiveData<String?>`
- `loadProfile()`
- `updateProfile(name, imageUri)`
- `logout()`

**Test:** ViewModel loads profile correctly

---

- [x] #### Step 11.2: Profile Fragment
**Files:** `fragment_profile.xml`, `ui/profile/ProfileFragment.kt`

Layout:
- Profile image ImageView (Coil)
- Username TextView
- Email TextView
- "Edit Profile" Button
- "My Posts" Button
- "Logout" Button
- ProgressBar

Fragment:
- Load current user profile (async)
- Display profile image and username
- Navigate to EditProfileFragment
- Navigate to MyPostsFragment
- Handle logout (navigate to LoginFragment)

**Test:** Profile displays correctly, navigation works, logout works

---

- [ ] #### Step 11.3: Edit Profile Fragment (Placeholder - needs UI implementation)
**Files:** `fragment_edit_profile.xml`, `ui/profile/EditProfileFragment.kt`

Layout:
- Username EditText (pre-filled)
- Profile image ImageView (current image)
- Change Image Button
- Save Button
- ProgressBar
- Error TextView

Fragment:
- Load current profile (async)
- Pre-fill form
- Image picker for profile picture
- Show loading spinner during update
- Update profile (async: update Firebase Auth → update Firestore → update Room cache)
- Navigate back on success
- Handle errors

**Test:** Can edit profile, image updates, navigation works

---

### Phase 12: External API Integration

- [x] #### Step 12.1: API Services Setup (Alon - Day 9-10)
**Files:** `data/remote/api/WeatherApiService.kt`, `data/remote/api/OpenTripMapApiService.kt`, `data/remote/api/*ApiClient.kt`

Create Retrofit interfaces:
- `WeatherApiService`:
  - `getCurrentWeather(lat, lon, apiKey): suspend WeatherResponseDto`
- `OpenTripMapApiService`:
  - `getNearbyPlaces(lat, lon, radius, apiKey): suspend NearbyPlacesResponseDto`
  - `getPlaceDetails(xid, apiKey): suspend PlaceDetailsDto`

Create API clients (Retrofit instances with base URLs, interceptors).

**Test:** API services compile, can make test calls

---

- [x] #### Step 12.2: API Repository (Alon - Day 9-10)
**Files:** `data/repository/PlaceInfoRepository.kt`

Create repository:
- `getWeather(lat, lon): Flow<WeatherInfo>` (async Flow)
- `getNearbyPlaces(lat, lon, radius): Flow<List<PlaceInfo>>` (async Flow)
- All methods async, handle errors gracefully

**Test:** Can fetch weather and nearby places

---

- [ ] #### Step 12.3: Update Post Details with API Data (Yaron - Day 10)
**Files:** `ui/post/PostDetailsFragment.kt`, `ui/post/PostViewModel.kt`

Update PostViewModel:
- Add `weather: LiveData<WeatherInfo?>`
- Add `nearbyPlaces: LiveData<List<PlaceInfo>>`
- Add `loadWeather(lat, lon)` method
- Add `loadNearbyPlaces(lat, lon)` method

Update PostDetailsFragment:
- Show loading spinner for weather/places
- Load weather async (if coordinates available)
- Load nearby places async (if coordinates available)
- Display weather info with icon
- Display nearby places as cards/RecyclerView
- Handle loading/error states for APIs

**Test:** Weather and places display in post details when coordinates available

---

### Phase 13: Trip Builder (Yaron - Day 10-12)

- [x] #### Step 13.1: Trip ViewModel
**Files:** `ui/trip/TripViewModel.kt`

✅ **Complete** - ViewModel created with all required methods. Repository constructors and method signatures verified and correct.

---

- [x] #### Step 13.2: Trip List Fragment
**Files:** `fragment_trip_list.xml`, `item_trip.xml`, `ui/adapter/TripAdapter.kt`, `ui/trip/TripListFragment.kt`

Create:
- Layout with RecyclerView, ProgressBar, empty state
- TripAdapter with DiffUtil
- Fragment that loads user's trips
- Navigate to TripBuilderFragment with Safe Args

**Test:** Trip list displays correctly, loading spinner works

---

- [x] #### Step 13.3: Trip Builder Fragment
**Files:** `fragment_trip_builder.xml`, `ui/trip/TripBuilderFragment.kt`

✅ **Complete** - Basic functionality implemented. Note: Currently uses placeholder user ID - needs integration with AuthRepository for production.

---

- [ ] #### Step 13.4: Trip Day Editor Fragment (Placeholder - needs UI implementation)
**Files:** `fragment_trip_day_editor.xml`, `ui/trip/TripDayEditorFragment.kt`

Create:
- Layout with available posts list
- Add posts to day
- Edit notes for items
- Reorder items
- Delete items
- Loading indicators

**Test:** Can edit day, add posts, reorder items, edit notes

---

### Phase 14: Final Polish & Requirements Verification (Alon - Day 12-13)

- [ ] #### Step 14.1: Requirements Checklist
**Files:** Code review across entire project

Verify:
- ✅ NO synchronous network calls (search codebase for blocking calls)
- ✅ All network operations use coroutines/Flow
- ✅ Loading indicators in all appropriate places
- ✅ Room used for local persistence
- ✅ Firebase NOT used for local caching
- ✅ Image libraries (Coil/Picasso) used only for loading
- ✅ Fragment-based navigation
- ✅ Navigation Graph implemented
- ✅ Safe Args used for all parameter passing
- ✅ User authentication and ownership
- ✅ Auto-login works
- ✅ Logout works
- ✅ Profile editing works
- ✅ Post editing/deletion (ownership)
- ✅ Lazy loading works
- ✅ Cache-first strategy works

**Test:** All requirements met

---

- [ ] #### Step 14.2: Error Handling
**Files:** All ViewModels

Add:
- Consistent error handling across all ViewModels
- User-friendly error messages
- Handle network errors gracefully
- Show cached data when offline

**Test:** Error handling works consistently

---

- [ ] #### Step 14.3: Performance Testing
**Files:** Test entire app

Test:
- Lazy loading works
- Image caching works
- Room caching works
- Offline functionality works
- Fix any performance issues

**Test:** App performs well, works offline

---

## Task Assignment Summary

### Alon's Tasks (Steps 1-6, 12.1-12.2, 14)
- ✅ Foundation & Setup
- ✅ Domain Models
- ✅ Room Database
- ✅ Navigation Setup
- ✅ Firebase Data Sources
- ✅ Repository Layer (including TripRepository)
- ✅ External API Setup (services and repository only)
- ✅ Final Polish & Verification

**Estimated Time:** ~6-7 days

### Yaron's Tasks (Steps 7-11, 12.3, 13)
- ✅ Authentication UI (100% complete)
- ✅ Feed UI (100% complete)
- ⚠️ Post Management UI (ViewModels complete, fragments need implementation)
  - ✅ PostViewModel
  - ⏳ PostDetailsFragment (placeholder)
  - ⏳ CreatePostFragment (placeholder)
  - ⏳ MyPostsFragment (placeholder)
  - ⏳ EditPostFragment (placeholder)
- ✅ Profile UI (ProfileFragment complete, EditProfileFragment needs implementation)
- ⏳ **API Integration in UI** (Step 12.3 - not started)
- ⚠️ Trip Builder UI (ViewModels and basic fragments complete, TripDayEditorFragment needs implementation)
  - ✅ TripViewModel
  - ✅ TripListFragment
  - ✅ TripBuilderFragment (basic)
  - ⏳ TripDayEditorFragment (placeholder)

**Estimated Time:** ~6-7 days

---

## Dependency Analysis

### Why This Split Works Better

**Advantages:**
- ✅ **Clear separation:** Alon owns data layer, Yaron owns UI layer
- ✅ **API integration:** Yaron integrates APIs into his UI (PostDetailsFragment), which he already owns
- ✅ **No blocking:** Yaron can start UI work as soon as Alon completes Step 6
- ✅ **Natural ownership:** Yaron already owns PostDetailsFragment, so adding API data there makes sense
- ✅ **Final polish:** Alon does final verification, which requires understanding both layers

**Coordination Points:**
1. **After Step 6:** Alon hands off repositories to Yaron
2. **After Step 12.2:** Alon hands off API repository to Yaron for UI integration
3. **Before Step 14:** Both complete features, then Alon does final verification

---

## Coordination Points

### Critical Handoff Points

1. **After Step 6 (Repository Layer):**
   - **Alon:** Complete all repositories, test with simple UI
   - **Yaron:** Review repository interfaces, ask questions
   - **Handoff:** Alon demonstrates repository usage to Yaron

2. **After Step 12.2 (API Repository):**
   - **Alon:** Complete API services and repository
   - **Yaron:** Integrate APIs into PostDetailsFragment (Step 12.3)
   - **Handoff:** Alon explains API repository usage to Yaron

3. **Before Step 14 (Final Polish):**
   - **Both:** Complete all features
   - **Joint:** Test together, fix issues
   - **Alon:** Final requirements verification

### Daily Sync Checklist

- [ ] What did you complete today?
- [ ] What are you working on next?
- [ ] Any blockers or questions?
- [ ] Any changes needed to interfaces/contracts?
- [ ] Test together if possible

---

## Git Workflow

### Branch Strategy

```
main (production-ready code)
  └── develop (integration branch)
      ├── feature/data-layer (Alon)
      ├── feature/apis (Alon)
      ├── feature/auth-ui (Yaron)
      ├── feature/feed-ui (Yaron)
      ├── feature/posts-ui (Yaron)
      ├── feature/api-integration-ui (Yaron)
      └── feature/trips-ui (Yaron)
```

### Commit Guidelines

- Use clear commit messages
- Commit frequently (after each logical step)
- Push to feature branches daily
- Merge to develop after feature completion
- Both review before merging to main

**Example Commit Messages:**
- "feat: Add Room database entities and DAOs"
- "feat: Implement cache-first PostRepository"
- "feat: Add LoginFragment with loading states"
- "feat: Integrate weather API in PostDetailsFragment"
- "fix: Correct ownership verification in EditPostFragment"

---

## Testing Checklist

### Alon's Testing (Data Layer)
- [ ] Room database creates successfully
- [ ] DAOs return Flow correctly
- [ ] Mappers convert correctly
- [ ] Firebase Auth works (sign up/in)
- [ ] Firestore operations work
- [ ] Storage upload works
- [ ] Repositories use cache-first strategy
- [ ] All operations are async (no blocking)
- [ ] API services work (weather, places)
- [ ] API repository works

### Yaron's Testing (UI Layer)
- [ ] All fragments display correctly
- [ ] Navigation works with Safe Args
- [ ] Loading indicators show/hide correctly
- [ ] Error messages display correctly
- [ ] Images load with Coil
- [ ] User can create/edit/delete own posts
- [ ] User cannot edit others' posts
- [ ] Profile editing works
- [ ] Auto-login works
- [ ] Logout works
- [ ] Weather displays in post details
- [ ] Nearby places display in post details
- [ ] Trip builder works

### Joint Testing (Integration)
- [ ] End-to-end user flows work
- [ ] Offline functionality works (cached data shows)
- [ ] Performance is acceptable
- [ ] All requirements met
- [ ] No crashes or bugs

---

## Timeline Estimate

### Week 1 (Days 1-5)
- **Alon:** Steps 1-6 (Foundation, Data Layer)
- **Yaron:** Wait for Step 6, then start Step 7

### Week 2 (Days 6-10)
- **Alon:** Step 12.1-12.2 (API Setup)
- **Yaron:** Steps 8-11, 12.3 (UI Features + API Integration)

### Week 3 (Days 11-13)
- **Yaron:** Step 13 (Trip Builder)
- **Alon:** Step 14 (Final Polish)
- **Both:** Joint testing and bug fixes

---

## Final Recommendations

1. ✅ **Alon completes Step 6 by Day 4** to unblock Yaron
2. ✅ **Daily sync meetings** to coordinate
3. ✅ **Alon creates repository stubs early** if possible (optional but helpful)
4. ✅ **Both test together** before final submission
5. ✅ **Use feature branches** and merge to develop frequently
6. ✅ **Document any issues** or questions

This split is **optimal** because:
- Clear ownership of layers
- No blocking dependencies
- Natural API integration (Yaron owns the UI that needs it)
- Alon can verify requirements at the end

---

## Questions or Issues?

If you encounter:
- **Repository interface questions** → Ask Alon
- **UI/UX questions** → Ask Yaron
- **Architecture questions** → Discuss together
- **Blockers** → Communicate immediately

**Good luck with the implementation!** 🚀

