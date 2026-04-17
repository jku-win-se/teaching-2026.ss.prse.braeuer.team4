package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.User;
import javafx.collections.ObservableList;

public abstract class UserService {

    /** Registration status outcomes. */
    public enum RegistrationStatus {
        /** Registration succeeded. */
        SUCCESS,
        /** Invalid input provided. */
        INVALID_INPUT,
        /** Passwords do not match. */
        PASSWORD_MISMATCH,
        /** Email already registered. */
        DUPLICATE_EMAIL,
        /** Database not configured. */
        DATABASE_NOT_CONFIGURED,
        /** Database error occurred. */
        DATABASE_ERROR
    }

    /** Login/authentication status outcomes. */
    public enum LoginStatus {
        /** Authentication succeeded. */
        SUCCESS,
        /** Invalid input provided. */
        INVALID_INPUT,
        /** Authentication failed. */
        AUTHENTICATION_FAILED,
        /** Account is inactive. */
        ACCOUNT_INACTIVE,
        /** Login throttled due to too many attempts. */
        THROTTLED,
        /** Database not configured. */
        DATABASE_NOT_CONFIGURED,
        /** Database error occurred. */
        DATABASE_ERROR
    }

    /**
     * Registers a new user.
     *
     * @param email email address
     * @param username username
     * @param password password
     * @param confirmPassword password confirmation
     * @return true if registration succeeded
     */
    public boolean register(String email, String username, String password, String confirmPassword) {
        return registerUser(email, username, password, confirmPassword) == RegistrationStatus.SUCCESS;
    }

    /**
     * Registers a new user with detailed status.
     *
     * @param email email address
     * @param username username
     * @param password password
     * @param confirmPassword password confirmation
     * @return registration status
     */
    public abstract RegistrationStatus registerUser(String email, String username, String password, String confirmPassword);

    /**
     * Logs in a user.
     *
     * @param email email address
     * @param password password
     * @return true if authentication succeeded
     */
    public boolean login(String email, String password) {
        return authenticate(email, password) == LoginStatus.SUCCESS;
    }

    /**
     * Authenticates a user with detailed status.
     *
     * @param email email address
     * @param password password
     * @return authentication status
     */
    public abstract LoginStatus authenticate(String email, String password);

    /**
     * Gets the current user's email.
     *
     * @return current user email or null
     */
    public abstract String getCurrentUserEmail();

    /**
     * Gets the current user's role.
     *
     * @return current user role or "Guest"
     */
    public String getCurrentUserRole() {
        String role = getCurrentUserRoleInternal();
        return role != null ? role : "Guest";
    }

    /**
     * Gets the current user's role (internal).
     *
     * @return current user role or null
     */
    protected abstract String getCurrentUserRoleInternal();

    /**
     * Checks if the current user is an owner.
     *
     * @return true if owner
     */
    public boolean isOwner() {
        return "Owner".equalsIgnoreCase(getCurrentUserRole());
    }

    /**
     * Checks if the current user can manage the system.
     *
     * @return true if can manage
     */
    public boolean canManageSystem() {
        return isOwner();
    }

    /**
     * Gets the current user object.
     *
     * @return current user or null
     */
    public abstract User getCurrentUser();

    /**
     * Gets all users.
     *
     * @return observable list of users
     */
    public abstract ObservableList<User> getUsers();

    /**
     * Checks if an active session exists.
     *
     * @return true if session active
     */
    public abstract boolean hasActiveSession();

    /**
     * Gets remaining login throttle time.
     *
     * @param email email address
     * @return remaining throttle seconds
     */
    public abstract long getRemainingThrottleSeconds(String email);

    /**
     * Invites a user.
     *
     * @param email email address
     * @param role user role
     * @return true if invitation sent
     */
    public abstract boolean inviteUser(String email, String role);

    /**
     * Revokes user access.
     *
     * @param email email address
     * @return true if revoked
     */
    public abstract boolean revokeUser(String email);

    /**
     * Restores user access.
     *
     * @param email email address
     * @return true if restored
     */
    public abstract boolean restoreUser(String email);

    /**
     * Logs out the current user.
     */
    public abstract void logout();
}
