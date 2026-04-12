package at.jku.se.smarthome.controller;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.se.smarthome.service.real.room.JdbcRoomService;
import at.jku.se.smarthome.service.real.auth.JdbcUserRegistrationStore;
import at.jku.se.smarthome.service.real.auth.UserRegistrationStore;
import at.jku.se.smarthome.service.api.ServiceRegistry;
import at.jku.se.smarthome.service.mock.MockUserService;
import at.jku.se.smarthome.model.Room;
import at.jku.se.smarthome.model.Device;
import at.jku.se.smarthome.config.DatabaseConfig;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

/**
 * Integrationstests für den {@code DevicesController} zur Validierung der UI-Logik
 * und der JDBC-Anbindung (H2 In-Memory). Die Tests vermeiden Reflection und
 * initialisieren den Controller über FXML, sodass {@code @FXML}-Injection
 * stattfindet.
 */
public class TestDevicesController {

    private JdbcRoomService roomService;

    @Before
    public void setUp() throws Exception {
        // configure H2 in-memory DB for both auth and rooms via system properties
        // use a unique in-memory database name per test to avoid cross-test contamination
        String uniqueDb = "rooms_" + System.nanoTime();
        System.setProperty("smarthome.db.url", "jdbc:h2:mem:" + uniqueDb + ";DB_CLOSE_DELAY=-1");
        System.setProperty("smarthome.db.user", "sa");
        System.setProperty("smarthome.db.password", "");

        // Ensure DatabaseConfig picks up system properties
        assertTrue(DatabaseConfig.load().isPresent());

        // Start JavaFX toolkit (safe to call multiple times; ignore IllegalState)
        try {
            Platform.startup(() -> {
                // no-op
            });
        } catch (IllegalStateException ignored) {
            // already started
        }

        // Reset singletons for isolation
        JdbcRoomService.resetForTesting();
        ServiceRegistry.resetRoomServiceForTesting();
        MockUserService.resetForTesting();

        // Create room service and persist initial state
        roomService = JdbcRoomService.getInstance();
        ServiceRegistry.setRoomServiceForTesting(roomService);

        // Ensure auth DB has an owner user so we can login when needed
        JdbcUserRegistrationStore userStore = new JdbcUserRegistrationStore();
        try {
            var existing = userStore.findByEmail("owner@smarthome.com");
            if (existing.isEmpty()) {
                userStore.save(new UserRegistrationStore.PersistedUser("owner@smarthome.com", "owner", "ownerpass", "Owner", "Active"));
            }
        } catch (Exception e) {
            // If any error occurs here, tests may still proceed but login-based checks will fail.
        }
    }

    @After
    public void tearDown() {
        // clear singletons
        JdbcRoomService.resetForTesting();
        ServiceRegistry.resetRoomServiceForTesting();
        MockUserService.resetForTesting();
    }

