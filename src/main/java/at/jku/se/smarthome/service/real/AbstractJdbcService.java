package at.jku.se.smarthome.service.real;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;

/**
 * Base class for JDBC-backed services providing shared database connection,
 * schema initialization, and init-script loading.
 */
@SuppressWarnings("PMD.DoNotUseThreads")
public abstract class AbstractJdbcService {

    /** Flag indicating database schema is ready. */
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);
    /** Display name used in error messages. */
    private final String serviceName;
    /** Classpath path to the SQL init script. */
    private final String initScriptPath;

    /**
     * Constructs the base service.
     *
     * @param serviceName display name for error messages
     * @param initScriptPath classpath path to the SQL init script
     */
    protected AbstractJdbcService(String serviceName, String initScriptPath) {
        this.serviceName = serviceName;
        this.initScriptPath = initScriptPath;
    }

    /**
     * Opens a database connection using configured settings.
     *
     * @return open database connection
     * @throws SQLException if connection fails
     */
    protected Connection openConnection() throws SQLException {
        DatabaseSettings settings = DatabaseConfig.load()
                .orElseThrow(() -> new IllegalStateException(serviceName + " database is not configured."));
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }

    /**
     * Ensures the database schema is initialized (double-checked locking).
     *
     * @param connection database connection to use
     */
    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    protected void ensureSchema(Connection connection) {
        if (!schemaReady.get()) {
            synchronized (this) {
                if (!schemaReady.get()) {
                    try (Statement stmt = connection.createStatement()) {
                        for (String sql : loadInitScript().split(";")) {
                            String trimmed = sql.trim();
                            if (!trimmed.isEmpty()) {
                                stmt.execute(trimmed);
                            }
                        }
                        schemaReady.set(true);
                    } catch (SQLException exception) {
                        throw new IllegalStateException(
                                "Failed to initialize " + serviceName + " schema.", exception);
                    }
                }
            }
        }
    }

    /**
     * Resets the schema-ready flag for testing.
     */
    protected void resetSchemaForTesting() {
        schemaReady.set(false);
    }

    private String loadInitScript() {
        try (InputStream inputStream = getClass().getResourceAsStream(initScriptPath)) {
            if (inputStream == null) {
                throw new IllegalStateException(
                        serviceName + " schema script not found at " + initScriptPath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read " + serviceName + " schema script.", exception);
        }
    }
}
