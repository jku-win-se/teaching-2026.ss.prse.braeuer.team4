package at.jku.se.smarthome.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Loads optional database settings from environment variables or a local properties file.
 */
public final class DatabaseConfig {

    /** Path to .env file for environment variables. */
    private static final Path DOT_ENV_PATH = Path.of(".env");
    /** Path to local database configuration properties file. */
    private static final Path LOCAL_CONFIG_PATH = Path.of("smarthome-db.properties");
    /** Environment variable for SmartHome database URL. */
    private static final String ENV_URL = "SMARTHOME_DB_URL";
    /** Environment variable for database user. */
    private static final String ENV_USER = "SMARTHOME_DB_USER";
    /** Environment variable for database password. */
    private static final String ENV_PASSWORD = "SMARTHOME_DB_PASSWORD";
    /** Environment variable for generic database URL. */
    private static final String ENV_DATABASE_URL = "DATABASE_URL";
    /** Environment variable for SmartHome database URL (alternative). */
    private static final String ENV_SMARTHOME_DATABASE_URL = "SMARTHOME_DATABASE_URL";
    /** Properties key for database URL. */
    private static final String PROPERTY_URL = "smarthome.db.url";
    /** Properties key for database user. */
    private static final String PROPERTY_USER = "smarthome.db.user";
    /** Properties key for database password. */
    private static final String PROPERTY_PASSWORD = "smarthome.db.password";
    /** Minimum length for a value wrapped in matching quote characters. */
    private static final int MIN_QUOTED_VALUE_LENGTH = 2;

    /** Private constructor prevents instantiation. */
    private DatabaseConfig() {
    }

    /**
     * Loads database settings from various sources.
     *
     * @return optional containing database settings if found
     * @throws IllegalStateException if unable to read the database properties file
     */
    public static Optional<DatabaseSettings> load() {
        Optional<DatabaseSettings> result = Optional.empty();
        
        DatabaseSettings settings = readFromSystemProperties();
        if (settings != null) {
            result = Optional.of(settings);
        } else {
            settings = readFromDotEnv();
            if (settings != null) {
                result = Optional.of(settings);
            } else {
                settings = readFromEnvironment();
                if (settings != null) {
                    result = Optional.of(settings);
                } else if (Files.exists(LOCAL_CONFIG_PATH)) {
                    Properties properties = new Properties();
                    try (InputStream inputStream = Files.newInputStream(LOCAL_CONFIG_PATH)) {
                        properties.load(inputStream);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Unable to read smarthome-db.properties", exception);
                    }

                    settings = readSettings(properties.getProperty(PROPERTY_URL),
                            properties.getProperty(PROPERTY_USER),
                            properties.getProperty(PROPERTY_PASSWORD));
                    if (settings != null) {
                        result = Optional.of(settings);
                    }
                }
            }
        }
        return result;
    }

    private static DatabaseSettings readFromSystemProperties() {
        return readSettings(
                System.getProperty(PROPERTY_URL),
                System.getProperty(PROPERTY_USER),
                System.getProperty(PROPERTY_PASSWORD),
                System.getProperty(ENV_SMARTHOME_DATABASE_URL),
                System.getProperty(ENV_DATABASE_URL)
        );
    }

    private static DatabaseSettings readFromDotEnv() {
        DatabaseSettings result = null;
        if (Files.exists(DOT_ENV_PATH)) {
            try {
                Map<String, String> values = parseDotEnv(Files.readAllLines(DOT_ENV_PATH));
                result = readSettings(
                        values.get(ENV_URL),
                        values.get(ENV_USER),
                        values.get(ENV_PASSWORD),
                        values.get(ENV_SMARTHOME_DATABASE_URL),
                        values.get(ENV_DATABASE_URL)
                );
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to read .env", exception);
            }
        }
        return result;
    }

    private static DatabaseSettings readFromEnvironment() {
        DatabaseSettings settings = readSettings(
                System.getenv(ENV_URL),
                System.getenv(ENV_USER),
                System.getenv(ENV_PASSWORD),
                System.getenv(ENV_SMARTHOME_DATABASE_URL),
                System.getenv(ENV_DATABASE_URL)
        );
        return settings;
    }

    private static DatabaseSettings readSettings(String url, String user, String password) {
        return readSettings(url, user, password, null, null);
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private static DatabaseSettings readSettings(String url, String user, String password, String smartHomeDatabaseUrl, String databaseUrl) {
        DatabaseSettings result = null;
        String normalizedUrl = normalize(url);
        String normalizedUser = normalize(user);
        String normalizedPassword = password == null ? null : password.trim();

        if (normalizedUrl != null || normalizedUser != null || normalizedPassword != null) {
            if (normalizedUrl == null || normalizedUser == null || normalizedPassword == null) {
                throw new IllegalStateException("Database configuration is incomplete. Provide URL, user, and password together.");
            }
            result = new DatabaseSettings(normalizedUrl, normalizedUser, normalizedPassword);
        } else {
            String normalizedConnectionString = normalize(smartHomeDatabaseUrl);
            if (normalizedConnectionString == null) {
                normalizedConnectionString = normalize(databaseUrl);
            }
            if (normalizedConnectionString != null) {
                result = parseConnectionString(normalizedConnectionString);
            }
        }
        return result;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private static DatabaseSettings parseConnectionString(String connectionString) {
        DatabaseSettings result = null;
        try {
            URI uri = new URI(connectionString);
            String scheme = normalize(uri.getScheme());
            if (scheme != null && (scheme.equals("postgres") || scheme.equals("postgresql"))) {
                String userInfo = uri.getUserInfo();
                if (!isBlank(userInfo) && userInfo.contains(":")) {
                    String[] userInfoParts = userInfo.split(":", 2);
                    String username = normalize(userInfoParts[0]);
                    String password = normalize(userInfoParts[1]);
                    String host = normalize(uri.getHost());
                    int port = uri.getPort();
                    String databaseName = normalize(uri.getPath() == null ? null : uri.getPath().replaceFirst("^/", ""));

                    if (username != null && password != null && host != null && port >= 0 && databaseName != null) {
                        String query = normalize(uri.getQuery());
                        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
                        if (query != null) {
                            jdbcUrl += "?" + query;
                        }
                        result = new DatabaseSettings(jdbcUrl, username, password);
                    } else {
                        throw new IllegalStateException("Database URL is missing required connection parts.");
                    }
                } else {
                    throw new IllegalStateException("Database URL must contain username and password.");
                }
            } else {
                throw new IllegalStateException("Unsupported database URL scheme in .env. Use postgres:// or postgresql://.");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Database URL in .env is not a valid URI.", exception);
        }
        return result;
    }

    private static Map<String, String> parseDotEnv(List<String> lines) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            int separatorIndex = trimmedLine.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }

            String key = trimmedLine.substring(0, separatorIndex).trim();
            String value = trimmedLine.substring(separatorIndex + 1).trim();
            values.put(key, stripQuotes(value));
        }
        return values;
    }

    private static String stripQuotes(String value) {
        String result = value;
        if (value.length() >= MIN_QUOTED_VALUE_LENGTH) {
            boolean hasDoubleQuotes = value.startsWith("\"") && value.endsWith("\"");
            boolean hasSingleQuotes = value.startsWith("'") && value.endsWith("'");
            if (hasDoubleQuotes || hasSingleQuotes) {
                result = value.substring(1, value.length() - 1);
            }
        }
        return result;
    }

    private static String normalize(String value) {
        String normalized = null;
        if (value != null) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                normalized = trimmed;
            }
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}