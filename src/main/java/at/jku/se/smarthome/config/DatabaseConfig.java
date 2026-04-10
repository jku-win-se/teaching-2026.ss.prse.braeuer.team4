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

    private static final Path DOT_ENV_PATH = Path.of(".env");
    private static final Path LOCAL_CONFIG_PATH = Path.of("smarthome-db.properties");
    private static final String ENV_URL = "SMARTHOME_DB_URL";
    private static final String ENV_USER = "SMARTHOME_DB_USER";
    private static final String ENV_PASSWORD = "SMARTHOME_DB_PASSWORD";
    private static final String ENV_DATABASE_URL = "DATABASE_URL";
    private static final String ENV_SMARTHOME_DATABASE_URL = "SMARTHOME_DATABASE_URL";
    private static final String PROPERTY_URL = "smarthome.db.url";
    private static final String PROPERTY_USER = "smarthome.db.user";
    private static final String PROPERTY_PASSWORD = "smarthome.db.password";

    private DatabaseConfig() {
    }

    public static Optional<DatabaseSettings> load() {
        DatabaseSettings dotEnvSettings = readFromDotEnv();
        if (dotEnvSettings != null) {
            return Optional.of(dotEnvSettings);
        }

        DatabaseSettings envSettings = readFromEnvironment();
        if (envSettings != null) {
            return Optional.of(envSettings);
        }

        if (!Files.exists(LOCAL_CONFIG_PATH)) {
            return Optional.empty();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(LOCAL_CONFIG_PATH)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read smarthome-db.properties", exception);
        }

        return Optional.of(readSettings(properties.getProperty(PROPERTY_URL),
                properties.getProperty(PROPERTY_USER),
                properties.getProperty(PROPERTY_PASSWORD)));
    }

    private static DatabaseSettings readFromDotEnv() {
        if (!Files.exists(DOT_ENV_PATH)) {
            return null;
        }

        try {
            Map<String, String> values = parseDotEnv(Files.readAllLines(DOT_ENV_PATH));
            return readSettings(
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

    private static DatabaseSettings readFromEnvironment() {
        DatabaseSettings settings = readSettings(
                System.getenv(ENV_URL),
                System.getenv(ENV_USER),
                System.getenv(ENV_PASSWORD),
                System.getenv(ENV_SMARTHOME_DATABASE_URL),
                System.getenv(ENV_DATABASE_URL)
        );

        if (settings == null) {
            return null;
        }

        return settings;
    }

    private static DatabaseSettings readSettings(String url, String user, String password) {
        return readSettings(url, user, password, null, null);
    }

    private static DatabaseSettings readSettings(String url, String user, String password, String smartHomeDatabaseUrl, String databaseUrl) {
        String normalizedUrl = normalize(url);
        String normalizedUser = normalize(user);
        String normalizedPassword = normalize(password);

        if (normalizedUrl != null || normalizedUser != null || normalizedPassword != null) {
            if (normalizedUrl == null || normalizedUser == null || normalizedPassword == null) {
                throw new IllegalStateException("Database configuration is incomplete. Provide URL, user, and password together.");
            }
            return new DatabaseSettings(normalizedUrl, normalizedUser, normalizedPassword);
        }

        String normalizedConnectionString = normalize(smartHomeDatabaseUrl);
        if (normalizedConnectionString == null) {
            normalizedConnectionString = normalize(databaseUrl);
        }

        if (normalizedConnectionString == null) {
            return null;
        }

        return parseConnectionString(normalizedConnectionString);
    }

    private static DatabaseSettings parseConnectionString(String connectionString) {
        try {
            URI uri = new URI(connectionString);
            String scheme = normalize(uri.getScheme());
            if (scheme == null || !(scheme.equals("postgres") || scheme.equals("postgresql"))) {
                throw new IllegalStateException("Unsupported database URL scheme in .env. Use postgres:// or postgresql://.");
            }

            String userInfo = uri.getUserInfo();
            if (isBlank(userInfo) || !userInfo.contains(":")) {
                throw new IllegalStateException("Database URL must contain username and password.");
            }

            String[] userInfoParts = userInfo.split(":", 2);
            String username = normalize(userInfoParts[0]);
            String password = normalize(userInfoParts[1]);
            String host = normalize(uri.getHost());
            int port = uri.getPort();
            String databaseName = normalize(uri.getPath() == null ? null : uri.getPath().replaceFirst("^/", ""));

            if (username == null || password == null || host == null || port < 0 || databaseName == null) {
                throw new IllegalStateException("Database URL is missing required connection parts.");
            }

            String query = normalize(uri.getQuery());
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
            if (query != null) {
                jdbcUrl += "?" + query;
            }

            return new DatabaseSettings(jdbcUrl, username, password);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Database URL in .env is not a valid URI.", exception);
        }
    }

    private static Map<String, String> parseDotEnv(List<String> lines) {
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
        if (value.length() >= 2) {
            boolean hasDoubleQuotes = value.startsWith("\"") && value.endsWith("\"");
            boolean hasSingleQuotes = value.startsWith("'") && value.endsWith("'");
            if (hasDoubleQuotes || hasSingleQuotes) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}