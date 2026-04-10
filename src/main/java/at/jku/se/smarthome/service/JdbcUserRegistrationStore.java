package at.jku.se.smarthome.service;

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

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;

/**
 * JDBC-backed registration store for the auth users table.
 */
class JdbcUserRegistrationStore implements UserRegistrationStore {

    private static final String DUPLICATE_EMAIL_SQL_STATE = "23505";
    private static final String INIT_SCRIPT_PATH = "/db/init-auth.sql";
    private volatile boolean schemaReady;

    @Override
    public boolean emailExists(String normalizedEmail) throws StoreException {
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM users WHERE email = ?")) {
                statement.setString(1, normalizedEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException exception) {
            throw new StoreException("Failed to check whether the e-mail already exists.", exception);
        }
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
        if (schemaReady) {
            return;
        }

        synchronized (this) {
            if (schemaReady) {
                return;
            }

            String script = loadInitScript();
            try (Statement statement = connection.createStatement()) {
                for (String sqlStatement : script.split(";")) {
                    String trimmedStatement = sqlStatement.trim();
                    if (!trimmedStatement.isEmpty()) {
                        statement.execute(trimmedStatement);
                    }
                }
                schemaReady = true;
            } catch (SQLException exception) {
                throw new StoreException("Failed to initialize the auth schema.", exception);
            }
        }
    }

    private String loadInitScript() throws StoreException {
        try (InputStream inputStream = getClass().getResourceAsStream(INIT_SCRIPT_PATH)) {
            if (inputStream == null) {
                throw new StoreException("Auth schema script was not found at " + INIT_SCRIPT_PATH + ".");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new StoreException("Failed to read the auth schema script.", exception);
        }
    }
}