# Git Branch Workflow Guide

This guide explains how to work with two branches: `ignite` and `postgres` for the IgniteVSPostgres project.

## Initial Setup

### 1. Create the branches (if they don't exist yet)

```bash
# Check current branch
git branch

# Create ignite branch from current state
git checkout -b ignite

# Create postgres branch from current state
git checkout -b postgres

# Or create both from main/master
git checkout main  # or master
git checkout -b ignite
git checkout -b postgres
```

## Switching Between Branches

### Switch to ignite branch
```bash
git checkout ignite
```

### Switch to postgres branch
```bash
git checkout postgres
```

### Quick switch to previous branch
```bash
git checkout -
```

## Checking Current Branch

```bash
# Show current branch
git branch

# Show current branch with more details
git status
```

## Pushing Changes

### Push to ignite branch
```bash
# First time pushing the branch
git push -u origin ignite

# Subsequent pushes
git push
```

### Push to postgres branch
```bash
# First time pushing the branch
git push -u origin postgres

# Subsequent pushes
git push
```

### Force push (use with caution!)
```bash
git push --force
# or safer option
git push --force-with-lease
```

## Pulling Changes

### Pull from ignite branch
```bash
git checkout ignite
git pull origin ignite
```

### Pull from postgres branch
```bash
git checkout postgres
git pull origin postgres
```

### Pull with rebase (cleaner history)
```bash
git pull --rebase origin ignite
```

## Committing Changes

### Standard workflow
```bash
# Check what changed
git status

# Add specific files
git add path/to/file

# Or add all changes
git add .

# Commit with message
git commit -m "Your descriptive commit message"

# Push to current branch
git push
```

## Merging Changes

### Merge postgres into ignite
```bash
# Switch to the branch you want to merge INTO
git checkout ignite

# Merge postgres branch into ignite
git merge postgres

# If there are conflicts, resolve them, then:
git add .
git commit -m "Merge postgres into ignite"

# Push the merged changes
git push
```

### Merge ignite into postgres
```bash
# Switch to postgres branch
git checkout postgres

# Merge ignite branch into postgres
git merge ignite

# Resolve conflicts if any, then:
git add .
git commit -m "Merge ignite into postgres"

# Push the merged changes
git push
```

### Merge with squash (cleaner history)
```bash
git checkout ignite
git merge --squash postgres
git commit -m "Merge postgres features into ignite"
git push
## Merging with Main Branch

### Option 1: Merge main into your feature branch (Recommended for keeping branches updated)

This brings the latest changes from main into your ignite or postgres branch:

```bash
# Update ignite branch with main
git checkout ignite
git pull origin main  # Get latest main changes
git merge main
# Resolve conflicts if any
git push origin ignite

# Update postgres branch with main
git checkout postgres
git pull origin main  # Get latest main changes
git merge main
# Resolve conflicts if any
git push origin postgres
```

### Option 2: Merge your feature branch into main (When ready to release)

This brings your ignite or postgres changes into the main branch:

```bash
# Merge ignite into main
git checkout main
git pull origin main  # Make sure main is up to date
git merge ignite
# Resolve conflicts if any
git push origin main

# Merge postgres into main
git checkout main
git pull origin main  # Make sure main is up to date
git merge postgres
# Resolve conflicts if any
git push origin main
```

### Option 3: Rebase onto main (Cleaner history)

Rebase replays your commits on top of main, creating a linear history:

```bash
# Rebase ignite onto main
git checkout ignite
git fetch origin
git rebase origin/main
# Resolve conflicts if any (for each commit)
git push --force-with-lease origin ignite

# Rebase postgres onto main
git checkout postgres
git fetch origin
git rebase origin/main
# Resolve conflicts if any (for each commit)
git push --force-with-lease origin postgres
```

**⚠️ Warning:** Only use rebase on branches you're working on alone. Never rebase shared branches.

### Option 4: Create Pull Request (Best practice for team collaboration)

Instead of merging directly, create a pull request:

```bash
# Push your branch
git checkout ignite
git push origin ignite

# Then on GitHub/GitLab/Bitbucket:
# 1. Go to your repository
# 2. Click "New Pull Request" or "Merge Request"
# 3. Select: base: main <- compare: ignite
# 4. Review changes and create PR
# 5. After approval, merge via the web interface
```

### Complete Workflow Example: Keeping branches in sync with main

```bash
# 1. Start with main
git checkout main
git pull origin main

# 2. Update ignite from main
git checkout ignite
git merge main
# Resolve conflicts if any
git push origin ignite

# 3. Update postgres from main
git checkout postgres
git merge main
# Resolve conflicts if any
git push origin postgres

