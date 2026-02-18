# TripTip Stitch UI Implementation Plan

## Overview
This document outlines the complete plan to transform the TripTip Android app to match the Stitch design exactly. The design features a modern, clean aesthetic with orange accents, bottom navigation, and professional UI components.

---

## 1. Design System

### 1.1 Color Palette
**Primary Colors:**
- **Orange Primary**: `#FF6B35` or similar vibrant orange (used for buttons, accents, selected states)
- **Orange Light**: `#FFF4E6` or similar light orange (used for backgrounds, highlights)
- **White**: `#FFFFFF` (backgrounds, cards)
- **Black/Dark Gray**: `#000000` / `#333333` (text, icons)
- **Light Gray**: `#F5F5F5` / `#E0E0E0` (input fields, borders)
- **Gray Text**: `#666666` / `#999999` (secondary text, placeholders)
- **Turquoise/Teal**: `#40E0D0` or similar (location tags, map backgrounds)

**Status Colors:**
- **Success**: Green variants
- **Error**: Red variants
- **Warning**: Orange/Yellow variants

### 1.2 Typography
- **Font Family**: Sans-serif (Roboto or similar)
- **Headings**: Bold, 24-32sp
- **Body Text**: Regular, 14-16sp
- **Captions**: Regular/Light, 12-14sp
- **Labels**: Bold/Uppercase, 12-14sp

### 1.3 Spacing & Layout
- **Card Padding**: 16dp
- **Screen Padding**: 16-24dp
- **Element Spacing**: 8dp, 16dp, 24dp
- **Card Corner Radius**: 12-16dp
- **Button Corner Radius**: 8-12dp
- **Card Elevation**: 2-4dp

### 1.4 Components
- **Bottom Navigation**: Fixed at bottom, 5 items (Home, Explore, Create, Plan, Profile)
- **Top App Bar**: Custom toolbar with logo, title, action icons
- **Cards**: White background, rounded corners, subtle shadow
- **Buttons**: Orange filled buttons, white text
- **Input Fields**: Rounded, light background, subtle borders
- **FAB**: Large circular orange button with white plus icon

---

## 2. Navigation Structure

### 2.1 Bottom Navigation Bar
**Always visible on main screens, hidden on:**
- Login/Register screens
- Post Details (full-screen)
- Create Post screen
- Trip Builder screen
- Edit screens

**Navigation Items:**
1. **Home/Feed** (house icon) - `feedFragment`
2. **Explore** (compass icon) - `exploreFragment` (NEW - needs creation)
3. **Create** (orange plus in circle) - `createPostFragment`
4. **Plan/My Trips** (calendar/suitcase icon) - `tripListFragment`
5. **Profile** (person icon) - `profileFragment`

### 2.2 Top App Bar Structure
**Feed Screen:**
- Left: TripTip logo (orange compass icon + text)
- Right: Bell icon (notifications), Message icon

**Other Screens:**
- Left: Back arrow
- Center: Screen title
- Right: Action icons (varies by screen)

---

## 3. Screen-by-Screen Implementation

### 3.1 Splash/Onboarding Screen (NEW)
**File**: `fragment_splash.xml`, `SplashFragment.kt`

**Design:**
- Full-screen orange background (`#FF6B35`)
- Centered logo: White rounded square with orange paper airplane icon
- Small orange circle with white heart icon overlapping bottom-right
- "TripTip" text in white, bold, large
- Tagline: "PLAN • SHARE • EXPLORE" in white, smaller
- Three navigation dots at bottom (indicates 3 onboarding screens)

**Implementation:**
- Create `SplashFragment` with ViewPager2 for onboarding
- Auto-navigate to Login after 3 seconds or on tap
- Store onboarding completion in SharedPreferences

---

### 3.2 Login Screen
**File**: `fragment_login.xml`, `LoginFragment.kt`

**Current State**: Basic implementation exists
**Required Changes:**

**Layout:**
- Remove top toolbar (full-screen)
- Add back arrow at top-left
- Center "TripTip" title at top
- Large "Welcome back!" heading
- Subtitle: "Ready for your next adventure?"
- Email input: Rounded, light background, placeholder "e.g. wanderlust@example.com"
- Password input: Rounded, light background, eye icon for visibility toggle
- "FORGOT?" link in orange, aligned right with password label
- Orange "Login" button, full-width, rounded
- "Or continue with" divider with horizontal lines
- Two social buttons: Google (white, border) and Apple (white, border)
- Bottom: "Don't have an account? Register" (Register in orange)

