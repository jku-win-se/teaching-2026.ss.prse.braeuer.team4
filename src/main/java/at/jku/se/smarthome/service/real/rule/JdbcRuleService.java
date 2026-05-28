package at.jku.se.smarthome.service.real.rule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import at.jku.se.smarthome.config.DatabaseConfig;
import at.jku.se.smarthome.config.DatabaseSettings;
import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.NotificationType;
import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.service.api.LogService;
import at.jku.se.smarthome.service.api.NotificationService;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.RuleService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.rule.RuleEvaluator;
import at.jku.se.smarthome.service.rule.RuleValidator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * JDBC-backed RuleService implementation. Persists automation rules (FR-10/FR-11)
 * to the configured database, mirroring the pattern used by JdbcLogService and
 * JdbcScheduleService.
 *
 * <p>An in-memory {@link ObservableList} mirror is kept in sync with every
 * write so that JavaFX-bound UI controls update live without having to re-query
 * the database after each mutation.
 */
@SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.TooManyMethods",
        "PMD.CouplingBetweenObjects", "PMD.ExcessiveImports",
        "PMD.GodClass", "PMD.AvoidDeeplyNestedIfStmts",
        "PMD.CyclomaticComplexity", "PMD.DoNotUseThreads"})
public final class JdbcRuleService implements RuleService {

    /** Path to rule schema initialization script in classpath. */
    private static final String INIT_SCRIPT_PATH = "/db/init-rules.sql";
    /** Lock for singleton synchronization. */
    private static final Object INSTANCE_LOCK = new Object();
    /** Singleton instance. */
    private static JdbcRuleService instance;

    /** Observable mirror of the rules table — bound by the UI. */
    private final ObservableList<Rule> rules = FXCollections.observableArrayList();
    /** Flag indicating database schema is initialized. */
    private final AtomicBoolean schemaReady = new AtomicBoolean(false);
    /** Rule IDs whose condition was true on the previous evaluation cycle (edge detection). */
    private final Set<String> activeRuleIds = new HashSet<>();

    /** Scheduler for background rule evaluation. */
    private ScheduledExecutorService ruleScheduler;

    private JdbcRuleService() {
        refreshRules();
        seedDemoRules();
    }

