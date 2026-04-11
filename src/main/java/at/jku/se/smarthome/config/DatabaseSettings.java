package at.jku.se.smarthome.config;

/**
 * JDBC connection settings for the SmartHome auth database.
 *
 * @param jdbcUrl JDBC connection URL
 * @param username database username
 * @param password database password
 */
public record DatabaseSettings(String jdbcUrl, String username, String password) {
}