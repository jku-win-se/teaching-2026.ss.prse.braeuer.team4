package at.jku.se.smarthome.model;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Robust unit tests for all model classes to maximize coverage.
 * These tests are strictly for data objects (models) and have no external dependencies.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
public class RobustModelCoverageTest {

    /**
     * Default constructor.
     */
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public RobustModelCoverageTest() {
        // Default constructor
    }

    /**
     * Tests the Device model.
     */
    @Test
    public void testDeviceModel() {
        Device device = new Device("id1", "name1", "type1", "room1", true);
        assertEquals("id1", device.getId());
        assertEquals("name1", device.getName());
        assertEquals("type1", device.getType());
        assertEquals("room1", device.getRoom());
        assertTrue(device.getState());

        device.setId("id2");
        device.setName("name2");
        device.setType("type2");
        device.setRoom("room2");
        device.setState(false);
        device.setBrightness(75);
        device.setTemperature(21.5);
        device.setCurrentValue(99.9);

        assertEquals("id2", device.getId());
        assertEquals("name2", device.getName());
        assertEquals("type2", device.getType());
        assertEquals("room2", device.getRoom());
        assertFalse(device.getState());
        assertEquals(75, device.getBrightness());
        assertEquals(21.5, device.getTemperature(), 0.01);
        assertEquals(99.9, device.getCurrentValue(), 0.01);

        assertNotNull(device.idProperty());
        assertNotNull(device.nameProperty());
        assertNotNull(device.typeProperty());
        assertNotNull(device.roomProperty());
        assertNotNull(device.stateProperty());
        assertNotNull(device.brightnessProperty());
        assertNotNull(device.temperatureProperty());
        assertNotNull(device.currentValueProperty());
        assertTrue(device.toString().contains("name2"));
    }

    /**
     * Tests the Schedule model.
     */
    @Test
    public void testScheduleModel() {
        Schedule schedule = new Schedule("sid", "sname", "did", "dname", "action", "time", "recur", true);
        assertEquals("sid", schedule.getId());
        assertEquals("sname", schedule.getName());
        assertEquals("did", schedule.getDeviceId());
        assertEquals("dname", schedule.getDevice());
        assertEquals("action", schedule.getAction());
        assertEquals("time", schedule.getTime());
        assertEquals("recur", schedule.getRecurrence());
        assertTrue(schedule.isActive());

        schedule.setName("n2");
        schedule.setDeviceId("d2");
        schedule.setDevice("dn2");
        schedule.setAction("a2");
        schedule.setTime("t2");
        schedule.setRecurrence("r2");
        schedule.setActive(false);

        assertEquals("n2", schedule.getName());
        assertEquals("d2", schedule.getDeviceId());
        assertEquals("dn2", schedule.getDevice());
        assertEquals("a2", schedule.getAction());
        assertEquals("t2", schedule.getTime());
        assertEquals("r2", schedule.getRecurrence());
        assertFalse(schedule.isActive());

        assertNotNull(schedule.idProperty());
        assertNotNull(schedule.nameProperty());
        assertNotNull(schedule.deviceIdProperty());
        assertNotNull(schedule.deviceProperty());
        assertNotNull(schedule.actionProperty());
        assertNotNull(schedule.timeProperty());
        assertNotNull(schedule.recurrenceProperty());
        assertNotNull(schedule.activeProperty());

        Schedule schedule2 = new Schedule("s2", "sn2", "did2", "act2", "t2", "rec2", true);
        assertEquals("did2", schedule2.getDeviceId());
        assertEquals("did2", schedule2.getDevice());
    }

