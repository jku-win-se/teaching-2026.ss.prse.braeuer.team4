package at.jku.se.smarthome.service.rule;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.api.RoomService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Unit tests for RuleValidator.
 */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.CommentRequired",
        "PMD.MethodNamingConventions", "PMD.TooManyMethods",
        "PMD.UnitTestContainsTooManyAsserts"})
public class RuleValidatorTest {

    /** Stub room service with predictable device lookup. */
    private final RoomService rooms = new StubRoomService();

    @Test
    public void validate_unknownTriggerType_returnsInvalid() {
        RuleValidator.Result result = RuleValidator.validate("Magic", "x", null, rooms);
        assertFalse(result.valid());
    }

    @Test
    public void validate_timeWithBadWeekday_returnsInvalid() {
        RuleValidator.Result result = RuleValidator.validate("Time", "Funday 07:00", null, rooms);
        assertFalse(result.valid());
    }

    @Test
    public void validate_timeWithBadTimeFormat_returnsInvalid() {
        RuleValidator.Result result = RuleValidator.validate("Time", "Daily hello", null, rooms);
        assertFalse(result.valid());
    }

    @Test
    public void validate_timeValid_returnsValid() {
        assertTrue(RuleValidator.validate("Time", "07:00", null, rooms).valid());
        assertTrue(RuleValidator.validate("Time", "Daily 07:00", null, rooms).valid());
        assertTrue(RuleValidator.validate("Time", "Weekdays 06:00 AM", null, rooms).valid());
    }

    @Test
    public void validate_thresholdNonNumericValue_returnsInvalid() {
        RuleValidator.Result result = RuleValidator.validate("Sensor Threshold", "Value > hot", "Motion Sensor", rooms);
        assertFalse(result.valid());
    }

    @Test
    public void validate_thresholdUnknownOperator_returnsInvalid() {
        RuleValidator.Result result = RuleValidator.validate("Sensor Threshold", "Value <> 5", "Motion Sensor", rooms);
        assertFalse(result.valid());
    }

    @Test
    public void validate_thresholdSourceNotASensor_returnsInvalid() {
        RuleValidator.Result result = RuleValidator.validate("Sensor Threshold", "Value > 5", "Main Light", rooms);
        assertFalse(result.valid());
    }

    @Test
    public void validate_thresholdSourceUnresolvable_returnsInvalid() {
        RuleValidator.Result result = RuleValidator.validate("Sensor Threshold", "Value > 5", "Ghost", rooms);
        assertFalse(result.valid());
    }

    @Test
    public void validate_thresholdValid_returnsValid() {
        RuleValidator.Result result = RuleValidator.validate("Sensor Threshold", "Value > 5", "Motion Sensor", rooms);
        assertTrue(result.valid());
    }

    @Test
    public void validate_deviceStateMalformedCondition_returnsInvalid() {
        RuleValidator.Result result = RuleValidator.validate("Device State", "State = Unknown", "Main Light", rooms);
        assertFalse(result.valid());
    }

    @Test
    public void validate_deviceStateValid_returnsValid() {
        assertTrue(RuleValidator.validate("Device State", "State = Active", "Main Light", rooms).valid());
        assertTrue(RuleValidator.validate("Device State", "State = Inactive", "Main Light", rooms).valid());
        assertTrue(RuleValidator.validate("Device State", "State = On", "Main Light", rooms).valid());
        assertTrue(RuleValidator.validate("Device State", "State = Off", "Main Light", rooms).valid());
        assertTrue(RuleValidator.validate("Device State", "state = active", "Main Light", rooms).valid());
    }

    /**
     * Minimal RoomService stub exposing only the devices needed for validation tests.
     */
    @SuppressWarnings("PMD.TooManyMethods")
    private static final class StubRoomService implements RoomService {
        private static final String MOTION_SENSOR = "Motion Sensor";
        private static final String MAIN_LIGHT = "Main Light";

        @Override
        public Device getDeviceByName(String deviceName) {
            Device result = null;
            if (MOTION_SENSOR.equals(deviceName)) {
                result = new Device("d1", MOTION_SENSOR, "sensor", "Hallway", true);
            } else if (MAIN_LIGHT.equals(deviceName)) {
                result = new Device("d2", MAIN_LIGHT, "Switch", "Living Room", true);
            }
            return result;
        }

        @Override
        public ObservableList<Room> getRooms() {
            return FXCollections.observableArrayList();
        }

        @Override
        public Room getRoomById(String roomId) {
            return null;
        }

        @Override
        public Room addRoom(String name) {
            return null;
        }

        @Override
        public boolean updateRoomName(String roomId, String newName) {
            return false;
        }

        @Override
        public boolean deleteRoom(String roomId) {
            return false;
        }

        @Override
        public ObservableList<Device> getAllDevices() {
            return FXCollections.observableArrayList();
        }

        @Override
        public Device getDeviceById(String deviceId) {
            return null;
        }

        @Override
        public Device addDeviceToRoom(String roomId, String deviceName, String deviceType) {
            return null;
        }

        @Override
        public boolean removeDeviceFromRoom(String roomId, String deviceId) {
            return false;
        }

        @Override
        public boolean renameDevice(String roomId, String deviceId, String newName) {
            return false;
        }

        @Override
        public boolean updateDeviceState(String deviceId, boolean state) {
            return false;
        }

        @Override
        public boolean updateDeviceBrightness(String deviceId, int brightness) {
            return false;
        }

        @Override
        public boolean updateDeviceTemperature(String deviceId, double temperature) {
            return false;
        }
    }
}
