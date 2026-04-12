package at.jku.se.smarthome.controller;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.service.api.RoomService;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockUserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// initialize JavaFX toolkit via JFXPanel
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Unit tests for {@link RoomsController} that exercise non-blocking UI logic
 * such as selection handling and enabling/disabling of action buttons.
 */
public class TestRoomsController {

    private RoomsController controller;
    private RoomService inMemoryRoomService;
    private Room roomA;

    @Before
    public void setUp() throws Exception {
        // Initialize JavaFX toolkit for controls used in the tests.
        // Prefer Platform.startup() when available; fall back to creating a JFXPanel.
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            try {
                java.lang.reflect.Method startup = platformClass.getMethod("startup", Runnable.class);
                try {
                    startup.invoke(null, (Runnable) () -> {});
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    // If toolkit already started or startup raised at runtime, ignore - we only need it running.
                }
            } catch (NoSuchMethodException nsme) {
                try {
                    Class<?> jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel");
                    jfxPanelClass.getConstructor().newInstance();
                } catch (ClassNotFoundException cnfe) {
                    // JavaFX not on classpath; tests that rely on controls will fail later.
                }
            }
        } catch (ClassNotFoundException cnfe) {
            // Platform class not available; try JFXPanel as fallback
            try {
                Class<?> jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel");
                jfxPanelClass.getConstructor().newInstance();
            } catch (ClassNotFoundException cnf2) {
                // JavaFX not on classpath; tests that rely on controls will fail later.
            }
        }

        MockUserService.resetForTesting();
        // Avoid calling login (which accesses DB). Instead set the current user to Owner via reflection.
        var mu = MockUserService.getInstance();
        try {
            java.lang.reflect.Field emailField = mu.getClass().getDeclaredField("currentUserEmail");
            emailField.setAccessible(true);
            emailField.set(mu, "owner@smarthome.com");
            java.lang.reflect.Field roleField = mu.getClass().getDeclaredField("currentUserRole");
            roleField.setAccessible(true);
            roleField.set(mu, "Owner");
        } catch (ReflectiveOperationException roe) {
            // If reflection fails, tests may still run but permission checks could fail.
        }

        inMemoryRoomService = new RoomService() {
            private final ObservableList<Room> rooms = FXCollections.observableArrayList();

            @Override
            public ObservableList<Room> getRooms() { return rooms; }

            @Override
            public Room getRoomById(String roomId) { return rooms.stream().filter(r -> r.getId().equals(roomId)).findFirst().orElse(null); }

            @Override
            public Room addRoom(String name) {
                Room r = new Room(UUID.randomUUID().toString(), name, 0);
                rooms.add(r);
                return r;
            }

            @Override
            public boolean updateRoomName(String roomId, String newName) { return false; }

            @Override
            public boolean deleteRoom(String roomId) { return false; }

            @Override
            public ObservableList<Device> getAllDevices() { return FXCollections.observableArrayList(); }

            @Override
            public Device getDeviceById(String deviceId) { return null; }

            @Override
            public Device getDeviceByName(String deviceName) { return null; }

            @Override
            public Device addDeviceToRoom(String roomId, String deviceName, String deviceType) { return null; }

            @Override
            public boolean removeDeviceFromRoom(String roomId, String deviceId) { return false; }

            @Override
            public boolean renameDevice(String roomId, String deviceId, String newName) { return false; }
        };

        roomA = inMemoryRoomService.addRoom("Alpha");
        roomA.addDevice(new Device(UUID.randomUUID().toString(), "Dev1", "Switch", roomA.getName(), false));

        ServiceRegistry.setRoomServiceForTesting(inMemoryRoomService);

        controller = new RoomsController();

        // Inject UI fields
        setPrivateField(controller, "roomsTable", new TableView<Room>());
        setPrivateField(controller, "nameColumn", new TableColumn<Room, String>("name"));
        setPrivateField(controller, "deviceCountColumn", new TableColumn<Room, Integer>("count"));
        setPrivateField(controller, "devicesTable", new TableView<Device>());
        setPrivateField(controller, "deviceNameColumn", new TableColumn<Device, String>("dname"));
        setPrivateField(controller, "deviceTypeColumn", new TableColumn<Device, String>("dtype"));
        setPrivateField(controller, "addRoomBtn", new Button());
        setPrivateField(controller, "addDeviceBtn", new Button());
        setPrivateField(controller, "selectedRoomLabel", new Label());

        // Call initialize
        Method init = RoomsController.class.getDeclaredMethod("initialize");
        init.setAccessible(true);
        init.invoke(controller);
    }

    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    public void selectingRoom_updatesSelectedLabelAndEnablesAddDevice() throws Exception {
        TableView<Room> roomsTable = (TableView<Room>) getPrivateField(controller, "roomsTable");
        // The controller initialize() sets the table items to the service list already.
        // Ensure the table contains the test room
        assertFalse("Rooms table must contain test data", roomsTable.getItems().isEmpty());

        // select first row; selection listener should update label and devices table
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                roomsTable.getSelectionModel().select(0);
            }
        });

        Label lbl = (Label) getPrivateField(controller, "selectedRoomLabel");
        assertEquals("Selected Room: " + roomA.getName(), lbl.getText());

        Button addDeviceBtn = (Button) getPrivateField(controller, "addDeviceBtn");
        assertFalse("Add device button must be enabled when a room is selected", addDeviceBtn.isDisabled());
    }

    private static Object getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
}






