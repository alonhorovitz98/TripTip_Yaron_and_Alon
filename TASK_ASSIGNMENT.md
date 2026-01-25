# TripTip - Quick Task Assignment Reference

## Task Split (Option 2: Adjusted)

### 👤 Alon's Tasks
**Steps: 1-6, 12.1-12.2, 14**

| Step | Task | Estimated Time |
|------|------|----------------|
| 1 | Project Foundation & Dependencies | Day 1 |
| 2 | Domain Models & Utilities | Day 1-2 |
| 3 | Room Database Setup | Day 2 |
| 4 | Navigation Setup | Day 2 |
| 5 | Firebase Remote Data Sources | Day 3 |
| 6 | Repository Layer | Day 3-4 |
| 12.1-12.2 | API Setup (Services + Repository) | Day 9-10 |
| 14 | Final Polish & Verification | Day 12-13 |

**Total: ~6-7 days**

---

### 👤 Yaron's Tasks
**Steps: 7-11, 12.3, 13**

| Step | Task | Estimated Time |
|------|------|----------------|
| 7 | Authentication UI | Day 4-5 |
| 8 | Feed UI | Day 5-6 |
| 9 | Post Details & Create Post | Day 6-7 |
| 10 | My Posts & Edit Post | Day 7-8 |
| 11 | Profile UI | Day 8-9 |
| 12.3 | API Integration in UI | Day 10 |
| 13 | Trip Builder UI | Day 10-12 |

**Total: ~6-7 days**

---

## Critical Handoff Points

### 🎯 Handoff 1: After Step 6 (Day 4)
- **Alon:** Complete all repositories
- **Yaron:** Start UI work
- **Action:** Alon demonstrates repository usage

### 🎯 Handoff 2: After Step 12.2 (Day 10)
- **Alon:** Complete API services and repository
- **Yaron:** Integrate APIs into PostDetailsFragment
- **Action:** Alon explains API repository usage

### 🎯 Handoff 3: Before Step 14 (Day 12)
- **Both:** Complete all features
- **Alon:** Final verification
- **Action:** Joint testing session

---

## Key Dependencies

```
Alon's Work:
1-6 → 12.1-12.2 → 14

Yaron's Work:
7 → 8 → 9 → 10 → 11 → 12.3 → 13

Dependencies:
Yaron Step 7+ depends on Alon Step 6
Yaron Step 12.3 depends on Alon Step 12.2
Alon Step 14 depends on Yaron Step 13
```

---

## Daily Sync Questions

1. What did you complete today?
2. What are you working on next?
3. Any blockers?
4. Any interface changes needed?
5. Ready to test together?

---

## Git Branches

- `main` - Production code
- `develop` - Integration branch
- `feature/data-layer` (Alon)
- `feature/apis` (Alon)
- `feature/auth-ui` (Yaron)
- `feature/feed-ui` (Yaron)
- `feature/posts-ui` (Yaron)
- `feature/api-integration-ui` (Yaron)
- `feature/trips-ui` (Yaron)

---

## Quick Requirements Checklist

- ✅ All network operations async (Flow/suspend)
- ✅ Cache-first strategy (Room → Firestore)
- ✅ Fragment-based navigation with Safe Args
- ✅ Loading indicators everywhere
- ✅ User ownership verification
- ✅ Lazy loading implemented
- ✅ Room for local storage
- ✅ Firebase for remote only
- ✅ Coil/Picasso for image loading only

