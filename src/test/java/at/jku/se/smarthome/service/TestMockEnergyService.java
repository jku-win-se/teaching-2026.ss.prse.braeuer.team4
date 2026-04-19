package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.mock.MockEnergyService;
import at.jku.se.smarthome.service.mock.MockEnergyService.AggregationPeriod;
import at.jku.se.smarthome.service.mock.MockEnergyService.EnergySnapshot;

/**
 * Tests for {@link MockEnergyService}.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.TooManyMethods"})
public class TestMockEnergyService {


    /** Energy service under test. */
    private MockEnergyService service;

    /**
     * Sets up test fixtures before each test.
     */
    @Before
    public void setUp() {
        MockEnergyService.resetForTesting();
        service = MockEnergyService.getInstance();
    }

    /**
     * Test: snapshot day has correct period.
     */
    @Test
    public void snapshotDayHasCorrectPeriod() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.DAY);
        assertEquals(AggregationPeriod.DAY, snapshot.period());
    }

    /**
     * Test: snapshot day contains household total.
     */
    @Test
    public void snapshotDayContainsHouseholdTotal() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.DAY);
        assertEquals(28.2, snapshot.householdTotal(), 0.001);
    }

    /**
     * Test: snapshot day contains top device name.
     */
    @Test
    public void snapshotDayContainsTopDeviceName() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.DAY);
        assertEquals("Living Room Thermostat", snapshot.topDeviceName());
    }

    /**
     * Test: snapshot day contains top device consumption.
     */
    @Test
    public void snapshotDayContainsTopDeviceConsumption() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.DAY);
        assertEquals(10.6, snapshot.topDeviceConsumption(), 0.001);
    }

    /**
     * Test: snapshot day contains top room name.
     */
    @Test
    public void snapshotDayContainsTopRoomName() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.DAY);
        assertEquals("Living Room", snapshot.topRoomName());
    }

    /**
     * Test: snapshot day contains top room consumption.
     */
    @Test
    public void snapshotDayContainsTopRoomConsumption() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.DAY);
        assertEquals(15.4, snapshot.topRoomConsumption(), 0.001);
    }

    /**
     * Test: snapshot day contains consumption by room.
     */
    @Test
    public void snapshotDayContainsConsumptionByRoom() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.DAY);
        assertEquals(5, snapshot.consumptionByRoom().size());
    }

    /**
     * Test: snapshot day contains timeline data.
     */
    @Test
    public void snapshotDayContainsTimelineData() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.DAY);
        assertEquals(7, snapshot.timelineSeries().getData().size());
    }

    /**
     * Test: snapshot week has correct period.
     */
    @Test
    public void snapshotWeekHasCorrectPeriod() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.WEEK);
        assertEquals(AggregationPeriod.WEEK, snapshot.period());
    }

    /**
     * Test: snapshot week has correct timeline name.
     */
    @Test
    public void snapshotWeekHasCorrectTimelineName() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.WEEK);
        assertEquals("Daily Usage (kWh)", snapshot.timelineSeries().getName());
    }

    /**
     * Test: snapshot week contains weekly timeline data.
     */
    @Test
    public void snapshotWeekContainsWeeklyTimelineData() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.WEEK);
        assertEquals(7, snapshot.timelineSeries().getData().size());
    }

    /**
     * Test: snapshot week contains bathroom totals.
     */
    @Test
    public void snapshotWeekContainsBathroomTotals() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.WEEK);
        assertEquals(8.5, snapshot.consumptionByRoom().get("Bathroom"), 0.001);
    }

    /**
     * Test: snapshot week household total exceeds threshold.
     */
    @Test
    public void snapshotWeekHouseholdTotalExceedsThreshold() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.WEEK);
        assertTrue(snapshot.householdTotal() > 150.0);
    }
}