**Styling:**
- Light beige/off-white background
- Orange accent color for buttons and links
- Rounded input fields (12dp corner radius)

---

### 3.3 Register Screen
**File**: `fragment_register.xml`, `RegisterFragment.kt`

**Current State**: Basic implementation exists
**Required Changes:**

**Layout:**
- Back arrow at top-left
- "Register Account" title centered
- Large circular profile picture placeholder (light green background, white frame illustration)
- Small orange circle with white pencil icon overlapping bottom-right
- "Join the community" text (bold, dark)
- "Upload a profile photo" text (orange, smaller)
- Full Name input: Rounded, placeholder "What should we call you?"
- Email input: Rounded, placeholder "example@email.com"
- Password input: Rounded, placeholder "Min. 8 characters", eye icon
- Checkbox: "By creating an account, you agree to our Terms of Service and Privacy Policy" (links in orange)
- Orange "Create Account" button with icon on right
- "OR SIGN UP WITH" divider
- Two social buttons: Google and Apple
- Bottom: "Already have an account? Log In" (Log In in orange)

---

### 3.4 Feed Screen
**File**: `fragment_feed.xml`, `FeedFragment.kt`

**Current State**: Basic RecyclerView implementation
**Required Changes:**

**Top Section:**
- Custom toolbar (not AppBarLayout):
  - Left: Orange circular icon with white compass symbol + "TripTip" text
  - Right: Bell icon (notifications), Message icon (speech bubble)
- Filter bar below toolbar:
  - Horizontal row of 4 pill-shaped buttons
  - "Trending" (selected - orange background, white text)
  - "Following", "Nearby", "Solo Travel" (unselected - light orange border, orange text)

**Post Cards:**
- White card with rounded corners (12dp)
- User info row:
  - Circular profile picture (left)
  - Username in dark gray (e.g., "@marco_polo")
  - Time ago in light gray (e.g., "2 hours ago")
  - Three-dot menu icon (right)
- Large rectangular image (full-width, rounded top corners)
- Location tag overlay (top-left of image):
  - Turquoise pill-shaped badge
  - White location pin icon
  - Location name in white (e.g., "Turquoise, Greece")
- Engagement row (below image):
  - Orange heart icon + count (e.g., "1.2k")
  - Orange comment icon + count (e.g., "48")
  - Orange share icon (paper airplane)
  - Orange bookmark icon (save)
- Description text: Dark gray, supports hashtags and mentions

**Bottom:**
- FAB: Large circular orange button with white plus icon (bottom-right)
- Bottom navigation bar (see section 2.1)

**Post Item Layout**: Update `item_post.xml` to match design

---

### 3.5 Create Post Screen
**File**: `fragment_create_post.xml`, `CreatePostFragment.kt`

**Current State**: Basic implementation exists
**Required Changes:**

**Top App Bar:**
- Left: "Cancel" text button (black)
- Center: "Create Post" title
- Right: Orange rounded button "Publish"

**Content:**
- Photo/Video upload area:
  - Large rectangular area with dashed light gray border
  - Rounded corners (12dp)
  - Centered: Large orange camera icon with white plus in light orange circle
  - Text: "Add photos or videos" (black)
  - Subtext: "Up to 10 files" (light gray)
- "CAPTION" label (bold, uppercase, dark gray)
- Caption input: Light gray background, placeholder "Where did you go? Share the magic of your trip..."
- Location input bar:
  - Light gray background, rounded
  - Orange map pin icon (left)
  - "Add a location" text (light gray)
  - Gray arrow icon (right)
- "TAGS" label (bold, uppercase, dark gray)
- Tags input field (to be implemented)

**Bottom:**
- Bottom navigation bar (Create item highlighted in orange)

---

### 3.6 Post Details Screen
**File**: `fragment_post_details.xml`, `PostDetailsFragment.kt`

**Current State**: Basic implementation exists
**Required Changes:**

**Layout Structure:**
- CoordinatorLayout for overlapping effect
- Top image section (40% of screen):
  - Full-width image (e.g., ocean scene)
  - Overlay icons (top):
    - Back arrow (circular white button, top-left)
    - Share icon (circular white button, top-right)
    - Heart icon (circular white button, top-right)
