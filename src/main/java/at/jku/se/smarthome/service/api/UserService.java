package at.jku.se.smarthome.service.api;

import at.jku.se.smarthome.model.User;
import javafx.collections.ObservableList;

public abstract class UserService {

    public enum RegistrationStatus {
        SUCCESS,
        INVALID_INPUT,
        PASSWORD_MISMATCH,
        DUPLICATE_EMAIL,
        DATABASE_NOT_CONFIGURED,
        DATABASE_ERROR
    }

    public enum LoginStatus {
        SUCCESS,
        INVALID_INPUT,
        AUTHENTICATION_FAILED,
        ACCOUNT_INACTIVE,
        THROTTLED,
        DATABASE_NOT_CONFIGURED,
        DATABASE_ERROR
    }

    public boolean register(String email, String username, String password, String confirmPassword) {
        return registerUser(email, username, password, confirmPassword) == RegistrationStatus.SUCCESS;
    }

    public abstract RegistrationStatus registerUser(String email, String username, String password, String confirmPassword);

    public boolean login(String email, String password) {
        return authenticate(email, password) == LoginStatus.SUCCESS;
    }

    public abstract LoginStatus authenticate(String email, String password);

    public abstract String getCurrentUserEmail();

    public String getCurrentUserRole() {
        String role = getCurrentUserRoleInternal();
        return role != null ? role : "Guest";
    }

    protected abstract String getCurrentUserRoleInternal();

    public boolean isOwner() {
        return "Owner".equalsIgnoreCase(getCurrentUserRole());
    }

    public boolean canManageSystem() {
        return isOwner();
    }

    public abstract User getCurrentUser();

    public abstract ObservableList<User> getUsers();

    public abstract boolean hasActiveSession();

    public abstract long getRemainingThrottleSeconds(String email);

    public abstract boolean inviteUser(String email, String role);

    public abstract boolean revokeUser(String email);

    public abstract boolean restoreUser(String email);

    public abstract void logout();
}
