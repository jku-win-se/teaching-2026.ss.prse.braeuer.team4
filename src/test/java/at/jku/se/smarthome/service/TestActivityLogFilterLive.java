package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.LogEntry;
import at.jku.se.smarthome.service.mock.MockLogService;
import javafx.collections.transformation.FilteredList;

/**
 * Verifies the "live filter" contract that {@code ActivityLogController} relies on
 * for FR-08: the {@link FilteredList} wrapper used by the controller must
 * (a) react when the predicate is replaced (user changes a filter input) and
 * (b) re-evaluate the predicate against newly appended source entries
 * (a new log entry is recorded while a filter is active).
 *
 * <p>The filter logic mirrors what {@code ActivityLogController.matches()} does:
 * device equality and ISO-date prefix range.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods",
        "PMD.MethodNamingConventions", "PMD.UnitTestContainsTooManyAsserts",
        "PMD.CommentRequired", "PMD.TooManyStaticImports"})
public class TestActivityLogFilterLive {

    /** Sentinel meaning "no device filter" (matches controller). */
    private static final String ALL_DEVICES = "All Devices";
    /** Length of the ISO date prefix "yyyy-MM-dd" inside stored timestamps. */
    private static final int ISO_DATE_LENGTH = 10;

    /** Log service under test. */
    private MockLogService service;
    /** FilteredList wrapping the service's log list — same wiring as the controller uses. */
    private FilteredList<LogEntry> filtered;

    /** Resets the singleton, clears seeded entries, and creates the FilteredList. */
    @Before
    public void setUp() {
        MockLogService.resetForTesting();
        service = MockLogService.getInstance();
        // Start from a clean slate so counts are deterministic.
        service.getLogs().clear();
        filtered = new FilteredList<>(service.getLogs(), entry -> true);
    }

    /** Builds a predicate equivalent to ActivityLogController.matches(). */
    private static Predicate<LogEntry> buildPredicate(String selectedDevice,
                                                      String fromIso, String toIso) {
        final boolean filterByDevice =
                selectedDevice != null && !ALL_DEVICES.equals(selectedDevice);
        return entry -> {
            final String timestamp = entry.getTimestamp();
            final String date = timestamp == null || timestamp.length() < ISO_DATE_LENGTH
                    ? "" : timestamp.substring(0, ISO_DATE_LENGTH);
            final boolean deviceOk =
                    !filterByDevice || selectedDevice.equals(entry.getDevice());
            final boolean fromOk = fromIso == null || date.compareTo(fromIso) >= 0;
            final boolean toOk = toIso == null || date.compareTo(toIso) <= 0;
            return deviceOk && fromOk && toOk;
        };
    }

    private void seed(String timestamp, String device, String room, String action) {
        service.getLogs().add(new LogEntry(timestamp, device, room, action, "User"));
    }

    /**
     * Aspect 1: changing the predicate (= user picks a device in the combo box)
     * must immediately update the filtered view.
     */
    @Test
    public void changingPredicate_updatesFilteredViewImmediately() {
        seed("2026-04-01 10:00:00", "Light A", "Living", "ON");
        seed("2026-04-02 10:00:00", "Light B", "Bedroom", "ON");
        seed("2026-04-03 10:00:00", "Light A", "Living", "OFF");
        assertEquals("baseline: predicate=true keeps all 3 entries", 3, filtered.size());

        filtered.setPredicate(buildPredicate("Light A", null, null));
        assertEquals("after device filter: only 2 Light A entries remain", 2, filtered.size());
        assertTrue(filtered.stream().allMatch(e -> "Light A".equals(e.getDevice())));

        filtered.setPredicate(buildPredicate(ALL_DEVICES, null, null));
        assertEquals("ALL_DEVICES sentinel disables device filter", 3, filtered.size());
    }

    /**
     * Aspect 2: while a filter is active, appending a new matching entry to the
     * source list must surface it in the filtered view; appending a non-matching
     * entry must not. This is the property that makes the filter "live" with
     * respect to incoming log entries (e.g. user toggles a device while viewing
     * the activity log).
     */
    @Test
    public void newSourceEntry_isFilteredLive_throughActivePredicate() {
        seed("2026-04-01 10:00:00", "Light A", "Living", "ON");
        filtered.setPredicate(buildPredicate("Light A", null, null));
        assertEquals(1, filtered.size());

        // Live append of a matching entry — filter should let it through.
        service.addLogEntry("Light A", "Living", "OFF", "User");
        assertEquals("matching live entry must appear in filtered view", 2, filtered.size());

        // Live append of a non-matching entry — filter should hide it.
        service.addLogEntry("Light B", "Bedroom", "ON", "User");
        assertEquals("non-matching live entry must stay hidden", 2, filtered.size());

        // Source list still has all 3 entries — filter is a view, not a copy.
        assertEquals("source list still grows with every addLogEntry", 3, service.getLogs().size());
    }

    /**
     * Aspect 3: date-range predicate uses ISO-prefix lexicographic comparison,
     * matching the controller's implementation.
     */
    @Test
    public void dateRangePredicate_filtersByIsoDatePrefix() {
        seed("2026-03-31 23:59:59", "Light A", "Living", "OFF");
        seed("2026-04-01 00:00:00", "Light A", "Living", "ON");
        seed("2026-04-15 12:00:00", "Light A", "Living", "ON");
        seed("2026-05-01 00:00:00", "Light A", "Living", "OFF");

        filtered.setPredicate(buildPredicate(ALL_DEVICES, "2026-04-01", "2026-04-30"));
        assertEquals("from/to inclusive on ISO date prefix", 2, filtered.size());
        assertTrue(filtered.stream()
                .allMatch(e -> e.getTimestamp().startsWith("2026-04-")));
    }

    /**
     * Aspect 4: combined device + date range. Verifies the conjunction of the
     * three sub-conditions in the predicate.
     */
    @Test
    public void combinedDeviceAndDate_intersectsConditions() {
        seed("2026-04-01 10:00:00", "Light A", "Living", "ON");
        seed("2026-04-01 10:00:00", "Light B", "Bedroom", "ON");
        seed("2026-04-15 10:00:00", "Light A", "Living", "OFF");
        seed("2026-05-01 10:00:00", "Light A", "Living", "ON");

        filtered.setPredicate(buildPredicate("Light A", "2026-04-01", "2026-04-30"));
        assertEquals("device=A AND April-only -> 2 entries", 2, filtered.size());
        assertTrue(filtered.stream().allMatch(e -> "Light A".equals(e.getDevice())));
        assertTrue(filtered.stream().allMatch(e -> e.getTimestamp().startsWith("2026-04-")));
    }
}
