package at.jku.se.smarthome.config;

/**
 * JDBC connection settings for the SmartHome auth database.
 */
public record DatabaseSettings(String jdbcUrl, String username, String password) {
}