# Manual Testing Guide: FR10, FR11, FR12, FR17, FR20

## Prerequisites

- PostgreSQL running (`SMARTHOME_DB_URL`, `SMARTHOME_DB_USER`, `SMARTHOME_DB_PASSWORD`)
- App built: `mvn clean compile`
- Launch: `mvn javafx:run`
- Default owner account: `admin@smarthome.com` / `admin`

---

# FR20 — User Management

**What it does:** Invite, revoke, and restore user accounts. Only the owner can manage users.

### Test F20.1: Invite a new user

| Step | Action | Expected |
|------|--------|----------|
| 1 | Login as owner (`admin@smarthome.com`) | Login successful |
| 2 | Navigate to **Users** tab | See user list with action buttons |
| 3 | Click **Invite Member** | Dialog appears asking for email |
| 4 | Enter `testuser@example.com` and confirm | Success message: "Invitation sent" |
| 5 | Verify user appears in the table with status "Pending" | New row: testuser@example.com, Pending, Member |

### Test F20.2: Invite validation — invalid email

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Invite Member** | Dialog appears |
| 2 | Enter `notanemail` (no @) and confirm | Error message about invalid email format |
| 3 | Try empty email | Button disabled / error message |
| 4 | Try email with only whitespace | Error message |

### Test F20.3: Revoke user access

| Step | Action | Expected |
|------|--------|----------|
| 1 | In the Users table, find a user with status "Active" | |
| 2 | Click **Revoke Access** on that row | User status changes to "Revoked" |
| 3 | Try to login as that revoked user | Login is rejected |

### Test F20.4: Restore revoked user

| Step | Action | Expected |
|------|--------|----------|
| 1 | Find a revoked user in the table | Status shows "Revoked" |
| 2 | Click **Restore Access** | User status changes back to "Active" |
| 3 | Login as that restored user | Login succeeds |

### Test F20.5: Cannot revoke the owner

| Step | Action | Expected |
|------|--------|----------|
| 1 | Find the owner account in the Users table | Owner role shown |
| 2 | Click **Revoke Access** on the owner | Warning: "Owner accounts cannot be revoked" |
| 3 | Owner can still login | Login works fine |

### Test F20.6: Non-owner cannot manage users

| Step | Action | Expected |
|------|--------|----------|
| 1 | Login as a Member (not owner) | Login successful |
| 2 | Navigate to **Users** tab | Invite button is greyed out / disabled |
| 3 | Verify no Revoke/Restore buttons visible | Actions are hidden for non-owners |

### Test F20.7: Invited user can login (after registration)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Owner invites `newmember@test.com` | User appears with status "Pending" |
| 2 | Logout and go to Register | |
| 3 | Register with email `newmember@test.com` and a password | Registration succeeds |
| 4 | Login with the new credentials | Login succeeds, role is "Member" |

---

# FR10 — Rule Engine

**What it does:** Create condition-action rules that automate smart home device control.

### Test F10.1: Create a Time-based rule

| Step | Action | Expected |
|------|--------|----------|
| 1 | Navigate to **Rules** tab | See list of existing rules |
| 2 | Click **Add Rule** | Rule dialog opens |
| 3 | Enter name: `Test Time Rule` | |
| 4 | Select trigger: `Time` | Source device combo becomes disabled, shows "No source device required" |
| 5 | Enter condition: `22:00` | |
| 6 | Select target device: `Main Light` | Action combo populates with "Turn On", "Turn Off" |
| 7 | Select action: `Turn On` | |
| 8 | Click **Save** | Rule appears in table with status "Active" |

### Test F10.2: Create a Sensor Threshold rule

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Add Rule** | Rule dialog opens |
| 2 | Enter name: `Temp Alert` | |
| 3 | Select trigger: `Sensor Threshold` | Source device combo enables |
| 4 | Select source device: `Temperature Sensor` | |
| 5 | Enter condition: `Value > 28` | |
| 6 | Select target device: `Thermostat` | |
| 7 | Select action: `Set to 22°C` | |
| 8 | Click **Save** | Rule created successfully |

### Test F10.3: Create a Device State rule

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Add Rule** | |
| 2 | Enter name: `Motion Light` | |
| 3 | Select trigger: `Device State` | |
| 4 | Select source device: `Motion Sensor` | |
| 5 | Enter condition: `State = Active` | |
| 6 | Select target device: `Main Light` | |
| 7 | Select action: `Turn On` | |
| 8 | Click **Save** | Rule created successfully |

### Test F10.4: Rule validation — empty fields

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Add Rule** | Dialog opens |
| 2 | Leave name empty, fill rest | Save button is disabled |
| 3 | Fill name, leave condition empty | Save button is disabled |
| 4 | Fill all required, click Save | Rule saves |

### Test F10.5: Rule validation — malformed condition

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Add Rule** | |
| 2 | Trigger: `Time`, condition: `not-a-time` | Error alert on Save: "malformed time expression" |
| 3 | Change condition to `14:30` | Rule saves successfully |