- Information card (60% of screen, white, rounded top corners):
  - Overlaps bottom of image
  - Orange pill tag: "ISLAND ESCAPE" (top of card)
  - Title: Large, bold, black (e.g., "Crystal Waters of Maya Bay")
  - Author row:
    - Circular profile picture
    - Name in bold (e.g., "Alex Wanderer")
    - Location + time (e.g., "Maldives • 2 days ago")
  - Description text (multi-paragraph, gray)
  - "CURRENT WEATHER" section:
    - Label (uppercase, gray)
    - Temperature: Large, bold (e.g., "28°C")
    - Condition: "Sunny" with orange sun icon
    - Wind: "12km/h" with wind icon
    - Humidity: "64%" with droplet icon
  - "Location" section:
    - Label
    - Map card: Teal/dark green background, abstract map with orange pin
  - Pricing section:
    - "Starting from" (small, gray)
    - "$1,250 /pp" (large, bold)
    - Avatar cluster with "+12" overlay
  - Bottom: Orange "Add to Trip" button (full-width, rounded)

**Implementation:**
- Use CoordinatorLayout with AppBarLayout for collapsing effect
- Custom map view or static map image
- Weather API integration (already exists)
- Avatar cluster custom view

---

### 3.7 My Trips Screen
**File**: `fragment_trip_list.xml`, `TripListFragment.kt`

**Current State**: Basic implementation exists
**Required Changes:**

**Top Section:**
- Left: Circular icon (light orange background, white document icon) + "My Trips" title (large, bold)
- Right: Bell icon, Settings icon (gear)
- Search bar below:
  - Full-width, rounded
  - Orange magnifying glass icon (left)
  - Placeholder: "Search your adventures..." (light gray)

**Featured Adventure Section:**
- Heading: "Featured Adventure" (bold) + "NEXT STOP" badge (orange, right-aligned)
- Large card:
  - Full-width, rounded (12dp)
  - Background image (e.g., Tokyo cityscape at night)
  - Orange badge overlay: "IN 12 DAYS" (top-left, white text)
  - Title: "Tokyo Adventure" (large, white, bold, overlaid)
  - Date: Calendar icon + "Oct 12-22" (white)
  - Duration: Clock icon + "10 Days" (white)

**Upcoming Trips Section:**
- Heading: "Upcoming Trips" (bold) + "See all" link (orange, right-aligned)
- Horizontal RecyclerView (scrollable):
  - Cards: Rounded, smaller than featured
  - Image (top)
  - Orange badge: "5 DAYS" (top-right)
  - Title (below image)
  - Date (below title, gray)

**Bottom:**
- FAB: Large orange rounded rectangle button with white plus icon + "Create Trip" text
- Bottom navigation bar (My Trips/Plan highlighted)

---

### 3.8 New Trip Setup Screen
**File**: `fragment_new_trip_setup.xml`, `NewTripSetupFragment.kt` (NEW)

**Design:**
- Top App Bar:
  - Left: X icon (close)
  - Center: "New Trip Setup" title
  - Right: "Next" text button (orange)
- Trip cover section:
  - Label: "Trip cover"
  - Large rectangular image area (rounded, dashed border)
  - Image with camera icon overlay: "Change cover photo"
- "TRIP NAME" label (uppercase, bold)
- Input field: Rounded, placeholder "e.g. Summer in Amalfi", pencil icon on right
- "DESTINATION" label (uppercase, bold)
- Input field: Rounded, orange location pin icon (left), placeholder "Where are you going?"
- Two date fields (side-by-side):
  - "START DATE": Calendar icon, date "Oct 12, 2024"
  - "END DATE": Calendar icon, date "Oct 18, 2024"
- Public Trip toggle:
  - Card: Rounded, white
  - Left: Orange globe icon
  - Text: "Public Trip" (bold) + "Allow others to see your plan" (smaller)
  - Toggle switch (right, orange when on)
- Bottom: Orange "Create My Trip" button (full-width, rounded, white arrow icon on right)

**Implementation:**
- Create new fragment (or integrate into TripBuilderFragment)
- Date picker dialogs
- Image picker for cover photo
- Toggle switch component

---

