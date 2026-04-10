package at.jku.se.smarthome.service;

/**
 * Store abstraction for persisted user registration.
 */
interface UserRegistrationStore {

    boolean emailExists(String normalizedEmail) throws StoreException;

    java.util.Optional<PersistedUser> findByEmail(String normalizedEmail) throws StoreException;

    void save(PersistedUser user) throws StoreException;

    void updateLastLogin(String normalizedEmail) throws StoreException;

    record PersistedUser(String email, String username, String passwordHash, String role, String status) {
    }

    class StoreException extends Exception {
        StoreException(String message, Throwable cause) {
            super(message, cause);
        }

        StoreException(String message) {
            super(message);
        }
    }

    class StoreConfigurationException extends StoreException {
        StoreConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }

        StoreConfigurationException(String message) {
            super(message);
        }
    }

    class DuplicateEmailException extends StoreException {
        DuplicateEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}