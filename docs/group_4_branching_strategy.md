# Group 4 Branching Strategy

## Overview

<<<<<<< Updated upstream
This repository uses a **GitFlow-inspired branching strategy** based on the model introduced in class.  
It separates stable releases, integration work, feature development, and emergency fixes.
=======
This repository uses a **simple branching strategy** adapted to our current project setup.
>>>>>>> Stashed changes

The goals are:

- keep the stable project state separated from ongoing development
- support parallel work in the team
- make pull requests easier to review
- keep releases and fixes traceable

---

## Main Branches

### `main`

The `main` branch represents the **stable project state**.

Characteristics:
- always intended to be stable and presentation-ready
- contains only reviewed and integrated work
- should not receive direct commits
- should be tagged for stable releases

<<<<<<< Updated upstream
Typical usage:
- receives merges from `release/*`
- receives merges from `hotfix/*`

---

### `develop`

The `develop` branch is the **main integration branch** for ongoing work.

Characteristics:
- collects completed development work for the next release
- may temporarily contain unfinished or not-yet-released changes
- serves as the base branch for most task branches

Typical usage:
- feature branches always start from `develop`
- documentation branches usually start from `develop`
- development bug fixes usually start from `develop`

=======
>>>>>>> Stashed changes
---

## Supporting Branches

### `feature/*`

Used for implementing new functionality.

Naming format:

```text
feature/<short-description>
```

Examples:

```text
feature/login
feature/create-room
feature/device-management
feature/change-device-state
feature/rules-scheduling
```

Rules:
- branch off from `develop`
- merge back into `develop`
- delete after merge

---

### `release/*`

Used to prepare a new release.

Naming format:

```text
release/<version-or-milestone>
```

Examples:

```text
release/1.0
release/m1-demo
```

Typical tasks:
- release preparation
- final polishing
- minor bug fixes
- release-related metadata

Rules:
- branch off from `develop`
- merge back into both `develop` and `main`
- tag the release on `main`

---

### `hotfix/*`

Used for urgent fixes of problems already present in `main`.

Naming format:

```text
hotfix/<short-description>
```

Examples:

```text
hotfix/login-crash
hotfix/device-state-corruption
```

Rules:
- branch off from `main`
- merge back into both `main` and `develop`
- use only for urgent production / release fixes

---

## Project-Specific Extension Branches

The following branch types are team conventions added on top of the GitFlow model for better structure in this project.

### `docs/*`

Used for documentation work.

Naming format:

```text
docs/<short-description>
```

Examples:

```text
docs/user-stories
docs/domain-model
docs/uml-grobkonzept
docs/presentation-20-03
```

Typical content:
- UML diagrams
- architecture documentation
- presentation material
- requirements descriptions

Rules:
- usually branch off from `develop`
- merge back into `develop`
- delete after merge

---

### `bugfix/*`

Used for non-critical bug fixes during normal development.

Naming format:

```text
bugfix/<short-description>
```

Examples:

```text
bugfix/device-state-update
bugfix/login-validation
```

Rules:
- usually branch off from `develop`
- merge back into `develop`
- not intended for urgent fixes on `main`
- urgent fixes on `main` must use `hotfix/*`

---

## Summary of Branch Roles

```text
main        stable released state
develop     integration branch for next release
feature/*   new functionality
release/*   release preparation
hotfix/*    urgent fix for main
docs/*      project documentation
bugfix/*    development bug fixes
```

Typical flow:

```text
<<<<<<< Updated upstream
feature/*  -> develop
docs/*     -> develop
bugfix/*   -> develop
release/*  -> main and develop
hotfix/*   -> main and develop
=======
feature/*  -> main
docs/*     -> main
bugfix/*   -> main
hotfix/*   -> main
>>>>>>> Stashed changes
```

---

## Working Workflow

### 1. Start from `develop`

Before creating a new branch:

```bash
git checkout develop
git pull origin develop
```

---

### 2. Create a working branch

Example for a feature:

```bash
git checkout -b feature/device-control
```

Example for documentation:

```bash
git checkout -b docs/uml-grobkonzept
```

Example for a development bug fix:

```bash
git checkout -b bugfix/login-validation
```

---

### 3. Work and commit locally

Commit regularly in small, meaningful steps.

Example:

```bash
git add .
git commit -m "#24 2h Add UML rough concept diagrams"
```

---

### 4. Push the branch

```bash
git push -u origin docs/uml-grobkonzept
```

After the upstream has been set, future pushes only require:

```bash
git push
```

---

### 5. Open a Pull Request

Open a pull request to merge the branch into:

```text
develop
```

For release branches:
- merge into `main`
- then back into `develop`

<<<<<<< Updated upstream
For hotfix branches:
- merge into `main`
- then back into `develop`
=======
- GitHub Actions currently run for pull requests into `main`
- this ensures checks are executed before merge
>>>>>>> Stashed changes

---

### 6. Merge after review

General rule:
- no direct commits to `main`
- preferably no direct commits to `develop`
- use short-lived task branches
- keep each branch focused on one clear purpose

---

## Team Commit Message Convention

To keep commits traceable, we use the format:

```text
#<issue-number> <time-spent> <short-description>
```

Examples:

```text
#24 2h Add UML rough concept diagrams
#18 1h Implement room creation logic
#12 0.5h Fix login validation
#30 1h Update user stories
```

---

## Merge Rules

- never commit directly to `main`
- avoid direct commits to `develop`
- always prefer pull requests
- each branch should have one clear purpose
- delete short-lived branches after merge

---

## Conflict Handling

If `develop` changed while you were working on a branch, update your branch before merging:

```bash
git checkout develop
git pull origin develop

git checkout feature/device-control
git merge develop
```

Then:
- resolve conflicts locally
- test again
- commit the conflict resolution
- push the updated branch

---
<<<<<<< Updated upstream

## Notes

This strategy follows the GitFlow principles discussed in class:
- long-lived `main` and `develop`
- feature branches from `develop`
- release branches from `develop`
- hotfix branches from `main`

The additional branch types `docs/*` and `bugfix/*` are project-specific conventions introduced to keep the repository more structured for this course project.

=======
>>>>>>> Stashed changes
