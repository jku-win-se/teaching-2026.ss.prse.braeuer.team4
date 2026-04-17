package at.jku.se.smarthome.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.mock.MockEnergyService;
import at.jku.se.smarthome.service.mock.MockEnergyService.AggregationPeriod;
import at.jku.se.smarthome.service.mock.MockEnergyService.EnergySnapshot;

public class TestMockEnergyService {

    private MockEnergyService service;

    @Before
    public void setUp() {
        MockEnergyService.resetForTesting();
        service = MockEnergyService.getInstance();
    }

    @Test
    public void getSnapshotDayContainsExpectedTotalsAndRanking() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.DAY);

        assertEquals(AggregationPeriod.DAY, snapshot.period());
        assertEquals(28.2, snapshot.householdTotal(), 0.001);
        assertEquals("Living Room Thermostat", snapshot.topDeviceName());
        assertEquals(10.6, snapshot.topDeviceConsumption(), 0.001);
        assertEquals("Living Room", snapshot.topRoomName());
        assertEquals(15.4, snapshot.topRoomConsumption(), 0.001);
        assertEquals(5, snapshot.consumptionByRoom().size());
        assertEquals(7, snapshot.timelineSeries().getData().size());
    }

    @Test
    public void getSnapshotWeekUsesWeeklyTimelineAndBathroomTotals() {
        EnergySnapshot snapshot = service.getSnapshot(AggregationPeriod.WEEK);

        assertEquals(AggregationPeriod.WEEK, snapshot.period());
        assertEquals("Daily Usage (kWh)", snapshot.timelineSeries().getName());
        assertEquals(7, snapshot.timelineSeries().getData().size());
        assertEquals(8.5, snapshot.consumptionByRoom().get("Bathroom"), 0.001);
        assertTrue(snapshot.householdTotal() > 150.0);
    }
}