### Test F10.6: Edit an existing rule

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Edit** on an existing rule | Rule dialog pre-filled with current values |
| 2 | Change the name to `Updated Rule` | |
| 3 | Change the action | |
| 4 | Click **Save** | Table reflects updated values |

### Test F10.7: Delete a rule

| Step | Action | Expected |
|------|--------|----------|
| 1 | Note how many rules are in the table | |
| 2 | Click **Delete** on a rule | Rule disappears from table |
| 3 | Row count decreases by 1 | |

### Test F10.8: Toggle rule enable/disable

| Step | Action | Expected |
|------|--------|----------|
| 1 | Find an **Active** rule | Status shows "Active" |
| 2 | Click the toggle/checkbox | Status changes to "Inactive" |
| 3 | Toggle again | Status returns to "Active" |

### Test F10.9: Execute rule manually

| Step | Action | Expected |
|------|--------|----------|
| 1 | Find a rule in the table | |
| 2 | Click the **Execute** (play) button | Rule executes immediately |
| 3 | Check Activity Log | Log entry appears for the rule execution |

### Test F10.10: Rule persists after app restart

| Step | Action | Expected |
|------|--------|----------|
| 1 | Create a rule | Rule visible in table |
| 2 | Close and restart the app | |
| 3 | Login and navigate to Rules | The rule is still there |

---

# FR11 — Rule Conflict Detection

**What it does:** Prevents saving rules that conflict with existing enabled rules (same target device, same trigger/condition, incompatible action).

### Test F11.1: Conflict on create — same target/trigger/condition, incompatible action

| Step | Action | Expected |
|------|--------|----------|
| 1 | Ensure Rule A exists: name `Light On`, trigger `Time`, condition `22:00`, target `Main Light`, action `Turn On` | |
| 2 | Click **Add Rule** | |
| 3 | Create Rule B: name `Light Off`, trigger `Time`, condition `22:00`, target `Main Light`, action `Turn Off` | |
| 4 | Click **Save** | **Conflict modal appears** showing Rule A as conflicting |
| 5 | Click **Cancel** on conflict modal | Rule B is **NOT saved** — does not appear in table |

### Test F11.2: No conflict — different condition

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Add Rule** | |
| 2 | Name: `Morning Light`, trigger: `Time`, condition: `08:00`, target: `Main Light`, action: `Turn Off` | |
| 3 | Click **Save** | Rule saved successfully — no conflict (different trigger time) |

### Test F11.3: No conflict — different target device

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Add Rule** | |
| 2 | Name: `Bedroom Light`, trigger: `Time`, condition: `22:00`, target: `Bed Light` (different device!), action: `Turn Off` | |
| 3 | Click **Save** | Rule saved successfully — no conflict (different target) |

### Test F11.4: Conflict on edit — revert on cancel

| Step | Action | Expected |
|------|--------|----------|
| 1 | Edit the `Morning Light` rule (condition `08:00`) | Dialog pre-filled |
| 2 | Change condition to `22:00`, action to `Turn Off` | Now conflicts with `Light On` rule |
| 3 | Click **Save** | **Conflict modal appears** |
| 4 | Click **Cancel** | Rule is **reverted** to original values (`08:00`, action unchanged) |

### Test F11.5: No conflict — same action is OK

| Step | Action | Expected |
|------|--------|----------|
| 1 | Two rules both say `Turn On` for `Main Light` at `22:00` | |
| 2 | Click **Save** | Rule saved — same action is not a conflict |

### Test F11.6: Disabled rules don't trigger conflicts

| Step | Action | Expected |
|------|--------|----------|
| 1 | Disable the `Light On` rule (toggle to Inactive) | |
| 2 | Create new rule: `Light Off`, `Time`, `22:00`, `Main Light`, `Turn Off` | |
| 3 | Click **Save** | Rule saved — disabled rules are ignored in conflict check |

---

# FR12 — In-App Notifications

**What it does:** Tracks rule execution events, errors, and system alerts via a notification bell.

### Test F12.1: No spam notifications at startup

| Step | Action | Expected |
|------|--------|----------|
| 1 | Login to the app | Bell badge shows 0 or only pre-existing unread count |
| 2 | Check notification list immediately | No new "Rule execution failed: condition not met" entries |
| 3 | Wait 30-60 seconds | No flood of new notifications |

### Test F12.2: Notification on rule execution

| Step | Action | Expected |
|------|--------|----------|
| 1 | Manually execute a rule | Check notification bell — count increases |
| 2 | Open notification panel | See execution notification with rule name |

### Test F12.3: Mark notification as read

| Step | Action | Expected |
|------|--------|----------|
| 1 | Have at least one unread notification | Bell badge shows count |
| 2 | Open notification panel, click a notification | That notification marked as read |
| 3 | Close and reopen notification panel | Read notification is visually distinct (greyed out) |

### Test F12.4: Mark all as read

| Step | Action | Expected |
|------|--------|----------|
| 1 | Have multiple unread notifications | Bell badge > 1 |
| 2 | Click **Mark All Read** | Bell badge goes to 0 |
| 3 | Check notification panel | All notifications show as read |