    /**
     * Returns the singleton instance of the JDBC rule service.
     *
     * @return singleton JdbcRuleService instance
     */
    public static JdbcRuleService getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new JdbcRuleService();
            }
            return instance;
        }
    }

    /**
     * Resets the singleton for unit testing.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public static void resetForTesting() {
        synchronized (INSTANCE_LOCK) {
            if (instance != null) {
                instance.stopRecurringExecution();
            }
            instance = null;
        }
    }

    @Override
    public ObservableList<Rule> getRules() {
        return rules;
    }

    @Override
    public Rule addRule(String name, String triggerType, String sourceDevice,
                        String condition, String action, String targetDevice) {
        Rule created = null;
        RuleValidator.Result validation =
                RuleValidator.validate(triggerType, condition, sourceDevice,
                        ServiceRegistry.getRoomService());
        if (validation.valid()) {
            String ruleId = nextRuleId();
            Rule rule = new Rule(ruleId, name, triggerType, sourceDevice, condition,
                    action, targetDevice, true, "Active");
            try (Connection connection = openConnection()) {
                ensureSchema(connection);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO rules (id, name, trigger_type, source_device, condition_expr,"
                        + " action, target_device, enabled, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    bindRule(statement, rule);
                    statement.executeUpdate();
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to persist the rule.", exception);
            }
            rules.add(rule);
            created = rule;
        }
        return created;
    }

    @Override
    public boolean updateRule(String ruleId, String name, String triggerType, String sourceDevice,
                              String condition, String action, String targetDevice) {
        boolean updated = false;
        Rule rule = findRule(ruleId);
        if (rule != null) {
            RuleValidator.Result validation =
                    RuleValidator.validate(triggerType, condition, sourceDevice,
                            ServiceRegistry.getRoomService());
            if (validation.valid()) {
                try (Connection connection = openConnection()) {
                    ensureSchema(connection);
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE rules SET name = ?, trigger_type = ?, source_device = ?,"
                            + " condition_expr = ?, action = ?, target_device = ?,"
                            + " updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                        statement.setString(1, name);
                        statement.setString(2, triggerType);
                        statement.setString(3, sourceDevice);
                        statement.setString(4, condition);
                        statement.setString(5, action);
                        statement.setString(6, targetDevice);
                        statement.setString(7, ruleId);
                        updated = statement.executeUpdate() > 0;
                    }
                } catch (SQLException exception) {
                    throw new IllegalStateException("Failed to update the rule.", exception);
                }
                if (updated) {
                    rule.setName(name);
                    rule.setTriggerType(triggerType);
                    rule.setSourceDevice(sourceDevice);
                    rule.setCondition(condition);
                    rule.setAction(action);
                    rule.setTargetDevice(targetDevice);
                }
            }
        }
        return updated;
    }

    @Override
    public boolean deleteRule(String ruleId) {
        boolean deleted = false;
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM rules WHERE id = ?")) {
                statement.setString(1, ruleId);
                deleted = statement.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete the rule.", exception);
        }
        if (deleted) {
            rules.removeIf(r -> r.getId().equals(ruleId));
        }
        return deleted;
    }

    @Override
    public boolean toggleRule(String ruleId) {
        boolean toggled = false;
        Rule rule = findRule(ruleId);
        if (rule != null) {
            boolean newEnabled = !rule.isEnabled();
            String newStatus = newEnabled ? "Active" : "Inactive";
            try (Connection connection = openConnection()) {
                ensureSchema(connection);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rules SET enabled = ?, status = ?, updated_at = CURRENT_TIMESTAMP"
                        + " WHERE id = ?")) {
                    statement.setBoolean(1, newEnabled);
                    statement.setString(2, newStatus);
                    statement.setString(3, ruleId);
                    toggled = statement.executeUpdate() > 0;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to toggle the rule.", exception);
            }
            if (toggled) {
                rule.setEnabled(newEnabled);
                rule.setStatus(newStatus);
            }
        }
        return toggled;
    }

    @Override
    public boolean executeRule(String ruleId) {
        return executeRule(ruleId, false);
    }

    @Override
    public boolean executeRule(String ruleId, boolean isManual) {
        boolean executed = false;
        Rule rule = findRule(ruleId);
        NotificationService notificationService = ServiceRegistry.getNotificationService();
        if (rule == null) {
            notificationService.addNotification(
                    "Rule execution failed: rule not found", NotificationType.FAILURE);
        } else if (rule.isEnabled()) {
            executed = executeEnabledRule(rule, isManual);
        } else {
            notificationService.addNotification(
                    "Rule execution failed: " + rule.getName() + " is inactive",
                    NotificationType.FAILURE);
        }
        return executed;
    }

    @Override
    public boolean hasConflicts(String ruleId) {
        boolean conflictDetected = false;
        Rule candidate = findRule(ruleId);
        if (candidate != null && candidate.isEnabled()) {
            String target = candidate.getTargetDevice();
            String candAction = normalizeValue(candidate.getAction());
            String candTrigger = normalizeTrigger(candidate.getTriggerType());
            String candCondition = normalizeValue(candidate.getCondition());

            for (Rule existing : rules) {
                if (existing.getId().equals(ruleId)) {
                    continue;
                }
                if (!existing.isEnabled()) {
                    continue;
                }
                if (!target.equals(existing.getTargetDevice())) {
                    continue;
                }
                String existTrigger = normalizeTrigger(existing.getTriggerType());
                String existCondition = normalizeValue(existing.getCondition());

                if (!candTrigger.equals(existTrigger) || !candCondition.equals(existCondition)) {
                    continue;
                }

                String existAction = normalizeValue(existing.getAction());
                if (valuesAreIncompatible(candAction, existAction)) {
                    conflictDetected = true;
                    break;
                }
            }
        }
        return conflictDetected;
    }

    @Override
    public void startRecurringExecution() {
        synchronized (INSTANCE_LOCK) {
            if (ruleScheduler != null && !ruleScheduler.isShutdown()) {
                return;
            }
            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable, "jdbc-rule-dispatcher");
                thread.setDaemon(true);
                return thread;
            };
            ruleScheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
            ruleScheduler.scheduleAtFixedRate(
                    () -> Platform.runLater(this::processDueRules),
                    0, 30, TimeUnit.SECONDS);
        }
    }

    @Override
    @SuppressWarnings("PMD.NullAssignment")
    public void stopRecurringExecution() {
        synchronized (INSTANCE_LOCK) {
            if (ruleScheduler != null) {
                ruleScheduler.shutdownNow();
                ruleScheduler = null;
            }
        }
    }

    private void processDueRules() {
        RoomService roomService = ServiceRegistry.getRoomService();
        RuleEvaluator evaluator = new RuleEvaluator();
        for (Rule rule : rules) {
            if (!rule.isEnabled()) {
                activeRuleIds.remove(rule.getId());
                continue;
            }
            Device sourceDevice = roomService.getDeviceByName(rule.getSourceDevice());
            boolean conditionMet = evaluator.evaluate(rule, sourceDevice);
            if (conditionMet && !activeRuleIds.contains(rule.getId())) {
                activeRuleIds.add(rule.getId());
                executeEnabledRule(rule, false);
            } else if (!conditionMet) {
                activeRuleIds.remove(rule.getId());
            }
        }
    }

    private boolean executeEnabledRule(Rule rule, boolean isManual) {
        boolean executed = false;
        RoomService roomService = ServiceRegistry.getRoomService();
        NotificationService notificationService = ServiceRegistry.getNotificationService();
        LogService logService = ServiceRegistry.getLogService();
        Device sourceDevice = roomService.getDeviceByName(rule.getSourceDevice());
        boolean conditionMet = isManual || new RuleEvaluator().evaluate(rule, sourceDevice);
        if (conditionMet) {
            Device targetDevice = roomService.getDeviceByName(rule.getTargetDevice());
            if (targetDevice == null) {
                notificationService.addNotification(
                        "Rule execution failed: target device missing for " + rule.getName(),
                        NotificationType.FAILURE);
            } else if (applyAction(targetDevice, rule.getAction())) {
                logService.addLogEntry(targetDevice.getName(), targetDevice.getRoom(),
                        rule.getAction(), "Rule: " + rule.getName());
                notificationService.addNotification(
                        "Rule '" + rule.getName() + "' executed: "
                                + rule.getAction() + " → " + rule.getTargetDevice(),
                        NotificationType.SUCCESS);
                executed = true;
            } else {
                notificationService.addNotification(
                        "Rule execution failed: unsupported action in " + rule.getName(),
                        NotificationType.FAILURE);
            }
        }
        return executed;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private boolean applyAction(Device targetDevice, String action) {
        boolean result = false;
        if (action != null) {
            result = switch (action) {
                case "Turn On", "Open" -> {
                    targetDevice.setState(true);
                    yield true;
                }
                case "Turn Off", "Close" -> {
                    targetDevice.setState(false);
                    yield true;
                }
                case "Notify User", "Trigger Alert" -> true;
                default -> applyParameterizedAction(targetDevice, action);
            };
        }
        return result;
    }

    private boolean applyParameterizedAction(Device targetDevice, String action) {
        boolean applied = false;
        if (action.startsWith("Set to ") && action.endsWith("%")) {
            try {
                int brightness = Integer.parseInt(action.substring(7, action.length() - 1));
                targetDevice.setBrightness(brightness);
                applied = true;
            } catch (NumberFormatException ignored) {
                applied = false;
            }
        } else if (action.startsWith("Set to ") && action.endsWith("°C")) {
            try {
                double temperature = Double.parseDouble(action.substring(7, action.length() - 2));
                targetDevice.setTemperature(temperature);
                applied = true;
            } catch (NumberFormatException ignored) {
                applied = false;
            }
        }
        return applied;
    }

    private Rule findRule(String ruleId) {
        return rules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElse(null);
    }

    private String nextRuleId() {
        int maxNum = 0;
        for (Rule rule : rules) {
            String id = rule.getId();
            if (id != null && id.startsWith("rule-")) {
                try {
                    int num = Integer.parseInt(id.substring(5));
                    if (num > maxNum) {
                        maxNum = num;
                    }
                } catch (NumberFormatException ignored) {
                    // skip malformed IDs
                }
            }
        }
        return String.format("rule-%03d", maxNum + 1);
    }

    // --- conflict helpers (small heuristic, paralleling schedule conflict logic) ---
    private String normalizeTrigger(String trigger) {
        return trigger == null ? "" : trigger.trim().toLowerCase();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean valuesAreIncompatible(String value1, String value2) {
        boolean incompatible = false;
        String action1 = canonicalizeAction(value1);
        String action2 = canonicalizeAction(value2);
        if (!action1.equals(action2)) {
            if (isSwitchValue(action1) && isSwitchValue(action2)) {
                incompatible = true;
            } else if (isCoverValue(action1) && isCoverValue(action2)) {
                incompatible = true;
            } else if (isDimmerValue(action1) && isDimmerValue(action2)) {
                incompatible = true;
            } else if (isThermostatValue(action1) && isThermostatValue(action2)) {
                try {
                    double temp1 = extractTemperatureValue(action1);
                    double temp2 = extractTemperatureValue(action2);
                    incompatible = Math.abs(temp1 - temp2) > 0.5;
                } catch (NumberFormatException e) {
                    incompatible = false;
                }
            }
        }
        return incompatible;
    }

    private boolean isSwitchValue(String value) {
        return "ON".equals(value) || "OFF".equals(value);
    }

    private boolean isCoverValue(String value) {
        return "OPEN".equals(value) || "CLOSED".equals(value) || "CLOSE".equals(value);
    }

    private boolean isDimmerValue(String value) {
        return value.matches("\\d+%?") || value.matches("0\\.\\d+");
    }

    private boolean isThermostatValue(String value) {
        return value.matches("[0-9]+(\\\\.[0-9]+)?°?C?") || value.matches("[0-9]+(\\\\.[0-9]+)?\\s*°?C?");
    }

    private double extractTemperatureValue(String value) {
        String numeric = value.replaceAll("[^0-9.]", "");
        return Double.parseDouble(numeric);
    }

    private String canonicalizeAction(String value) {
        String result = "";
        if (value != null) {
            String upperValue = value.trim().toUpperCase();
            if (upperValue.contains("TURN ON")) {
                result = "ON";
            } else if (upperValue.contains("TURN OFF")) {
                result = "OFF";
            } else if (upperValue.contains("OPEN")) {
                result = "OPEN";
            } else if (upperValue.contains("CLOSE") || upperValue.contains("CLOSED")) {
                result = "CLOSED";
            } else if (upperValue.startsWith("SET TO ") && upperValue.endsWith("%")) {
                result = upperValue.substring(7);
            } else if (upperValue.startsWith("SET TO ") && upperValue.endsWith("°C")) {
                result = upperValue.substring(7);
            } else {
                result = upperValue;
            }
        }
        return result;
    }

    private void refreshRules() {
        List<Rule> loaded = new ArrayList<>();
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, name, trigger_type, source_device, condition_expr, action,"
                    + " target_device, enabled, status FROM rules ORDER BY created_at, id")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        loaded.add(new Rule(
                                resultSet.getString("id"),
                                resultSet.getString("name"),
                                resultSet.getString("trigger_type"),
                                resultSet.getString("source_device"),
                                resultSet.getString("condition_expr"),
                                resultSet.getString("action"),
                                resultSet.getString("target_device"),
                                resultSet.getBoolean("enabled"),
                                resultSet.getString("status")
                        ));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load rules from the database.", exception);
        }
        rules.setAll(loaded);
    }

    /**
     * Seeds demo rules when the database table is empty.
     * Mirrors MockRuleService.initializeMockRules() so first-time users see
     * the same demo rules whether they run against the mock or the real backend.
     */
    private void seedDemoRules() {
        if (!rules.isEmpty()) {
            return;
        }
        List<Rule> seed = List.of(
                new Rule("rule-001", "Morning Routine", "Time", "Clock", "06:00 AM",
                        "Turn On", "Main Light", true, "Active"),
                new Rule("rule-002", "Motion Welcome", "Device State", "Bedroom Light",
                        "State = Active", "Turn On", "Main Light", true, "Active"),
                new Rule("rule-003", "Heat Boost", "Sensor Threshold", "Motion Sensor",
                        "Value > 0", "Set to 22°C", "Temperature Control", true, "Inactive")
        );
        try (Connection connection = openConnection()) {
            ensureSchema(connection);
            for (Rule rule : seed) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO rules (id, name, trigger_type, source_device, condition_expr,"
                        + " action, target_device, enabled, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    bindRule(statement, rule);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to seed demo rules.", exception);
        }
        rules.setAll(seed);
    }

    private void bindRule(PreparedStatement statement, Rule rule) throws SQLException {
        statement.setString(1, rule.getId());
        statement.setString(2, rule.getName());
        statement.setString(3, rule.getTriggerType());
        statement.setString(4, rule.getSourceDevice());
        statement.setString(5, rule.getCondition());
        statement.setString(6, rule.getAction());
        statement.setString(7, rule.getTargetDevice());
        statement.setBoolean(8, rule.isEnabled());
        statement.setString(9, rule.getStatus());
    }

    private Connection openConnection() throws SQLException {
        DatabaseSettings settings = DatabaseConfig.load()
                .orElseThrow(() -> new IllegalStateException("Rule database is not configured."));
        return DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
    }

    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    private void ensureSchema(Connection connection) {
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
                        throw new IllegalStateException("Failed to initialize rules schema.", exception);
                    }
                }
            }
        }
    }

    private String loadInitScript() {
        try (InputStream scriptStream = getClass().getResourceAsStream(INIT_SCRIPT_PATH)) {
            if (scriptStream == null) {
                throw new IllegalStateException("Rule schema script not found at " + INIT_SCRIPT_PATH);
            }
            return new String(scriptStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read rule schema script.", exception);
        }
    }
}