    /**
     * Tests the Rule model.
     */
    @Test
    public void testRuleModel() {
        Rule rule = new Rule("rid", "rname", "trigger", "source", "cond", "action", "target", true, "status");
        assertEquals("rid", rule.getId());
        assertEquals("rname", rule.getName());
        assertEquals("trigger", rule.getTriggerType());
        assertEquals("source", rule.getSourceDevice());
        assertEquals("cond", rule.getCondition());
        assertEquals("action", rule.getAction());
        assertEquals("target", rule.getTargetDevice());
        assertTrue(rule.isEnabled());
        assertEquals("status", rule.getStatus());

        rule.setName("rn2");
        rule.setTriggerType("tt2");
        rule.setSourceDevice("sd2");
        rule.setCondition("c2");
        rule.setAction("a2");
        rule.setTargetDevice("td2");
        rule.setEnabled(false);
        rule.setStatus("st2");

        assertEquals("rn2", rule.getName());
        assertEquals("tt2", rule.getTriggerType());
        assertEquals("sd2", rule.getSourceDevice());
        assertEquals("c2", rule.getCondition());
        assertEquals("a2", rule.getAction());
        assertEquals("td2", rule.getTargetDevice());
        assertFalse(rule.isEnabled());
        assertEquals("st2", rule.getStatus());

        assertNotNull(rule.idProperty());
        assertNotNull(rule.nameProperty());
        assertNotNull(rule.triggerTypeProperty());
        assertNotNull(rule.sourceDeviceProperty());
        assertNotNull(rule.conditionProperty());
        assertNotNull(rule.actionProperty());
        assertNotNull(rule.targetDeviceProperty());
        assertNotNull(rule.enabledProperty());
        assertNotNull(rule.statusProperty());
    }

    /**
     * Tests the User model.
     */
    @Test
    public void testUserModel() {
        User user = new User("e1", "u1", "p1", "r1", "s1");
        assertEquals("e1", user.getEmail());
        assertEquals("u1", user.getUsername());
        assertEquals("p1", user.getPassword());
        assertEquals("r1", user.getRole());
        assertEquals("s1", user.getStatus());

        user.setEmail("e2");
        user.setUsername("u2");
        user.setPassword("p2");
        user.setRole("r2");
        user.setStatus("s2");

        assertEquals("e2", user.getEmail());
        assertEquals("u2", user.getUsername());
        assertEquals("p2", user.getPassword());
        assertEquals("r2", user.getRole());
        assertEquals("s2", user.getStatus());

        assertNotNull(user.emailProperty());
        assertNotNull(user.usernameProperty());
        assertNotNull(user.passwordProperty());
        assertNotNull(user.roleProperty());
        assertNotNull(user.statusProperty());
    }

    /**
     * Tests the Room model.
     */
    @Test
    public void testRoomModel() {
        Room room = new Room("rid", "rname", 5);
        assertEquals("rid", room.getId());
        assertEquals("rname", room.getName());
        assertEquals(5, room.getDeviceCount());

        room.setId("id2");
        room.setName("name2");
        room.setDeviceCount(10);
        assertEquals("id2", room.getId());
        assertEquals("name2", room.getName());
        assertEquals(10, room.getDeviceCount());

        Device device = new Device("d1", "n1", "t1", "r1", true);
        room.addDevice(device);
        assertEquals(1, room.getDevices().size());
        room.removeDevice(device);
        assertEquals(0, room.getDevices().size());

        assertNotNull(room.idProperty());
        assertNotNull(room.nameProperty());
        assertNotNull(room.deviceCountProperty());
    }

    /**
     * Tests the LogEntry model.
     */
    @Test
    public void testLogEntryModel() {
        LogEntry entry = new LogEntry("ts", "dev", "room", "act", "actor");
        assertEquals("ts", entry.getTimestamp());
        assertEquals("dev", entry.getDevice());
        assertEquals("room", entry.getRoom());
        assertEquals("act", entry.getAction());
        assertEquals("actor", entry.getActor());

        LogEntry entry2 = new LogEntry("ts", "dev", "act", "actor");
        assertEquals("Unknown", entry2.getRoom());

        assertNotNull(entry.timestampProperty());
        assertNotNull(entry.deviceProperty());
        assertNotNull(entry.roomProperty());
        assertNotNull(entry.actionProperty());
        assertNotNull(entry.actorProperty());
    }

    /**
     * Tests the NotificationEntry model.
     */
    @Test
    public void testNotificationEntryModel() {
        NotificationEntry entry = new NotificationEntry("ts", "msg", NotificationType.INFO, true);
        assertEquals("ts", entry.getTimestamp());
        assertEquals("msg", entry.getMessage());
        assertEquals(NotificationType.INFO, entry.getType());
        assertTrue(entry.isRead());

        entry.markRead();
        assertTrue(entry.isRead());

        NotificationEntry entry2 = new NotificationEntry("ts", "msg", NotificationType.WARNING);
        assertFalse(entry2.isRead());

        assertNotNull(entry.timestampProperty());
        assertNotNull(entry.messageProperty());
        assertNotNull(entry.readProperty());
    }