### 3.9 Trip Builder Screen
**File**: `fragment_trip_builder.xml`, `TripBuilderFragment.kt`

**Current State**: Basic implementation exists
**Required Changes:**

**Top App Bar:**
- Left: Back arrow
- Center: "Trip Builder" title
- Right: Three-dot menu

**Day Selector:**
- Horizontal row of rounded buttons
- "Day 1" (selected - orange background, white text)
- "Day 2", "Day 3", "Day 4" (unselected - light orange border, orange text)

**Timeline View:**
- Vertical timeline on left (thin orange line)
- Activity cards aligned right of timeline
- Each activity:
  - Orange circle icon on timeline (airplane, bed, fork/knife icons)
  - Card: Rounded, white
  - Small image (left, rounded corners)
  - Time in orange (e.g., "08:30 AM")
  - Title in bold (e.g., "Arrive at Paris CDG")
  - Subtitle in gray (e.g., "International Airport")
  - X icon on right (light gray circle) for delete
- Add activity button:
  - Dashed orange circle on timeline
  - Dashed card: Orange plus icon + "Add New Activity" text (orange)

**Bottom:**
- Orange "Save Full Trip" button (full-width, white folder icon on left)

**Implementation:**
- Custom timeline view or RecyclerView with custom item layout
- Drag-and-drop for reordering (optional, advanced)

---

### 3.10 Profile Screen
**File**: `fragment_profile.xml`, `ProfileFragment.kt`

**Current State**: Basic implementation exists
**Required Changes:**

**Top App Bar:**
- Left: Orange compass icon + "TripTip" text
- Right: Bell icon, Share/Logout icon (square with arrow)

**Profile Header:**
- Large circular profile picture (centered)
- Small orange camera icon overlapping bottom-right
- Name: Large, bold, black (e.g., "Alex Rivera")
- Username: Orange text (e.g., "@alex_trips")
- Bio: Two lines, gray (e.g., "Exploring the hidden gems of SE Asia 🌍. Coffee enthusiast & sunset chaser.")

**Statistics Card:**
- White card, rounded, horizontal
- Three columns with dividers:
  - "128" (large, orange) + "POSTS" (small, black)
  - "34" (large, orange) + "TRIPS" (small, black)
  - "2.4k" (large, orange) + "FOLLOWERS" (small, black)

**Action Button:**
- "Edit Profile" button: Light blue outline, rounded, pencil icon

**Content Tabs:**
- Two tabs:
  - "My Posts" (selected - orange grid icon, orange text, orange underline)
  - "Saved Trips" (unselected - gray bookmark icon, gray text)

**Image Grid:**
- 3-column grid of square images
- Represents user's posts
- Use StaggeredGridLayoutManager

**Bottom:**
- Bottom navigation bar (Profile highlighted)

---

## 4. Components to Create/Update

### 4.1 New Components
1. **Bottom Navigation Bar**
   - File: `layout/bottom_navigation.xml` (include in `activity_main.xml`)
   - Material BottomNavigationView
   - 5 items with icons and labels
   - Orange highlight for selected item

2. **Custom Top Toolbar** (for Feed)
   - Logo + title layout
   - Action icons (notifications, messages)

3. **Filter Pills** (for Feed)
   - Custom view or ChipGroup
   - Pill-shaped buttons
   - Selected/unselected states

4. **Location Tag Overlay** (for posts)
   - Custom view or ImageView overlay
   - Turquoise pill shape
   - White icon + text

5. **Avatar Cluster** (for post details)
   - Custom ViewGroup
   - Overlapping circular images
   - "+X" overlay

6. **Timeline View** (for Trip Builder)
   - Custom View or RecyclerView with custom layout
   - Vertical line + circular icons

7. **Statistics Card** (for Profile)
   - Custom layout with dividers
   - Large numbers + labels

### 4.2 Updated Components
1. **Post Item Card** (`item_post.xml`)
   - Match Stitch design exactly
   - Location tag overlay
   - Engagement icons row
   - User info row

2. **Trip Card** (`item_trip.xml`)
   - Featured trip card (large)
   - Upcoming trip cards (smaller, horizontal scroll)

3. **Input Fields**
   - Rounded corners
   - Light background
   - Proper placeholders
   - Icons where needed

---

## 5. Implementation Steps

