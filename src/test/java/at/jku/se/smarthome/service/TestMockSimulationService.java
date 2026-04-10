package at.jku.se.smarthome.service;

import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Rule;
import at.jku.se.smarthome.model.SimulationDeviceState;
import at.jku.se.smarthome.service.mock.MockRoomService;
import at.jku.se.smarthome.service.mock.MockSimulationService;
import at.jku.se.smarthome.service.mock.MockSimulationService.SimulationConfiguration;
import at.jku.se.smarthome.service.mock.MockSimulationService.SimulationEvent;
import at.jku.se.smarthome.service.mock.MockSimulationService.SimulationPlan;
import javafx.collections.ObservableList;

public class TestMockSimulationService {

    private MockSimulationService service;

    @Before
    public void setUp() {
        MockRoomService.resetForTesting();
        MockSimulationService.resetForTesting();
        service = MockSimulationService.getInstance();
    }

    @Test
    public void buildPlan_createsSnapshotAndSortedEvents() {
        Rule timeRule = new Rule("rule-100", "Morning Routine", "Time", "Clock", "06:30 AM", "Turn On", "Main Light", true, "Active");
        Rule thresholdRule = new Rule("rule-101", "Heat Alert", "Sensor Threshold", "Temperature Sensor", "Value > 20", "Set to 22°C", "Temperature Control", true, "Active");
        Rule disabledRule = new Rule("rule-102", "Disabled", "Device State", "Bed Light", "State = Active", "Turn Off", "Bed Light", false, "Inactive");

        SimulationConfiguration configuration = new SimulationConfiguration(
                LocalTime.of(6, 0),
                21.5,
                45.0,
                List.of(timeRule, thresholdRule, disabledRule),
                4
        );

        SimulationPlan plan = service.buildPlan(configuration);

        assertNotNull(plan);
        assertEquals(MockRoomService.getInstance().getAllDevices().size(), plan.simulatedDeviceStates().size());
        assertEquals(2, plan.events().size());
        assertTrue(plan.events().get(0).simulatedTime().compareTo(plan.events().get(1).simulatedTime()) <= 0);
        assertEquals("Rule: Morning Routine", plan.events().get(0).triggerSource());
    }

    @Test
    public void applyEvent_updatesMatchingSimulationSnapshotEntry() {
        SimulationConfiguration configuration = new SimulationConfiguration(
                LocalTime.of(8, 0),
                20.0,
                40.0,
                List.of(),
                2
        );
        SimulationPlan plan = service.buildPlan(configuration);
        ObservableList<SimulationDeviceState> states = plan.simulatedDeviceStates();
        SimulationEvent event = new SimulationEvent(LocalTime.of(8, 30), "Main Light", "Living Room", "OFF", "Rule: Night");

        service.applyEvent(states, event);

        SimulationDeviceState state = states.stream()
                .filter(device -> device.getDeviceName().equals("Main Light"))
                .findFirst()
                .orElseThrow();
        assertEquals("OFF", state.getState());
        assertEquals("08:30:00", state.getLastChanged());
    }

    @Test
    public void parseStartTime_supportsShortAndLongFormat() {
        assertEquals(LocalTime.of(7, 15), service.parseStartTime("07:15"));
        assertEquals(LocalTime.of(7, 15, 30), service.parseStartTime("07:15:30"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseStartTime_rejectsInvalidFormat() {
        service.parseStartTime("invalid");
    }

    @Test
    public void buildPlan_mapsDifferentActionTypesToSimulationStates() {
        Rule notifyRule = new Rule("rule-103", "Notify", "Device State", "Bed Light", "State = Active", "Notify User", "Main Light", true, "Active");
        Rule blindRule = new Rule("rule-104", "Open Blind", "Device State", "Bed Light", "State = Active", "Open", "Hallway Blind", true, "Active");

        SimulationPlan plan = service.buildPlan(new SimulationConfiguration(
                LocalTime.of(9, 0),
                19.0,
                35.0,
                List.of(notifyRule, blindRule),
                1
        ));

        assertEquals(2, plan.events().size());
        assertTrue(plan.events().stream().anyMatch(event -> event.resultingState().equals("NOTIFY")));
        assertTrue(plan.events().stream().anyMatch(event -> event.resultingState().equals("OPEN")));
        assertFalse(plan.events().stream().anyMatch(event -> event.resultingState().equals("UNCHANGED")));
    }
}