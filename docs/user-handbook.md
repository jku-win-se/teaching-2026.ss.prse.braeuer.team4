# Benutzerhandbuch - SmartHome Orchestrator

Dieses Handbuch beschreibt die Bedienung der JavaFX-Anwendung aus Anwendersicht.
Alle Schritte sind auf die aktuelle UI abgestimmt (Button-Namen wie in der App).

## Inhaltsverzeichnis

1. [Zielgruppe](#1-zielgruppe)
2. [Systemanforderungen und Start](#2-systemanforderungen-und-start)
3. [Registrierung und Login](#3-registrierung-und-login)
4. [Benutzerrollen](#4-benutzerrollen)
5. [Navigation und Grundaufbau](#5-navigation-und-grundaufbau)
6. [Rooms: Räume verwalten](#6-rooms-raume-verwalten)
7. [Devices: Geräte steuern und filtern](#7-devices-gerate-steuern-und-filtern)
8. [Rules: Automationsregeln](#8-rules-automationsregeln)
9. [Schedules: Zeitpläne](#9-schedules-zeitplane)
10. [Vacation Mode](#10-vacation-mode)
11. [Simulation](#11-simulation)
12. [Scenes](#12-scenes)
13. [Energy](#13-energy)
14. [Activity Log](#14-activity-log)
15. [Users](#15-users)
16. [IoT Integration](#16-iot-integration)
17. [Preferences](#17-preferences)
18. [Benachrichtigungen und Konflikthinweise](#18-benachrichtigungen-und-konflikthinweise)
19. [Schnelle Praxis-Szenarien](#19-schnelle-praxis-szenarien)
20. [Bekannte Einschränkungen](#20-bekannte-einschrankungen)
21. [FAQ](#21-faq)

## 1. Zielgruppe

Dieses Handbuch richtet sich an die zwei realen Nutzerrollen der Anwendung:

- **Owner**: verwaltet das gesamte SmartHome (Räume, Geräte, Regeln, Zeitpläne, User, Vacation Mode, Simulation usw.).
- **Member**: bedient vorhandene Geräte, hat aber keine Verwaltungsrechte.

## 2. Systemanforderungen und Start

### Voraussetzungen

- Java 21
- Maven 3.9+
- Optional: JDBC-Datenbankkonfiguration (wenn nicht im Mock-Modus gearbeitet wird)

### Anwendung starten

Im Projektordner:

```powershell
mvn clean package
mvn javafx:run
```

Danach öffnet sich die Login-Ansicht.

## 3. Registrierung und Login

### Registrierung

1. In der Login-Ansicht auf **Register** klicken.
2. E-Mail, Benutzername und Passwort eingeben.
3. Registrierung bestätigen.
4. Bei Erfolg kannst du dich direkt einloggen.

Hinweis: Neue Konten werden als **Member** erstellt.

### Login

1. E-Mail und Passwort eingeben.
2. Auf **Login** klicken.
3. Nach erfolgreichem Login wird die Hauptansicht geladen.

### Logout

1. Oben rechts auf **Logout** klicken.
2. Die Session wird beendet und die Login-Seite angezeigt.

## 4. Benutzerrollen

| Funktion | Owner | Member |
|---|---|---|
| Räume anlegen/umbenennen/löschen | ja | nein |
| Geräte anlegen/umbenennen/löschen | ja | nein |
| Geräte bedienen | ja | ja |
| Rules verwalten | ja | nein |
| Schedules verwalten | ja | nein |
| Scenes verwalten/aktivieren | ja | nein |
| Vacation Mode | ja | nein |
| Simulation | ja | nein |
| CSV-Export in Energy/Activity | ja | nein |
| Users verwalten | ja | nein |
| IoT Integration | ja | nein |

Owner-only Menüpunkte sind für Member ausgeblendet.

## 5. Navigation und Grundaufbau

### Linke Navigation

In der Sidebar findest du:

- `Rooms`
- `Devices`
- `Schedules`
- `Rules`
- `Scenes`
- `Energy`
- `Activity Log`
- `Users`
- `Vacation Mode`
- `Simulation`
- `IoT Integration`
- `Preferences`

### Obere Leiste

- **🔔**: Benachrichtigungen öffnen
- **Welcome ... (Role)**: aktueller User + Rolle
- **Logout**: abmelden

## 6. Rooms: Räume verwalten

🔒 Nur Owner.

### Raum hinzufügen

1. `Rooms` öffnen.
2. Auf **+ Add Room** klicken.
3. Raumname eingeben.
4. Mit **OK** bestätigen.

### Raum umbenennen

1. In der Tabelle den gewünschten Raum suchen.
2. In der Spalte **Actions** auf **Rename** klicken.
3. Neuen Namen eingeben.
4. Mit **OK** bestätigen.

### Raum löschen

1. In der Tabelle beim Raum auf **Delete** klicken.
2. Bestätigungsdialog lesen.
3. Mit **OK** bestätigen.

Hinweis: Beim Löschen eines Raums werden zugehörige Geräte mit entfernt.

### Gerät direkt im ausgewählten Raum anlegen

1. In der oberen Tabelle einen Raum anklicken.
2. Unten wird `Selected Room: ...` angezeigt.
3. Auf **+ Add Device** klicken.
4. Device-Name eingeben.
5. Device-Typ auswählen (`Switch`, `Dimmer`, `Thermostat`, `Sensor`, `Cover/Blind`).
6. Dialog bestätigen.

### Gerät im ausgewählten Raum umbenennen/löschen

1. In der unteren Device-Tabelle beim Gerät **Rename** oder **Delete** klicken.
2. Eingabe bzw. Bestätigung durchführen.

## 7. Devices: Geräte steuern und filtern

### Geräte nach Raum filtern

1. `Devices` öffnen.
2. Bei **Filter by Room:** einen Raum auswählen.
3. Es werden nur Geräte aus diesem Raum angezeigt.

### Filter zurücksetzen

1. Auf **Clear Filter** klicken.
2. Die Gesamtansicht über alle Räume wird wieder angezeigt.

### Gerät hinzufügen (globale Device-Ansicht)

🔒 Nur Owner.

1. In `Devices` auf **Add Device** klicken.
2. Im Dialog **Room** auswählen.
3. **Name** eingeben.
4. **Type** auswählen.
5. Mit **Save** speichern.

### Geräte bedienen (Owner und Member)

- **Switch**: Toggle auf `ON/OFF` setzen.
- **Dimmer**:
  1. Helligkeit über Slider wählen.
  2. Optional mit ON/OFF-Toggle aktivieren/deaktivieren.
- **Thermostat**: Temperatur mit `+` und `-` anpassen.
- **Sensor**:
  1. Wert im Feld eingeben.
  2. Auf **Inject** klicken.
- **Cover/Blind**: Mit **Open** oder **Close** steuern.

### Gerät umbenennen/löschen in Device-Karten

🔒 Nur Owner.

1. Auf der Device-Karte **Rename** oder **Delete** klicken.
2. Dialog bestätigen.

## 8. Rules: Automationsregeln

🔒 Nur Owner.

### Regel anlegen

1. `Rules` öffnen.
2. **+ Add Rule** klicken.
3. Name vergeben.
4. Trigger-Typ wählen (`Time`, `Sensor Threshold`, `Device State`).
5. Quelle/Bedingung setzen.
6. Zielgerät + Aktion setzen.
7. Mit **Save** speichern.

### Regel manuell ausführen

1. In der Rule-Zeile auf **Run** klicken.
2. Aktion wird sofort ausgelöst.

### Regel bearbeiten

1. In der Rule-Zeile auf **Edit** klicken.
2. Werte anpassen.
3. Speichern.

### Regel löschen

1. In der Rule-Zeile auf **Delete** klicken.
2. Löschung bestätigen.

### Regel aktiv/inaktiv setzen

1. In der Spalte **Status** den Zustand umstellen.
2. Nur aktive Regeln werden automatisch ausgewertet.

## 9. Schedules: Zeitpläne

🔒 Nur Owner.

### Schedule erstellen

1. `Schedules` öffnen.
2. **+ Add Schedule** klicken.
3. Name, Zielgerät, Aktion, Zeit und Wiederholung setzen.
4. Mit **Save** speichern.

### Schedule bearbeiten

1. In der Zeile auf **Edit** klicken.
2. Werte anpassen.
3. Speichern.

### Schedule löschen

1. In der Zeile auf **Delete** klicken.
2. Löschung bestätigen.

### Aktiv-Status umschalten

1. In der Spalte **Active** den Status ändern.
2. Nur aktive Schedules werden ausgeführt.

### Konflikthinweis lesen

- Über der Tabelle kann ein Hinweis bei möglichen Konflikten erscheinen.
- Hinweis prüfen und Zeitplan/Regeln bei Bedarf anpassen.

## 10. Vacation Mode

🔒 Nur Owner.

### Vacation Mode aktivieren

1. `Vacation Mode` öffnen.
2. **Start Date** wählen.
3. **End Date** wählen.
4. **Start Hour** und **End Hour** wählen.
5. Bei **Schedule to Apply** einen Schedule auswählen.
6. Auf **Activate Vacation Mode** klicken.

### Vacation Mode deaktivieren

1. In derselben Ansicht auf **Deactivate** klicken.
2. Danach laufen normale Schedules wieder.

### Status prüfen

- **Current Vacation Override** zeigt aktuellen Status und aktive Konfiguration.
- **Affected Devices** zeigt die betroffenen Geräte.

## 11. Simulation

🔒 Nur Owner.

### Simulation konfigurieren

1. `Simulation` öffnen.
2. Bei **Start Time** eine Zeit eintragen (Format `HH:MM:SS`, z. B. `00:00:00`).
3. Bei **Speed** eine Geschwindigkeit wählen (`x10`, `x100`, `x300`).
4. Anfangswerte für Sensoren eintragen (z. B. Temperatur, Feuchte).
5. In **Active Rules** gewünschte Regeln aktiv lassen/deaktivieren.

### Simulation starten

1. Auf **Start Simulation** klicken.
2. Die Timeline läuft hoch.
3. Tabelle **Simulated Device States** und **Simulation Replay Log** aktualisieren sich live.

### Simulation pausieren

1. Auf **Pause** klicken.
2. Der Simulationszeitpunkt friert ein.
3. Zum Fortsetzen erneut **Pause** verwenden (Toggle-Verhalten je nach Zustand).

### Simulation neu starten

1. Auf **Restart Simulation** klicken.
2. Lauf startet wieder von der Anfangskonfiguration.

## 12. Scenes

🔒 Nur Owner.

### Neue Szene erstellen

1. `Scenes` öffnen.
2. Auf **+ Create New Scene** klicken.
3. Szenenname eingeben.
4. Optional Icon/Farbe wählen.
5. Mit **+ Add Device State** Geräteaktionen hinzufügen.
6. Mit **Save** speichern.

### Szene ausführen

1. In der Szenenkarte auf **Activate** klicken.
2. Alle konfigurierten Zustände werden angewendet.

### Szene bearbeiten

1. In der Karte auf **Edit** klicken.
2. Device-States anpassen (optional mit **Remove** einzelne States entfernen).
3. Speichern.

### Szene löschen

1. In der Karte auf **Delete** klicken.
2. Löschung bestätigen.

## 13. Energy

### Aggregation umschalten

1. `Energy` öffnen.
2. Oben bei den Toggles zwischen **Day** und **Week** wechseln.
3. Die Kennzahlen und Diagramme aktualisieren sich entsprechend.

### Werte lesen

- **Household Total**: Gesamtverbrauch.
- **Highest Device Consumption**: Gerät mit höchstem Verbrauch.
- **Highest Room Consumption**: Raum mit höchstem Verbrauch.
- **Consumption by Room** und **Top Devices** visualisieren die Verteilung.

### CSV exportieren

🔒 Nur Owner (Button für Member ausgeblendet).

1. Auf **Export CSV** klicken.
2. Speicherort im Dateidialog wählen.
3. Datei speichern.

## 14. Activity Log

### Log filtern

1. `Activity Log` öffnen.
2. Bei **From** Startdatum wählen.
3. Bei **To** Enddatum wählen.
4. Bei **Device** optional ein einzelnes Gerät auswählen.
5. Die Tabelle zeigt dann genau die gefilterten Einträge.

### CSV exportieren

🔒 Nur Owner (Button für Member ausgeblendet).

1. Gewünschte Filter setzen.
2. Auf **Export CSV** klicken.
3. Zielpfad im Save-Dialog wählen.
4. Speichern.

Hinweis: Exportiert wird genau die aktuell sichtbare, gefilterte Tabelle.

## 15. Users

🔒 Nur Owner.

### Member einladen

1. `Users` öffnen.
2. Auf **+ Invite Member** klicken.
3. E-Mail eingeben.
4. Einladung bestätigen.

### Benutzerliste filtern

1. Bei **Status Filter** einen Status wählen.
2. Die Tabelle wird entsprechend gefiltert.

### Zugriff entziehen

1. In der User-Zeile auf **Revoke Access** klicken.
2. Zugriff wird deaktiviert.

### Zugriff wiederherstellen

1. In der User-Zeile auf **Restore Access** klicken.
2. Zugriff wird wieder aktiviert.

## 16. IoT Integration

🔒 Nur Owner.

### Integration aktivieren/deaktivieren

1. `IoT Integration` öffnen.
2. Toggle **OFF/ON** bei **Enable MQTT Integration** umlegen.
3. Statusanzeige prüfen.

### Verbindung konfigurieren

1. **Broker URL** eintragen.
2. **Port** eintragen.
3. Optional **Username** und **Password** ausfüllen.
4. Auf **Save Settings** klicken.

### Verbindung testen

1. Auf **Test Connection** klicken.
2. Ergebnis unterhalb als Statusmeldung prüfen.

### Geräte suchen

1. Auf **Discover Devices** klicken.
2. Gefundene Einträge erscheinen in **Discovered Physical Devices**.

## 17. Preferences

### Profil aktualisieren

1. `Preferences` öffnen.
2. Bei **New Password** neues Passwort eingeben.
3. Bei **Confirm Password** wiederholen.
4. Auf **Update Profile** klicken.

### Daten exportieren

1. In **Data Management** auf **Export All Data** klicken.
2. Exportablauf bestätigen (je nach Dialog).

## 18. Benachrichtigungen und Konflikthinweise

### Benachrichtigungen öffnen

1. Oben rechts auf **🔔** klicken.
2. Popup-Liste mit Einträgen erscheint.

### Einzelne Benachrichtigung als gelesen markieren

1. Beim Eintrag auf **✓** klicken.

### Alle als gelesen markieren

1. Im Popup auf **Mark all read** klicken.

### Konflikthinweise

- Beim Erstellen/Ändern von Rules und Schedules können Warnungen erscheinen.
- Diese Hinweise zeigen mögliche widersprüchliche Automationen.

## 19. Schnelle Praxis-Szenarien

### Szenario A: Neues Gerät komplett anlegen

1. Als Owner einloggen.
2. `Rooms` öffnen und auf **+ Add Room** klicken.
3. Raumname eingeben, **OK**.
4. `Devices` öffnen und **Add Device** klicken.
5. Room auswählen, Name eintragen, Type wählen, **Save**.
6. In `Devices` das Gerät direkt testen.

### Szenario B: Nur Geräte eines Raums anzeigen

1. `Devices` öffnen.
2. Bei **Filter by Room** den Raum wählen.
3. Nur dessen Geräte werden angezeigt.
4. Mit **Clear Filter** zurück zur Gesamtansicht.

### Szenario C: Activity Log für Zeitraum exportieren

1. `Activity Log` öffnen.
2. **From** und **To** setzen.
3. Optional **Device** wählen.
4. Auf **Export CSV** klicken und speichern.

## 20. Bekannte Einschränkungen

- Energie-Werte sind modellbasierte Schätzungen (keine physische Live-Messung).
- Simulation ist isoliert und schreibt nicht ins Live-Activity-Log.
- Einige Funktionen sind absichtlich nur für Owner sichtbar.
- Verhalten kann je nach Datenquelle (Mock/JDBC) leicht variieren.

## 21. FAQ

**Ich sehe manche Menüpunkte nicht. Warum?**  
Du bist wahrscheinlich als Member eingeloggt. Owner-only Bereiche sind ausgeblendet.

**Wie komme ich schnell zurück auf alle Geräte?**  
Im Bereich `Devices` auf **Clear Filter** klicken.

**Was exportiert der Activity-CSV-Export genau?**  
Genau die aktuell sichtbaren, gefilterten Tabellenzeilen.

**Warum sehe ich keinen Export-Button in Energy/Activity?**  
CSV-Export ist Owner-only. Für Member ist der Button unsichtbar.

**Was ist der schnellste Startbefehl lokal?**  
`mvn clean package` und danach `mvn javafx:run`.