    /**
     * Tests the SchedulingConflict model.
     */
    @Test
    public void testSchedulingConflictModel() {
        SchedulingConflict conflict = new SchedulingConflict("cid", "candId", "existId", "candName", "existName", 
                                "devId", "devName", "candVal", "existVal", "candTime", "existTime", "desc");
        assertEquals("cid", conflict.getConflictId());
        assertEquals("candId", conflict.getCandidateId());
        assertEquals("existId", conflict.getConflictingId());
        assertEquals("candName", conflict.getCandidateName());
        assertEquals("existName", conflict.getConflictingName());
        assertEquals("devId", conflict.getDeviceId());
        assertEquals("devName", conflict.getDeviceName());
        assertEquals("candVal", conflict.getCandidateValue());
        assertEquals("existVal", conflict.getConflictingValue());
        assertEquals("candTime", conflict.getCandidateTime());
        assertEquals("existTime", conflict.getConflictingTime());
        assertEquals("desc", conflict.getDescription());

        assertTrue(conflict.toString().contains("cid"));
    }

    /**
     * Tests the SimulationDeviceState model.
     */
    @Test
    public void testSimulationDeviceStateModel() {
        SimulationDeviceState state = new SimulationDeviceState("name", "room", "type", "state", "last");
        assertEquals("name", state.getDeviceName());
        assertEquals("room", state.getRoom());
        assertEquals("type", state.getType());
        assertEquals("state", state.getState());
        assertEquals("last", state.getLastChanged());

        state.setState("s2");
        state.setLastChanged("l2");
        assertEquals("s2", state.getState());
        assertEquals("l2", state.getLastChanged());

        assertNotNull(state.deviceNameProperty());
        assertNotNull(state.roomProperty());
        assertNotNull(state.typeProperty());
        assertNotNull(state.stateProperty());
        assertNotNull(state.lastChangedProperty());
    }

    /**
     * Tests the IntegrationDevice model.
     */
    @Test
    public void testIntegrationDeviceModel() {
        IntegrationDevice device = new IntegrationDevice("name", "type", "topic", "status");
        assertEquals("name", device.getName());
        assertEquals("type", device.getType());
        assertEquals("topic", device.getTopic());
        assertEquals("status", device.getStatus());

        assertNotNull(device.nameProperty());
        assertNotNull(device.typeProperty());
        assertNotNull(device.topicProperty());
        assertNotNull(device.statusProperty());
    }

    /**
     * Tests the Scene model.
     */
    @Test
    public void testSceneModel() {
        Scene scene = new Scene("id", "name", "desc");
        assertEquals("id", scene.getId());
        assertEquals("name", scene.getName());
        assertEquals("desc", scene.getDescription());

        scene.setName("n2");
        scene.setDescription("d2");
        assertEquals("n2", scene.getName());
        assertEquals("d2", scene.getDescription());

        scene.addDeviceState("ds1");
        assertEquals(1, scene.getDeviceStates().size());
        scene.removeDeviceState("ds1");
        assertEquals(0, scene.getDeviceStates().size());

        scene.setDeviceStates(List.of("a", "b"));
        assertEquals(2, scene.getDeviceStates().size());

        assertNotNull(scene.idProperty());
        assertNotNull(scene.nameProperty());
        assertNotNull(scene.descriptionProperty());
    }

    /**
     * Tests the VacationModeConfig model.
     */
    @Test
    public void testVacationModeConfigModel() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.NOON;
        LocalTime endTime = LocalTime.MIDNIGHT;
        
        VacationModeConfig config = new VacationModeConfig(true, start, end, "sid");
        assertTrue(config.isEnabled());
        assertEquals(start, config.getStartDate());
        assertEquals(end, config.getEndDate());
        assertEquals("sid", config.getScheduleId());
        assertTrue(config.isConfigured());

        config.setEnabled(false);
        config.setStartDate(null);
        config.setEndDate(null);
        config.setStartTime(startTime);
        config.setEndTime(endTime);
        config.setScheduleId(null);

        assertFalse(config.isEnabled());
        assertNull(config.getStartDate());
        assertNull(config.getEndDate());
        assertEquals(startTime, config.getStartTime());
        assertEquals(endTime, config.getEndTime());
        assertNull(config.getScheduleId());
        assertFalse(config.isConfigured());
    }
}
