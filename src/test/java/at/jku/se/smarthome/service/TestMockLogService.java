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
     * Test: a new entry must appear at index 0 (newest-first ordering).
     */
    @Test
    public void addLogEntryInsertsAtFront() {
        int before = service.getLogs().size();
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        assertEquals(before + 1, service.getLogs().size());
        assertEquals("Garden Blind", service.getLogs().get(0).getDevice());
    }

    /**
     * Test: all four fields must be stored without mutation.
     */
    @Test
    public void addLogEntryStoresAllFields() {
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Garden Blind", entry.getDevice());
        assertEquals("Garden", entry.getRoom());
        assertEquals("Opened", entry.getAction());
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
     * Test: legacy overload must default room to unknown.
     */
    @Test
    public void addLogEntryLegacyWithoutRoomDefaultsToUnknown() {
        service.addLogEntry("Main Light", "Turned ON", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Main Light", entry.getDevice());
        assertEquals("Unknown", entry.getRoom());
        assertEquals("Turned ON", entry.getAction());
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
     * Test: get logs by room returns only matching entries.
     */
    @Test
    public void getLogsByRoomReturnsOnlyMatchingEntries() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        service.addLogEntry("Bed Light", "Bedroom", "Turned OFF", "User");
        ObservableList<LogEntry> result = service.getLogsByRoom("Living Room");
        assertTrue(result.size() >= 1);
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
     * Test: get logs by device returns only matching entries.
     */
    @Test
    public void getLogsByDeviceReturnsOnlyMatchingEntries() {
        service.addLogEntry("Unique Device ABC", "Room", "Action", "User");
        ObservableList<LogEntry> result = service.getLogsByDevice("Unique Device ABC");
        assertEquals(1, result.size());
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
     * Test: manual user actions must be filterable by actor user.
     */
    @Test
    public void getLogsByActorUserReturnsMatchingEntries() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        ObservableList<LogEntry> result = service.getLogsByActor("User");
        assertTrue(result.size() >= 1);
        for (LogEntry e : result) {
            assertEquals("User", e.getActor());
        }
    }

    /**
     * Test: rule-triggered entries must be filterable by actor rule.
     */
    @Test
    public void getLogsByActorRuleReturnsMatchingEntries() {
        service.addLogEntry("Dimmer Light", "Bedroom", "Set to 50%", "Rule: Morning Routine");
        ObservableList<LogEntry> result = service.getLogsByActor("Rule: Morning Routine");
        assertTrue(result.size() >= 1);
        for (LogEntry e : result) {
            assertEquals("Rule: Morning Routine", e.getActor());
        }
    }

    /**
     * Test: schedule-triggered entries must be filterable by actor schedule.
     */
    @Test
    public void getLogsByActorScheduleReturnsMatchingEntries() {
        service.addLogEntry("Kitchen Light", "Kitchen", "Turned OFF", "Schedule: Evening Shutdown");
        ObservableList<LogEntry> result = service.getLogsByActor("Schedule: Evening Shutdown");
        assertTrue(result.size() >= 1);
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
     * Test: export to CSV contains added entry.
     */
    @Test
    public void exportToCSVContainsAddedEntry() {
        service.addLogEntry("Export Device", "Export Room", "Export Action", "Export Actor");
        String csv = service.exportToCSV();
        assertTrue(csv.contains("Export Device"));
        assertTrue(csv.contains("Export Room"));
        assertTrue(csv.contains("Export Action"));
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
