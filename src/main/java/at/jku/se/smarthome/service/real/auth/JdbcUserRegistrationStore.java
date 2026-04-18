package at.jku.se.smarthome.service.real.auth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;

/**
 * JDBC-backed registration store for the auth users table.
 */
public class JdbcUserRegistrationStore implements UserRegistrationStore {

    /** SQL state for duplicate key constraint violation. */
    private static final String DUPLICATE_EMAIL_SQL_STATE = "23505";
    /** Path to user registration initialization SQL script. */
    private static final String INIT_SCRIPT_PATH = "/db/init-auth.sql";
    /** Flag indicating database schema is ready. */
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);

    @Override
    public boolean emailExists(String normalizedEmail) throws StoreException {
        boolean exists = false;
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM users WHERE email = ?")) {
                statement.setString(1, normalizedEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                    exists = resultSet.next();
                }
            }
        } catch (SQLException exception) {
            throw new StoreException("Failed to check whether the e-mail already exists.", exception);
        }
        return exists;
    }

    @Override
    public Optional<PersistedUser> findByEmail(String normalizedEmail) throws StoreException {
        Optional<PersistedUser> result = Optional.empty();
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT email, username, password_hash, role, status FROM users WHERE email = ?")) {
                statement.setString(1, normalizedEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        result = Optional.of(new PersistedUser(
                                resultSet.getString("email"),
                                resultSet.getString("username"),
                                resultSet.getString("password_hash"),
                                resultSet.getString("role"),
                                resultSet.getString("status")
                        ));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new StoreException("Failed to load the user for authentication.", exception);
        }
        return result;
    }

    @Override
    public void save(PersistedUser user) throws StoreException {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (email, username, password_hash, role, status) VALUES (?, ?, ?, ?, ?)")) {
                statement.setString(1, user.email());
                statement.setString(2, user.username());
                statement.setString(3, user.passwordHash());
                statement.setString(4, user.role());
                statement.setString(5, user.status());
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            if (DUPLICATE_EMAIL_SQL_STATE.equals(exception.getSQLState())) {
                throw new DuplicateEmailException("A user with this e-mail already exists.", exception);
            }
            throw new StoreException("Failed to persist the registered user.", exception);
        }
    }

    @Override
    public java.util.List<PersistedUser> findAllUsers() throws StoreException {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT email, username, password_hash, role, status FROM users")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    java.util.List<PersistedUser> users = new java.util.ArrayList<>();
                    while (resultSet.next()) {
                        users.add(new PersistedUser(
                                resultSet.getString("email"),
                                resultSet.getString("username"),
                                resultSet.getString("password_hash"),
                                resultSet.getString("role"),
                                resultSet.getString("status")
                        ));
                    }
                    return users;
                }
            }
        } catch (SQLException exception) {
            throw new StoreException("Failed to load persisted user records.", exception);
        }
    }

    @Override
    public void updateLastLogin(String normalizedEmail) throws StoreException {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET last_login_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE email = ?")) {
                statement.setString(1, normalizedEmail);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new StoreException("Failed to update the last-login timestamp.", exception);
        }
    }

    private Connection openConnection() throws StoreException, SQLException {
        DatabaseSettings settings = loadSettings();
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }

    private DatabaseSettings loadSettings() throws StoreConfigurationException {
        try {
            Optional<DatabaseSettings> settings = DatabaseConfig.load();
            if (settings.isEmpty()) {
                throw new StoreConfigurationException("Registration database is not configured.");
            }
            return settings.get();
        } catch (IllegalStateException exception) {
            throw new StoreConfigurationException(exception.getMessage(), exception);
        }
    }

    private void ensureSchema(Connection connection) throws StoreException {
        boolean needsSchema = !schemaReady.get();
        if (needsSchema) {
            synchronized (this) {
                if (!schemaReady.get()) {
                    String script = loadInitScript();
                    try (Statement statement = connection.createStatement()) {
                        for (String sqlStatement : script.split(";")) {
                            String trimmedStatement = sqlStatement.trim();
                            if (!trimmedStatement.isEmpty()) {
                                statement.execute(trimmedStatement);
                            }
                        }
                        schemaReady.set(true);
                    } catch (SQLException exception) {
                        throw new StoreException("Failed to initialize the auth schema.", exception);
                    }
                }
            }
        }
    }

    private String loadInitScript() throws StoreException {
        String result = null;
        try (InputStream inputStream = getClass().getResourceAsStream(INIT_SCRIPT_PATH)) {
            if (inputStream == null) {
                throw new StoreException("Auth schema script was not found at " + INIT_SCRIPT_PATH + ".");
            }
            result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new StoreException("Failed to read the auth schema script.", exception);
        }
        return result;
    }
}