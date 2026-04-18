package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.LogEntry;
import at.jku.se.smarthome.service.mock.MockLogService;
import javafx.collections.ObservableList;

/**
 * Tests for {@link MockLogService} covering log entry storage, filtering, and export.
 *
 * The service seeds 5 mock entries on construction; tests measure relative to
 * that baseline unless the test specifically needs an empty-ish list.
 *
 * Actor conventions verified:
 *   "User"                 – manual action by the current user
 *   "Rule: <name>"         – triggered by an automation rule
 *   "Schedule: <name>"     – triggered by a schedule
 */
public class TestMockLogService {

    /** Log service under test. */
    private MockLogService service;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MockLogService.resetForTesting();
        service = MockLogService.getInstance();
    }

    // -----------------------------------------------------------------------
    // addLogEntry (4-arg, with room)
    // -----------------------------------------------------------------------

    /**
     * Test: a new entry must increment the log size.
     */
    @Test
    public void addLogEntryIncrementsSize() {
        int before = service.getLogs().size();
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        assertEquals(before + 1, service.getLogs().size());
    }

    /**
     * Test: a new entry must appear at index 0 (newest-first ordering).
     */
    @Test
    public void addLogEntryInsertsAtFront() {
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        assertEquals("Garden Blind", service.getLogs().get(0).getDevice());
    }

    /**
     * Test: device field must be stored without mutation.
     */
    @Test
    public void addLogEntryStoresDevice() {
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Garden Blind", entry.getDevice());
    }

    /**
     * Test: room field must be stored without mutation.
     */
    @Test
    public void addLogEntryStoresRoom() {
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Garden", entry.getRoom());
    }

    /**
     * Test: action field must be stored without mutation.
     */
    @Test
    public void addLogEntryStoresAction() {
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Opened", entry.getAction());
    }

    /**
     * Test: actor field must be stored without mutation.
     */
    @Test
    public void addLogEntryStoresActor() {
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("User", entry.getActor());
    }

    /**
     * Test: timestamp must be auto-generated and non-empty.
     */
    @Test
    public void addLogEntryTimestampIsNotBlank() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        assertFalse(service.getLogs().get(0).getTimestamp().isBlank());
    }

    // -----------------------------------------------------------------------
    // addLogEntry (3-arg legacy, without room)
    // -----------------------------------------------------------------------

    /**
     * Test: legacy overload must default room to unknown - device field.
     */
    @Test
    public void addLogEntryLegacyStoresDevice() {
        service.addLogEntry("Main Light", "Turned ON", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Main Light", entry.getDevice());
    }

    /**
     * Test: legacy overload must default room to unknown.
     */
    @Test
    public void addLogEntryLegacyDefaultsRoomToUnknown() {
        service.addLogEntry("Main Light", "Turned ON", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Unknown", entry.getRoom());
    }

    /**
     * Test: legacy overload must preserve action field.
     */
    @Test
    public void addLogEntryLegacyStoresAction() {
        service.addLogEntry("Main Light", "Turned ON", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Turned ON", entry.getAction());
    }

    /**
     * Test: legacy overload must preserve actor field.
     */
    @Test
    public void addLogEntryLegacyStoresActor() {
        service.addLogEntry("Main Light", "Turned ON", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("User", entry.getActor());
    }

    // -----------------------------------------------------------------------
    // getLogs
    // -----------------------------------------------------------------------

    /**
     * Test: after a fresh init the service always has at least the seeded entries.
     */
    @Test
    public void getLogsReturnsAtLeastSeedEntries() {
        assertTrue(service.getLogs().size() >= 5);
    }

    // -----------------------------------------------------------------------
    // getLogsByRoom
    // -----------------------------------------------------------------------

    /**
     * Test: get logs by room returns at least one entry for matching room.
     */
    @Test
    public void getLogsByRoomReturnsAtLeastOneEntry() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        service.addLogEntry("Bed Light", "Bedroom", "Turned OFF", "User");
        ObservableList<LogEntry> result = service.getLogsByRoom("Living Room");
        assertTrue(result.size() >= 1);
    }

    /**
     * Test: get logs by room returns only matching entries.
     */
    @Test
    public void getLogsByRoomReturnsOnlyMatchingEntries() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        service.addLogEntry("Bed Light", "Bedroom", "Turned OFF", "User");
        ObservableList<LogEntry> result = service.getLogsByRoom("Living Room");
        for (LogEntry e : result) {
            assertEquals("Living Room", e.getRoom());
        }
    }

    /**
     * Test: get logs by room no match returns empty.
     */
    @Test
    public void getLogsByRoomNoMatchReturnsEmpty() {
        ObservableList<LogEntry> result = service.getLogsByRoom("Nonexistent Room XYZ");
        assertEquals(0, result.size());
    }

    // -----------------------------------------------------------------------
    // getLogsByDevice
    // -----------------------------------------------------------------------

    /**
     * Test: get logs by device returns exactly matching entry count.
     */
    @Test
    public void getLogsByDeviceReturnsCorrectCount() {
        service.addLogEntry("Unique Device ABC", "Room", "Action", "User");
        ObservableList<LogEntry> result = service.getLogsByDevice("Unique Device ABC");
        assertEquals(1, result.size());
    }

    /**
     * Test: get logs by device returns only matching entries.
     */
    @Test
    public void getLogsByDeviceReturnsOnlyMatchingEntries() {
        service.addLogEntry("Unique Device ABC", "Room", "Action", "User");
        ObservableList<LogEntry> result = service.getLogsByDevice("Unique Device ABC");
        assertEquals("Unique Device ABC", result.get(0).getDevice());
    }

    /**
     * Test: get logs by device no match returns empty.
     */
    @Test
    public void getLogsByDeviceNoMatchReturnsEmpty() {
        ObservableList<LogEntry> result = service.getLogsByDevice("Device That Does Not Exist");
        assertEquals(0, result.size());
    }

    // -----------------------------------------------------------------------
    // getLogsByActor
    // -----------------------------------------------------------------------

    /**
     * Test: manual user actions must have at least one entry when filtered by user.
     */
    @Test
    public void getLogsByActorUserReturnsAtLeastOneEntry() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        ObservableList<LogEntry> result = service.getLogsByActor("User");
        assertTrue(result.size() >= 1);
    }

    /**
     * Test: manual user actions must be filterable by actor user.
     */
    @Test
    public void getLogsByActorUserReturnsOnlyUserEntries() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        ObservableList<LogEntry> result = service.getLogsByActor("User");
        for (LogEntry e : result) {
            assertEquals("User", e.getActor());
        }
    }

    /**
     * Test: rule-triggered entries must have at least one entry when filtered.
     */
    @Test
    public void getLogsByActorRuleReturnsAtLeastOneEntry() {
        service.addLogEntry("Dimmer Light", "Bedroom", "Set to 50%", "Rule: Morning Routine");
        ObservableList<LogEntry> result = service.getLogsByActor("Rule: Morning Routine");
        assertTrue(result.size() >= 1);
    }

    /**
     * Test: rule-triggered entries must be filterable by actor rule.
     */
    @Test
    public void getLogsByActorRuleReturnsOnlyRuleEntries() {
        service.addLogEntry("Dimmer Light", "Bedroom", "Set to 50%", "Rule: Morning Routine");
        ObservableList<LogEntry> result = service.getLogsByActor("Rule: Morning Routine");
        for (LogEntry e : result) {
            assertEquals("Rule: Morning Routine", e.getActor());
        }
    }

    /**
     * Test: schedule-triggered entries must have at least one entry when filtered.
     */
    @Test
    public void getLogsByActorScheduleReturnsAtLeastOneEntry() {
        service.addLogEntry("Kitchen Light", "Kitchen", "Turned OFF", "Schedule: Evening Shutdown");
        ObservableList<LogEntry> result = service.getLogsByActor("Schedule: Evening Shutdown");
        assertTrue(result.size() >= 1);
    }

    /**
     * Test: schedule-triggered entries must be filterable by actor schedule.
     */
    @Test
    public void getLogsByActorScheduleReturnsOnlyScheduleEntries() {
        service.addLogEntry("Kitchen Light", "Kitchen", "Turned OFF", "Schedule: Evening Shutdown");
        ObservableList<LogEntry> result = service.getLogsByActor("Schedule: Evening Shutdown");
        for (LogEntry e : result) {
            assertEquals("Schedule: Evening Shutdown", e.getActor());
        }
    }

    // -----------------------------------------------------------------------
    // exportToCSV
    // -----------------------------------------------------------------------

    /**
     * Test: export to CSV contains header.
     */
    @Test
    public void exportToCSVContainsHeader() {
        String csv = service.exportToCSV();
        assertTrue(csv.startsWith("Timestamp,Device,Room,Action,Actor"));
    }

    /**
     * Test: export to CSV contains device field of added entry.
     */
    @Test
    public void exportToCSVContainsDevice() {
        service.addLogEntry("Export Device", "Export Room", "Export Action", "Export Actor");
        String csv = service.exportToCSV();
        assertTrue(csv.contains("Export Device"));
    }

    /**
     * Test: export to CSV contains room field of added entry.
     */
    @Test
    public void exportToCSVContainsRoom() {
        service.addLogEntry("Export Device", "Export Room", "Export Action", "Export Actor");
        String csv = service.exportToCSV();
        assertTrue(csv.contains("Export Room"));
    }

    /**
     * Test: export to CSV contains action field of added entry.
     */
    @Test
    public void exportToCSVContainsAction() {
        service.addLogEntry("Export Device", "Export Room", "Export Action", "Export Actor");
        String csv = service.exportToCSV();
        assertTrue(csv.contains("Export Action"));
    }

    /**
     * Test: export to CSV contains actor field of added entry.
     */
    @Test
    public void exportToCSVContainsActor() {
        service.addLogEntry("Export Device", "Export Room", "Export Action", "Export Actor");
        String csv = service.exportToCSV();
        assertTrue(csv.contains("Export Actor"));
    }

    // -----------------------------------------------------------------------
    // resetForTesting
    // -----------------------------------------------------------------------

    /**
     * Test: after reset a new instance must be independent (fresh seed data, no carryover).
     */
    @Test
    public void resetForTestingCreatesNewInstance() {
        service.addLogEntry("Carry Device", "Room", "Action", "User");
        int sizeAfterAdd = service.getLogs().size();

        MockLogService.resetForTesting();
        MockLogService fresh = MockLogService.getInstance();

        // The new instance has seed data but not the extra entry we added before reset
        assertEquals(sizeAfterAdd - 1, fresh.getLogs().size());
    }
}