### Phase 1: Design System Setup
1. ✅ Create/update `colors.xml` with Stitch color palette
2. ✅ Create/update `themes.xml` with Material 3 theme
3. ✅ Create `dimens.xml` for spacing, corner radius, etc.
4. ✅ Create `typography.xml` or update text styles
5. ✅ Add required icons (drawable resources or vector assets)

### Phase 2: Navigation Infrastructure
1. ✅ Update `activity_main.xml` to include BottomNavigationView
2. ✅ Create bottom navigation menu resource
3. ✅ Update `MainActivity.kt` to handle bottom navigation
4. ✅ Update navigation graph to support new flow
5. ✅ Implement navigation logic (show/hide bottom nav based on screen)

### Phase 3: Core Screens
1. ✅ **Splash/Onboarding**: Create new fragment and layout
2. ✅ **Login**: Redesign layout to match Stitch
3. ✅ **Register**: Redesign layout to match Stitch
4. ✅ **Feed**: Add top toolbar, filter bar, update post cards
5. ✅ **Create Post**: Redesign layout to match Stitch
6. ✅ **Post Details**: Implement overlapping card design
7. ✅ **My Trips**: Redesign with featured + upcoming sections
8. ✅ **New Trip Setup**: Create new fragment or integrate
9. ✅ **Trip Builder**: Update timeline view
10. ✅ **Profile**: Redesign with stats card, tabs, grid

### Phase 4: Components & Polish
1. ✅ Create all custom components (location tags, avatar clusters, etc.)
2. ✅ Update all item layouts (post, trip, etc.)
3. ✅ Add animations and transitions
4. ✅ Test on different screen sizes
5. ✅ Polish spacing, colors, typography

### Phase 5: Testing & Refinement
1. ✅ Test all navigation flows
2. ✅ Verify all screens match Stitch design
3. ✅ Fix any layout issues
4. ✅ Optimize performance
5. ✅ Final polish

---

## 6. Technical Requirements

### 6.1 Dependencies (Already in project)
- Material Design Components
- Navigation Component
- ViewBinding
- Coil (image loading)
- RecyclerView
- ConstraintLayout

### 6.2 New Dependencies (May need)
- ViewPager2 (for onboarding)
- Material Chip/ChipGroup (for filter pills)
- Any custom view libraries if needed

### 6.3 Assets Needed
- App logo (orange compass icon)
- Icons: Home, Explore, Create, Plan, Profile, Bell, Message, etc.
- Placeholder images for profile pictures, trip covers
- Map placeholder images

---

## 7. Key Design Patterns

### 7.1 Card Design
- White background
- 12-16dp corner radius
- 2-4dp elevation
- 16dp padding
- Subtle shadow

### 7.2 Button Design
- Orange filled buttons: `#FF6B35` background, white text
- Outlined buttons: Orange border, orange text
- Rounded corners: 8-12dp
- Full-width on forms, auto-width for icons

### 7.3 Input Field Design
- Light gray/beige background
- Rounded corners (12dp)
- Subtle border
- Proper placeholders
- Icons where appropriate

### 7.4 Navigation Pattern
- Bottom nav always visible on main screens
- Hide on detail/edit screens
- Top app bar varies by screen
- Back navigation consistent

---

## 8. Color Reference

```xml
<!-- colors.xml additions -->
<color name="orange_primary">#FF6B35</color>
<color name="orange_light">#FFF4E6</color>
<color name="turquoise">#40E0D0</color>
<color name="text_primary">#000000</color>
<color name="text_secondary">#666666</color>
<color name="text_hint">#999999</color>
<color name="background_light">#F5F5F5</color>
<color name="background_beige">#FAF9F6</color>
```

---

## 9. Next Steps

1. **Review this plan** with the team
2. **Prioritize screens** (start with most important)
3. **Create design tokens** (colors, spacing, typography)
4. **Set up bottom navigation** infrastructure
5. **Implement screens one by one**, testing as you go
6. **Iterate and refine** based on Stitch design

---

## 10. Notes

- All measurements should be in `dp` (density-independent pixels)
- Use Material Design 3 components where possible
- Ensure accessibility (content descriptions, proper contrast)
- Test on multiple screen sizes (phone, tablet if applicable)
- Maintain existing functionality while updating UI
- Keep code organized and maintainable

---

**Last Updated**: Based on Stitch design analysis
**Status**: Ready for implementation
