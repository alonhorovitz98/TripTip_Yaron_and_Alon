# Git Workflow - TripTip Project

## 🌿 Branch Structure

### Main Branches
- **`main`** - Production-ready code (protected)
- **`yaron/dev`** - Yaron's development branch
- **`alon/dev`** - Alon's development branch

## 📋 Workflow Process

### For Yaron:
1. **Work on your branch:**
   ```bash
   git checkout yaron/dev
   git pull origin yaron/dev
   ```

2. **Make changes and commit:**
   ```bash
   git add -A
   git commit -m "feat: description of feature"
   git push origin yaron/dev
   ```

3. **Create Pull Request:**
   - Go to GitHub repository
   - Click "Pull Requests" → "New Pull Request"
   - Base: `main` ← Compare: `yaron/dev`
   - Add description of changes
   - Assign Alon as reviewer
   - Create PR

4. **After Alon approves:**
   - PR will be merged to `main`
   - Update your branch:
   ```bash
   git checkout yaron/dev
   git pull origin main
   ```

### For Alon:
1. **Work on your branch:**
   ```bash
   git checkout alon/dev
   git pull origin alon/dev
   ```

2. **Make changes and commit:**
   ```bash
   git add -A
   git commit -m "feat: description of feature"
   git push origin alon/dev
   ```

3. **Create Pull Request:**
   - Same process as Yaron
   - Base: `main` ← Compare: `alon/dev`
   - Assign Yaron as reviewer

4. **Review Yaron's PRs:**
   - Go to Pull Requests tab
   - Review code changes
   - Approve or request changes
   - Merge when ready

## 🧹 Clean Up Old Branches

### Delete local branches you don't need:
```bash
# List all local branches
git branch

# Delete a branch
git branch -D branch-name
```

### Delete remote branches:
```bash
# Delete remote branch
git push origin --delete branch-name
```

## ✅ Best Practices

1. **Always pull before starting work:**
   ```bash
   git pull origin yaron/dev  # or alon/dev
   ```

2. **Commit messages format:**
   - `feat:` - New feature
   - `fix:` - Bug fix
   - `docs:` - Documentation changes
   - `refactor:` - Code refactoring
   - `test:` - Adding tests

3. **Keep PRs focused:**
   - One feature/fix per PR
   - Clear description of changes
   - Reference related issues if any

4. **Sync with main regularly:**
   ```bash
   git checkout yaron/dev
   git pull origin main
   git push origin yaron/dev
   ```

## 🚫 Rules

- ❌ **Never** commit directly to `main`
- ❌ **Never** merge your own PR without review
- ✅ **Always** create PR for changes
- ✅ **Always** wait for approval before merging
- ✅ **Always** sync with main after merge

## 📌 Current Setup Commands

### First Time Setup (run once):
```bash
# Make sure you're on latest main
git checkout master
git pull origin master

# Create your development branch
git checkout -b yaron/dev
git push -u origin yaron/dev

# Alon creates his branch
git checkout -b alon/dev
git push -u origin alon/dev
```

### Daily Workflow:
```bash
# Start your day
git checkout yaron/dev
git pull origin yaron/dev
git pull origin main  # Get latest from main

# After making changes
git add -A
git commit -m "feat: your feature description"
git push origin yaron/dev

# Then create PR on GitHub
```

## 🔄 After PR is Merged

```bash
# Update your dev branch with merged changes
git checkout yaron/dev
git pull origin main
git push origin yaron/dev
```

---

זרימת עבודה נקייה ומסודרת! 🎉