    private Parent loadControllerRoot() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Parent[] rootHolder = new Parent[1];
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test_devices.fxml"));
                rootHolder[0] = loader.load();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout loading FXML");
        return rootHolder[0];
    }

    private Object[] loadControllerAndRoot() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Parent[] rootHolder = new Parent[1];
        final DevicesController[] controllerHolder = new DevicesController[1];
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/test_devices.fxml"));
                rootHolder[0] = loader.load();
                controllerHolder[0] = loader.getController();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout loading FXML");
        return new Object[]{controllerHolder[0], rootHolder[0]};
    }

    /**
     * Helper to run code on the JavaFX thread and wait for completion.
     *
     * @param r Runnable that will be executed on the JavaFX Application Thread
     * @throws Exception if the FX task does not complete in time
     */
    private void runAndWait(Runnable r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout waiting for FX runLater");
    }

    /**
     * Überprüft, ob {@code updateActionOptions} für Geräte des Typs
     * Switch, Dimmer und Thermostat die erwarteten Aktions-Items liefert.
     * Erwartung: für Switch "Turn On"/"Turn Off", für Dimmer die
     * Set-to-Einträge und für Thermostat Einträge die mit "Set to" beginnen.
     */
    @Test
    public void updateActionOptions_switches_and_dimmer_and_thermostat() throws Exception {
        Object[] pair = loadControllerAndRoot();
        DevicesController controller = (DevicesController) pair[0];

        javafx.scene.control.ComboBox<Device> deviceCombo = new javafx.scene.control.ComboBox<>();
        javafx.scene.control.ComboBox<String> actionCombo = new javafx.scene.control.ComboBox<>();

        Device switchDevice = new Device("id1", "S1", "Switch", "R", true);
        deviceCombo.getItems().add(switchDevice);
        deviceCombo.setValue(switchDevice);
        controller.updateActionOptions(deviceCombo, actionCombo, null);
        assertTrue(actionCombo.getItems().contains("Turn On"));

        Device dimmer = new Device("id2", "D1", "Dimmer", "R", true);
        deviceCombo.getItems().setAll(dimmer);
        deviceCombo.setValue(dimmer);
        controller.updateActionOptions(deviceCombo, actionCombo, "Set to 50%");
        assertTrue(actionCombo.getItems().contains("Set to 50%"));
        assertEquals("Set to 50%", actionCombo.getValue());

        Device thermo = new Device("id3", "T1", "Thermostat", "R", true);
        deviceCombo.getItems().setAll(thermo);
        deviceCombo.setValue(thermo);
        controller.updateActionOptions(deviceCombo, actionCombo, null);
        assertTrue(actionCombo.getItems().stream().anyMatch(s -> s.startsWith("Set to")));

        // Cover/Blind case - ensure Open/Close are present
        Device cover = new Device("id4", "C1", "Cover/Blind", "R", true);
        deviceCombo.getItems().setAll(cover);
        deviceCombo.setValue(cover);
        controller.updateActionOptions(deviceCombo, actionCombo, null);
        assertTrue(actionCombo.getItems().contains("Open") && actionCombo.getItems().contains("Close"));

        // Unknown type -> default branch (no items expected)
        Device unknown = new Device("idX", "U1", "Mystery", "R", true);
        deviceCombo.getItems().setAll(unknown);
        deviceCombo.setValue(unknown);
        controller.updateActionOptions(deviceCombo, actionCombo, null);
        assertTrue(actionCombo.getItems().isEmpty());
    }
    /**
     * Überprüft, dass das Setzen eines konkreten Raums im Filter dazu führt,
     * dass nur Geräte dieses Raums angezeigt werden. Erwartung: keine Geräte
     * vorhanden -> Container leer.
     */
    @Test
    public void handleRoomFilterChange_applies_filter() throws Exception {
        // create rooms
        roomService.addRoom("Kitchen");
        roomService.addRoom("Bathroom");

        Object[] pair = loadControllerAndRoot();
        DevicesController controller = (DevicesController) pair[0];
        Parent root = (Parent) pair[1];

        javafx.scene.control.ComboBox<String> roomFilter = (javafx.scene.control.ComboBox<String>) root.lookup("#roomFilterCombo");
        VBox devicesContainer = (VBox) root.lookup("#devicesContainer");

        // set filter to Kitchen and invoke handler on FX thread and wait for FX tasks
        runAndWait(() -> {
            roomFilter.setValue("Kitchen");
            controller.handleRoomFilterChange();
            controller.loadDevices();
        });

        // ensure FX processed UI updates
        runAndWait(() -> { /* no-op to wait */ });
        assertNotNull(devicesContainer);
        assertTrue("Container sollte nach Filterung leer sein, da keine Geräte in Kitchen sind",
                devicesContainer.getChildren().isEmpty());
    }

    /**
     * Überprüft, dass das Löschen eines Geräts abgebrochen wird, wenn der
     * aktuelle Benutzer kein System-Manager (Owner) ist. Erwartung: Gerät bleibt
     * in der Datenbank erhalten.
     */
    @Test
    public void handleDeleteDevice_denied_for_non_owner() throws Exception {
        Room r = roomService.addRoom("Den");
        Device d = roomService.addDeviceToRoom(r.getId(), "Gadget", "Switch");
        assertNotNull(d);

        Object[] pair = loadControllerAndRoot();
        DevicesController controller = (DevicesController) pair[0];

        // ensure current user is not owner
        MockUserService.getInstance().logout();

        // calling handleDelete should return early and not remove device
        controller.handleDelete(d, r);
        // device should still be present
        assertNotNull(roomService.getDeviceById(d.getId()));
    }

    /**
     * Überprüft den End-to-End-Flow zum Anlegen eines Geräts über den
     * RoomService und die Anzeige im Controller. Erwartung: nach Anlage ist
     * eine Device-Card in der UI vorhanden.
     */
    @Test
    public void addDevice_flow_and_persistence() throws Exception {
        // create room and add device via real service
        Room r = roomService.addRoom("Living");
        Device d = roomService.addDeviceToRoom(r.getId(), "Lamp", "Switch");
        assertNotNull(d);

        // load controller (reads DB during initialize)
        Parent root = loadControllerRoot();
        VBox devicesContainer = (VBox) root.lookup("#devicesContainer");
        assertNotNull(devicesContainer);

        // devicesContainer should contain one card for the Lamp
        // wait for UI thread to apply created card(s)
        runAndWait(() -> { /* no-op, just wait for FX tasks */ });
        assertFalse(devicesContainer.getChildren().isEmpty());
    }

    /**
     * Überprüft Validierungs-Pfade beim Anlegen von Geräten über
     * {@code JdbcRoomService.addDeviceToRoom}: null/leer/ungültige Werte führen
     * zu {@code null} (kein Device erstellt).
     */
    @Test
    public void addDevice_invalid_inputs_are_rejected() {
        Room r = roomService.addRoom("Kitchen");
        // null name
        assertNull(roomService.addDeviceToRoom(r.getId(), null, "Switch"));
        // empty name
        assertNull(roomService.addDeviceToRoom(r.getId(), "   ", "Switch"));
        // null type
        assertNull(roomService.addDeviceToRoom(r.getId(), "Sensor1", null));
        // invalid type
        assertNull(roomService.addDeviceToRoom(r.getId(), "X", "UnknownType"));
    }

    /**
     * Testet, dass ohne Login die Add-Button-UI deaktiviert ist und nach
     * Login als Owner wieder aktiviert wird. Erwartung: Gast -> disabled,
     * Owner -> enabled.
     */
    @Test
    public void permission_and_button_state_owner_vs_guest() throws Exception {
        // without login, controller should disable addDeviceBtn
        Parent rootGuest = loadControllerRoot();
        javafx.scene.control.Button addBtnGuest = (javafx.scene.control.Button) rootGuest.lookup("#addDeviceBtn");
        assertNotNull(addBtnGuest);
        assertTrue(addBtnGuest.isDisable());

        // login as owner
        MockUserService.getInstance().login("owner@smarthome.com", "ownerpass");

        // load new controller instance after login
        Parent rootOwner = loadControllerRoot();
        javafx.scene.control.Button addBtnOwner = (javafx.scene.control.Button) rootOwner.lookup("#addDeviceBtn");
        assertNotNull(addBtnOwner);
        assertFalse(addBtnOwner.isDisable());
    }

    /**
     * Deckt Randfälle der Umbenennung und Löschung auf Service-Ebene ab:
     * leere Namen, nicht existierende IDs und erfolgreiche Entfernung.
     */
    @Test
    public void remove_and_rename_device_edge_cases() {
        Room r = roomService.addRoom("Office");
        Device d = roomService.addDeviceToRoom(r.getId(), "DeskLamp", "Switch");
        assertNotNull(d);

        // rename blank -> false
        assertFalse(roomService.renameDevice(r.getId(), d.getId(), "   "));
        // rename non-existing -> false
        assertFalse(roomService.renameDevice(r.getId(), "non-existent-id", "Name"));

        // remove non-existing -> false
        assertFalse(roomService.removeDeviceFromRoom(r.getId(), "no-id"));

        // remove existing -> true
        assertTrue(roomService.removeDeviceFromRoom(r.getId(), d.getId()));
    }

    /**
     * Stellt sicher, dass Daten persistiert werden und nach einem Service-
     * "Restart" (Singleton-Reset) erneut geladen werden können.
     */
    @Test
    public void persistence_across_service_restart() {
        Room r = roomService.addRoom("Garage");
        Device d = roomService.addDeviceToRoom(r.getId(), "Opener", "Cover/Blind");
        assertNotNull(d);

        // reset singleton and create new instance to simulate restart
        JdbcRoomService.resetForTesting();
        ServiceRegistry.resetRoomServiceForTesting();
        JdbcRoomService fresh = JdbcRoomService.getInstance();
        List<Room> rooms = fresh.getRooms();
        boolean found = rooms.stream().anyMatch(rr -> rr.getName().equals("Garage") && rr.getDevices().stream().anyMatch(dd -> dd.getName().equals("Opener")));
        assertTrue(found);
    }

    /**
     * Überprüft den Branch für die Auswahl "All Rooms" im Raumfilter. Erwartung:
     * Geräte aus allen Räumen werden angezeigt.
     */
    @Test
    public void handleRoomFilterChange_allRooms_shows_all() throws Exception {
        // create rooms and devices
        Room r1 = roomService.addRoom("Kitchen");
        Room r2 = roomService.addRoom("Bathroom");
        roomService.addDeviceToRoom(r1.getId(), "K-Lamp", "Switch");
        roomService.addDeviceToRoom(r2.getId(), "B-Sensor", "Sensor");

        Object[] pair = loadControllerAndRoot();
        DevicesController controller = (DevicesController) pair[0];
        Parent root = (Parent) pair[1];

        javafx.scene.control.ComboBox<String> roomFilter = (javafx.scene.control.ComboBox<String>) root.lookup("#roomFilterCombo");
        assertNotNull(roomFilter);

        // set filter to All Rooms and invoke handler on FX thread
        runAndWait(() -> {
            roomFilter.setValue("All Rooms");
            controller.handleRoomFilterChange();
        });
        VBox devicesContainer = (VBox) root.lookup("#devicesContainer");
        assertNotNull(devicesContainer);
        // should contain at least the two devices we created
        runAndWait(() -> { /* ensure FX work done */ });
        assertTrue(devicesContainer.getChildren().size() >= 2);
    }

    /**
     * Testet, dass eine {@code null}-Auswahl im Raumfilter sicher behandelt
     * wird und die Anzeige nicht fehlschlägt (zeigt alle Geräte).
     */
    @Test
    public void handleRoomFilterChange_nullSelection_isHandled() throws Exception {
        // create rooms and devices
        Room r1 = roomService.addRoom("A");
        roomService.addDeviceToRoom(r1.getId(), "A1", "Switch");

        Object[] pair = loadControllerAndRoot();
        DevicesController controller = (DevicesController) pair[0];
        Parent root = (Parent) pair[1];

        javafx.scene.control.ComboBox<String> roomFilter = (javafx.scene.control.ComboBox<String>) root.lookup("#roomFilterCombo");
        assertNotNull(roomFilter);

        // set filter to null and invoke handler on FX thread
        runAndWait(() -> {
            roomFilter.setValue(null);
            controller.handleRoomFilterChange();
        });

        // loadDevices should show all rooms (no exception) - at least one device should be present
        runAndWait(() -> controller.loadDevices());
        VBox devicesContainer = (VBox) root.lookup("#devicesContainer");
        assertNotNull(devicesContainer);
        assertTrue(devicesContainer.getChildren().size() >= 1);
    }

    /**
     * Überprüft das Verhalten beim Versuch, ein nicht vorhandenes Gerät zu
     * löschen. Erwartung: Methode liefert keine Ausnahme und der Service
     * liefert weiterhin {@code null} für die Phantom-ID.
     */
    @Test
    public void handleDelete_deviceNotFound_isHarmless() throws Exception {
        // create a room but do NOT create the device
        Room r = roomService.addRoom("Den2");

        Object[] pair = loadControllerAndRoot();
        DevicesController controller = (DevicesController) pair[0];

        // Construct a Device with a fantasy id that does not exist
        Device phantom = new Device("fantasy-id-xyz", "Ghost", "Switch", r.getId(), true);

        // calling handleDelete should not throw and should be handled gracefully
        controller.handleDelete(phantom, r);

        // ensure service still returns null for that id
        assertNull(roomService.getDeviceById("fantasy-id-xyz"));
    }

    /**
     * Deckt folgende Pfade in {@code updateActionOptions} ab:
     * - kein ausgewähltes Gerät (selectedDevice == null) -> keine Aktionen
     * - preferredAction nicht in Liste -> default wird gesetzt
     */
    @Test
    public void updateActionOptions_emptyDeviceCombo_and_preferredAbsent() throws Exception {
        Object[] pair = loadControllerAndRoot();
        DevicesController controller = (DevicesController) pair[0];

        javafx.scene.control.ComboBox<Device> deviceCombo = new javafx.scene.control.ComboBox<>();
        javafx.scene.control.ComboBox<String> actionCombo = new javafx.scene.control.ComboBox<>();

        // no selected device -> should return quickly and leave action list empty
        deviceCombo.setValue(null);
        controller.updateActionOptions(deviceCombo, actionCombo, null);
        assertTrue(actionCombo.getItems().isEmpty());

        // switch back to a valid device and prefer an absent action
        Device switchDevice = new Device("idA2", "S-B", "Switch", "R", true);
        deviceCombo.getItems().add(switchDevice);
        deviceCombo.setValue(switchDevice);
        controller.updateActionOptions(deviceCombo, actionCombo, "Tanz für mich");
        // preferred absent -> not selected, but default value should be set to first item
        assertNotEquals("Tanz für mich", actionCombo.getValue());
        assertTrue(actionCombo.getItems().contains("Turn On"));
    }

    /**
     * Überprüft, dass {@code handleRename} bei einem nicht berechtigten
     * Benutzer frühzeitig abbricht und das Label unverändert bleibt.
     */
    @Test
    public void handleRename_denied_for_non_owner_doesNothing() throws Exception {
        Room r = roomService.addRoom("SrvOffice");
        Device d = roomService.addDeviceToRoom(r.getId(), "ServerLamp", "Switch");
        assertNotNull(d);

        Object[] pair = loadControllerAndRoot();
        DevicesController controller = (DevicesController) pair[0];

        // ensure current user is not owner
        MockUserService.getInstance().logout();

        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(d.getName());
        controller.handleRename(d, r, nameLabel);
        // nameLabel should remain unchanged because rename should be denied early
        assertEquals(d.getName(), nameLabel.getText());
    }

    /**
     * Prüft die Logik zum Setzen einer bevorzugten Aktion: wenn bevorzugte
     * Aktion nicht vorhanden ist, wird sie nicht gesetzt; wenn vorhanden,
     * wird sie ausgewählt.
     */
    @Test
    public void updateActionOptions_preferredAction_present_and_absent() throws Exception {
        Object[] pair = loadControllerAndRoot();
        DevicesController controller = (DevicesController) pair[0];

        javafx.scene.control.ComboBox<Device> deviceCombo = new javafx.scene.control.ComboBox<>();
        javafx.scene.control.ComboBox<String> actionCombo = new javafx.scene.control.ComboBox<>();

        Device switchDevice = new Device("idA", "S-A", "Switch", "R", true);
        deviceCombo.getItems().add(switchDevice);
        deviceCombo.setValue(switchDevice);

        // preferred action not in list
        controller.updateActionOptions(deviceCombo, actionCombo, "Tanz für mich");
        // preferred should not be selected
        assertNotEquals("Tanz für mich", actionCombo.getValue());

        // preferred action that exists
        controller.updateActionOptions(deviceCombo, actionCombo, "Turn On");
        assertEquals("Turn On", actionCombo.getValue());
    }
}







