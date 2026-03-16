# Group 4 Branching Strategy

## Overview

This repository uses a **simplified branching strategy** adapted to our current project setup.

The goals are:

- keep the stable project state separated from ongoing development
- support parallel work in the team
- ensure CI checks run before merging
- keep commits and pull requests traceable
- avoid starting work from outdated branches

---

## Main Branches

### `main`

The `main` branch represents the **stable project state**.

Characteristics:

- always intended to be stable and presentation-ready
- contains only reviewed and integrated work
- pull requests into `main` trigger the current GitHub Actions setup
- should not receive direct commits
- should be tagged for stable milestones or releases

Typical usage:

- feature branches are currently merged into `main`
- documentation branches are currently merged into `main`
- bugfix branches are currently merged into `main`
- urgent fixes are merged into `main`

---

### `develop` *(optional integration branch)*

The `develop` branch is currently treated as an **optional integration branch**, not as the default base branch for new work.

---

## Working Branches

Short-lived branches are used for clearly scoped tasks. Each branch should represent **one clear piece of work**.

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

- create from the latest `main`
- open pull requests into `main`
- delete after merge

---

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

- create from the latest `main`
- open pull requests into `main`
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

- create from the latest `main`
- open pull requests into `main`
- delete after merge

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

- create from `main`
- merge back into `main`
- use only for urgent fixes that should be integrated immediately
- if `develop` is actively maintained later, it must be synchronized separately

---

## Summary of Branch Roles

```text
main        stable project state and default PR target
develop     optional integration branch, only if kept in sync
feature/*   new functionality
docs/*      project documentation
bugfix/*    development bug fixes
hotfix/*    urgent fix for main
```

Current default flow:

```text
feature/*  -> main
docs/*     -> main
bugfix/*   -> main
hotfix/*   -> main
```

---

## Working Workflow

### 1. Always update the branch base first

Before creating a new branch, always fetch and pull first.

Default workflow:

```bash
git checkout main
git fetch origin
git pull origin main
```

Important:

- do not start a new branch from an outdated local branch
- be especially careful with `develop`, because it is currently behind `main`

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

Example for a bug fix:

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

Open a pull request into:

```text
main
```

Reason:

- GitHub Actions currently run for pull requests into `main`
- this ensures checks are executed before merge

If the team later reactivates `develop` and configures CI for it, the pull request target can be reconsidered.

---

### 6. Merge after review

General rule:

- no direct commits to `main`
- use short-lived task branches
- keep each branch focused on one clear purpose
- prefer pull requests over direct pushes

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
- prefer pull requests for all changes
- each branch should have one clear purpose
- delete short-lived branches after merge

---

## Conflict Handling

If `main` changed while you were working on a branch, update your branch before merging:

```bash
git checkout main
git fetch origin
git pull origin main

git checkout feature/device-control
git merge main
```

Then:

- resolve conflicts locally
- test again
- commit the conflict resolution
- push the updated branch

---