### Test F12.5: Genuine failures still notify

| Step | Action | Expected |
|------|--------|----------|
| 1 | Create a rule with a target device that doesn't exist (if possible) | |
| 2 | Execute the rule | FAILURE notification appears: "target device not found" or similar |
| 3 | Verify notification type | Bell shows it as a failure/error notification |

### Test F12.6: Notifications persist after restart

| Step | Action | Expected |
|------|--------|----------|
| 1 | Generate some notifications (execute rules) | |
| 2 | Restart the app and login | |
| 3 | Open notification panel | Previous notifications are still visible |

---

# FR17 — Persistent Scene Management

**What it does:** Create and activate saved multi-device scenes ("Movie Night", "Away", "Morning").

### Test F17.1: View scenes list

| Step | Action | Expected |
|------|--------|----------|
| 1 | Navigate to **Scenes** tab | See list of scenes (demo scenes: Movie Night, Away, Morning) |
| 2 | Verify each scene shows name, description, and expandable device states | |

### Test F17.2: Create a new scene

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Add Scene** | Scene creation dialog opens |
| 2 | Enter name: `Reading Time` | |
| 3 | Enter description: `Dim lights for reading` | |
| 4 | Add device states (e.g., `Main Light: OFF`, `Dimmer Light: 30%`) | |
| 5 | Click **Save** | Scene appears in the list |

### Test F17.3: Activate a scene

| Step | Action | Expected |
|------|--------|----------|
| 1 | Select a scene from the list | |
| 2 | Click **Activate** | All devices change to the scene's configured states |
| 3 | Check Activity Log | Log entry appears for scene activation |
| 4 | Check notification bell | Notification about scene activation |

### Test F17.4: Activate "Movie Night" scene

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Activate** on "Movie Night" scene | Main Light turns Off, Dimmer Light set to 25%, TV turns On (as defined in scene) |
| 2 | Navigate to Rooms view | Verify device states changed accordingly |

### Test F17.5: Activate "Away" scene

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Activate** on "Away" scene | All lights Off, all devices Off/Safe state |
| 2 | Navigate to Rooms view | All controllable devices are off |

### Test F17.6: Edit a scene

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click **Edit** on "Reading Time" scene | Dialog pre-filled |
| 2 | Change description, add/remove device states | |
| 3 | Click **Save** | Scene updated in list |

### Test F17.7: Delete a scene

| Step | Action | Expected |
|------|--------|----------|
| 1 | Note number of scenes | |
| 2 | Click **Delete** on "Reading Time" scene | Scene removed from list |
| 3 | Count decreases by 1 | |

### Test F17.8: Scenes persist after restart

| Step | Action | Expected |
|------|--------|----------|
| 1 | Create a new scene "Test Persist" | |
| 2 | Restart the app and login | |
| 3 | Navigate to Scenes | "Test Persist" is still in the list |

### Test F17.9: Activate non-existent scene

| Step | Action | Expected |
|------|--------|----------|
| 1 | If possible, try to activate a deleted scene | Returns false or shows error |

---

# Cross-FR Integration Tests

Tests that verify multiple FRs work together.

### Test INT.1: Rule execution → Notification → Activity Log

| Step | Action | Expected |
|------|--------|----------|
| 1 | Manually execute a rule | Rule fires |
| 2 | Check notification bell | New notification about execution |
| 3 | Check Activity Log | New entry with rule name and action details |

### Test INT.2: Scene activation → Log + Notification

| Step | Action | Expected |
|------|--------|----------|
| 1 | Activate "Away" scene | Scene activates |
| 2 | Check notification bell | Notification about scene activation |
| 3 | Check Activity Log | Log entry for each device change |

### Test INT.3: Rule conflict + notification

| Step | Action | Expected |
|------|--------|----------|
| 1 | Create conflicting rule → cancel on modal | No notification generated (rule was not saved) |
| 2 | Check notification bell | No new notifications |

### Test INT.4: User restricted actions

| Step | Action | Expected |
|------|--------|----------|
| 1 | Login as Member (not owner) | |
| 2 | Navigate to Rules | Add Rule button disabled if user can't manage system |
| 3 | Navigate to Users | Invite button disabled for non-owner |
| 4 | Scenes, Rooms, Energy | Viewable but potentially limited actions |

---

## Quick Smoke Test (~10 min)

1. **Login** as owner → app loads without freeze
2. **FR20**: Invite a user → check appears in table
3. **FR20**: Revoke then restore the user → status changes correctly
4. **FR10**: Create a Time rule → saves successfully
5. **FR10**: Edit the rule → changes reflected
6. **FR11**: Create a conflicting rule → modal appears → cancel → rule not saved
7. **FR12**: Execute a rule → notification appears → mark as read → badge updates
8. **FR17**: Activate "Movie Night" → devices change → log + notification appear
9. **FR10**: Delete a rule → create another → no duplicate key error
10. **Restart app** → login → all created data persists