# 4. Now both branches have latest main changes
```

### Merge Strategy Comparison

| Strategy | Use When | Pros | Cons |
|----------|----------|------|------|
| **Merge main into feature** | Keeping feature branch updated | Preserves history, safe | Creates merge commits |
| **Merge feature into main** | Ready to release | Simple, straightforward | Can clutter main history |
| **Rebase onto main** | Want clean history | Linear history, clean | Rewrites history, risky if shared |
| **Pull Request** | Team collaboration | Code review, discussion | Requires web interface |

```

## Viewing Differences

### Compare branches
```bash
# See differences between ignite and postgres
git diff ignite..postgres

# See file names that differ
git diff --name-only ignite..postgres

# See differences for specific file
git diff ignite..postgres -- path/to/file
```

### Compare with remote
```bash
# Compare local ignite with remote ignite
git diff ignite origin/ignite
```

## Handling Merge Conflicts

When you encounter merge conflicts:

1. **Identify conflicted files**
```bash
git status
```

2. **Open conflicted files** - they will contain markers like:
```
<<<<<<< HEAD
Your current branch changes
=======
Changes from the branch being merged
>>>>>>> branch-name
```

3. **Resolve conflicts** - edit the file to keep what you want

4. **Mark as resolved**
```bash
git add path/to/resolved/file
```

5. **Complete the merge**
```bash
git commit -m "Resolved merge conflicts"
git push
```

6. **Abort merge if needed**
```bash
git merge --abort
```

## Cherry-picking Commits

To copy specific commits from one branch to another:

```bash
# Switch to target branch
git checkout ignite

# Cherry-pick a commit from postgres
git cherry-pick <commit-hash>

# Cherry-pick multiple commits
git cherry-pick <commit-hash1> <commit-hash2>

# Push changes
git push
```

## Viewing Branch History

```bash
# View commit history
git log

# View compact history
git log --oneline

# View branch history with graph
git log --graph --oneline --all

# View history of specific branch
git log postgres

# Compare commits between branches
git log ignite..postgres
```

## Stashing Changes

When you need to switch branches but have uncommitted changes:

```bash
# Stash current changes
git stash

# Switch branch
git checkout postgres

# Later, return and apply stashed changes
git checkout ignite
git stash pop

# List all stashes
git stash list

# Apply specific stash
git stash apply stash@{0}
```

## Syncing with Remote

### Update all remote branches
```bash
git fetch origin

# See all branches (local and remote)
git branch -a
```

### Update local branch from remote
```bash
git checkout ignite
git pull origin ignite
```

## Best Practices

1. **Always commit before switching branches**
   - Or use `git stash` to save work in progress

2. **Pull before pushing**
   ```bash
   git pull origin ignite
   git push origin ignite
   ```

3. **Use descriptive commit messages**
   ```bash
   git commit -m "feat: Add Ignite cache configuration"
   git commit -m "fix: Resolve PostgreSQL connection timeout"
   ```

4. **Keep branches up to date**
   - Regularly merge or rebase from main/master
   ```bash
   git checkout ignite
   git merge main
   ```

5. **Review changes before committing**
   ```bash
   git diff
   git status
   ```

## Common Workflows

### Workflow 1: Develop feature in ignite, merge to postgres
```bash
# Work on ignite
git checkout ignite
# ... make changes ...
git add .
git commit -m "Add Ignite-specific feature"
git push origin ignite

# Merge to postgres
git checkout postgres
git merge ignite
git push origin postgres
```

### Workflow 2: Keep branches separate, cherry-pick shared code
```bash
# Work on ignite
git checkout ignite
# ... make changes ...
git commit -m "Add shared utility class"
git push origin ignite

# Copy specific commit to postgres
git checkout postgres
git cherry-pick <commit-hash>
git push origin postgres
```

### Workflow 3: Sync both branches with main
```bash
# Update ignite from main
git checkout ignite
git merge main
git push origin ignite

# Update postgres from main
git checkout postgres
git merge main
git push origin postgres
```

## Troubleshooting

### Undo last commit (keep changes)
```bash
git reset --soft HEAD~1
```

### Undo last commit (discard changes)
```bash
git reset --hard HEAD~1
```

### Discard all local changes
```bash
git checkout .
```

### Delete local branch
```bash
git branch -d branch-name
# Force delete
git branch -D branch-name
```
| `git merge main` | Merge main into current branch |
| `git rebase main` | Rebase current branch onto main |

### Delete remote branch
```bash
git push origin --delete branch-name
```

### Recover deleted branch
```bash
git reflog
git checkout -b branch-name <commit-hash>
```

## Quick Reference

| Command | Description |
|---------|-------------|
| `git checkout ignite` | Switch to ignite branch |
| `git checkout postgres` | Switch to postgres branch |
| `git push origin ignite` | Push ignite branch |
| `git pull origin postgres` | Pull postgres branch |
| `git merge postgres` | Merge postgres into current branch |
| `git diff ignite..postgres` | Compare branches |
| `git stash` | Save uncommitted changes |
| `git stash pop` | Restore stashed changes |
| `git log --oneline` | View commit history |
| `git status` | Check current status |
