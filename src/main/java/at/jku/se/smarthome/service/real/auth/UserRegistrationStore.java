package at.jku.se.smarthome.service.real.auth;

/**
 * Store abstraction for persisted user registration.
 */
public interface UserRegistrationStore {

    /**
     * Checks if a user with the given email already exists.
     *
     * @param normalizedEmail the email to check
     * @return true if email exists, false otherwise
     * @throws StoreException if database access fails
     */
    boolean emailExists(String normalizedEmail) throws StoreException;

    /**
     * Retrieves a user by email address.
     *
     * @param normalizedEmail the email to search for
     * @return optional containing user if found
     * @throws StoreException if database access fails
     */
    java.util.Optional<PersistedUser> findByEmail(String normalizedEmail) throws StoreException;

    /**
     * Retrieves all registered users.
     *
     * @return list of all users
     * @throws StoreException if database access fails
     */
    java.util.List<PersistedUser> findAllUsers() throws StoreException;

    /**
     * Saves a new user to the store.
     *
     * @param user the user to save
     * @throws StoreException if database access fails
     */
    void save(PersistedUser user) throws StoreException;

    /**
     * Updates the last login timestamp for a user.
     *
     * @param normalizedEmail the email of the user
     * @throws StoreException if database access fails
     */
    void updateLastLogin(String normalizedEmail) throws StoreException;

    /**
     * Persisted user record with authentication and role information.
     *
     * @param email user email address
     * @param username user display name
     * @param passwordHash bcrypt hash of password
     * @param role user role
     * @param status user account status
     */
    record PersistedUser(String email, String username, String passwordHash, String role, String status) {
    }

    /**
     * Base exception for store-related errors.
     */
    class StoreException extends Exception {
        /**
         * Constructs with message and cause.
         *
         * @param message error message
         * @param cause underlying cause
         */
        public StoreException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs with message only.
         *
         * @param message error message
         */
        public StoreException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown for store configuration errors.
     */
    class StoreConfigurationException extends StoreException {
        /**
         * Constructs with message and cause.
         *
         * @param message error message
         * @param cause underlying cause
         */
        public StoreConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs with message only.
         *
         * @param message error message
         */
        public StoreConfigurationException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when attempting to register duplicate email.
     */
    class DuplicateEmailException extends StoreException {
        /**
         * Constructs with message and cause.
         *
         * @param message error message
         * @param cause underlying cause
         */
        public DuplicateEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}