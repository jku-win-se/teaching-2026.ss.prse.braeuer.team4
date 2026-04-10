package at.jku.se.smarthome.service.real.auth;

/**
 * Store abstraction for persisted user registration.
 */
public interface UserRegistrationStore {

    boolean emailExists(String normalizedEmail) throws StoreException;

    java.util.Optional<PersistedUser> findByEmail(String normalizedEmail) throws StoreException;

    void save(PersistedUser user) throws StoreException;

    void updateLastLogin(String normalizedEmail) throws StoreException;

    record PersistedUser(String email, String username, String passwordHash, String role, String status) {
    }

    class StoreException extends Exception {
        public StoreException(String message, Throwable cause) {
            super(message, cause);
        }

        public StoreException(String message) {
            super(message);
        }
    }

    class StoreConfigurationException extends StoreException {
        public StoreConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }

        public StoreConfigurationException(String message) {
            super(message);
        }
    }

    class DuplicateEmailException extends StoreException {
        public DuplicateEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}