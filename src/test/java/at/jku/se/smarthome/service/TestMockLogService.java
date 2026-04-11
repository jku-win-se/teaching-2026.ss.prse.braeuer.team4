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

    private MockLogService service;

    @Before
    public void setUp() {
        MockLogService.resetForTesting();
        service = MockLogService.getInstance();
    }

    // -----------------------------------------------------------------------
    // addLogEntry (4-arg, with room)
    // -----------------------------------------------------------------------

    /** A new entry must appear at index 0 (newest-first ordering). */
    @Test
    public void addLogEntry_insertsAtFront() {
        int before = service.getLogs().size();
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        assertEquals(before + 1, service.getLogs().size());
        assertEquals("Garden Blind", service.getLogs().get(0).getDevice());
    }

    /** All four fields must be stored without mutation. */
    @Test
    public void addLogEntry_storesAllFields() {
        service.addLogEntry("Garden Blind", "Garden", "Opened", "User");
        LogEntry entry = service.getLogs().get(0);
        assertEquals("Garden Blind", entry.getDevice());
        assertEquals("Garden", entry.getRoom());
        assertEquals("Opened", entry.getAction());
        assertEquals("User", entry.getActor());
    }

    /** Timestamp must be auto-generated and non-empty. */
    @Test
    public void addLogEntry_timestampIsNotBlank() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        assertFalse(service.getLogs().get(0).getTimestamp().isBlank());
    }

    // -----------------------------------------------------------------------
    // addLogEntry (3-arg legacy, without room)
    // -----------------------------------------------------------------------

    /** Legacy overload must default room to "Unknown". */
    @Test
    public void addLogEntry_legacyWithoutRoom_defaultsToUnknown() {
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

    /** After a fresh init the service always has at least the seeded entries. */
    @Test
    public void getLogs_returnsAtLeastSeedEntries() {
        assertTrue(service.getLogs().size() >= 5);
    }

    // -----------------------------------------------------------------------
    // getLogsByRoom
    // -----------------------------------------------------------------------

    @Test
    public void getLogsByRoom_returnsOnlyMatchingEntries() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        service.addLogEntry("Bed Light", "Bedroom", "Turned OFF", "User");
        ObservableList<LogEntry> result = service.getLogsByRoom("Living Room");
        assertTrue(result.size() >= 1);
        for (LogEntry e : result) {
            assertEquals("Living Room", e.getRoom());
        }
    }

    @Test
    public void getLogsByRoom_noMatch_returnsEmpty() {
        ObservableList<LogEntry> result = service.getLogsByRoom("Nonexistent Room XYZ");
        assertEquals(0, result.size());
    }

    // -----------------------------------------------------------------------
    // getLogsByDevice
    // -----------------------------------------------------------------------

    @Test
    public void getLogsByDevice_returnsOnlyMatchingEntries() {
        service.addLogEntry("Unique Device ABC", "Room", "Action", "User");
        ObservableList<LogEntry> result = service.getLogsByDevice("Unique Device ABC");
        assertEquals(1, result.size());
        assertEquals("Unique Device ABC", result.get(0).getDevice());
    }

    @Test
    public void getLogsByDevice_noMatch_returnsEmpty() {
        ObservableList<LogEntry> result = service.getLogsByDevice("Device That Does Not Exist");
        assertEquals(0, result.size());
    }

    // -----------------------------------------------------------------------
    // getLogsByActor
    // -----------------------------------------------------------------------

    /** Manual user actions must be filterable by actor "User". */
    @Test
    public void getLogsByActor_user_returnsMatchingEntries() {
        service.addLogEntry("Main Light", "Living Room", "Turned ON", "User");
        ObservableList<LogEntry> result = service.getLogsByActor("User");
        assertTrue(result.size() >= 1);
        for (LogEntry e : result) {
            assertEquals("User", e.getActor());
        }
    }

    /** Rule-triggered entries must be filterable by actor "Rule: Morning Routine". */
    @Test
    public void getLogsByActor_rule_returnsMatchingEntries() {
        service.addLogEntry("Dimmer Light", "Bedroom", "Set to 50%", "Rule: Morning Routine");
        ObservableList<LogEntry> result = service.getLogsByActor("Rule: Morning Routine");
        assertTrue(result.size() >= 1);
        for (LogEntry e : result) {
            assertEquals("Rule: Morning Routine", e.getActor());
        }
    }

    /** Schedule-triggered entries must be filterable by actor "Schedule: Evening Shutdown". */
    @Test
    public void getLogsByActor_schedule_returnsMatchingEntries() {
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

    @Test
    public void exportToCSV_containsHeader() {
        String csv = service.exportToCSV();
        assertTrue(csv.startsWith("Timestamp,Device,Room,Action,Actor"));
    }

    @Test
    public void exportToCSV_containsAddedEntry() {
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

    /** After reset a new instance must be independent (fresh seed data, no carryover). */
    @Test
    public void resetForTesting_createsNewInstance() {
        service.addLogEntry("Carry Device", "Room", "Action", "User");
        int sizeAfterAdd = service.getLogs().size();

        MockLogService.resetForTesting();
        MockLogService fresh = MockLogService.getInstance();

        // The new instance has seed data but not the extra entry we added before reset
        assertEquals(sizeAfterAdd - 1, fresh.getLogs().size());
    }
}
