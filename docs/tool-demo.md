# Tool - XY

## 1. Einleitung *(~1 min)*

### Was ist das Tool und wofür wird es eingesetzt?

### Warum ist es für unser Projekt relevant?

## 2. Kernkonzepte *(~2 min)*

### Wichtigste Begriffe und Konzepte

### Architektur / Funktionsweise auf hohem Niveau

## 4. Live-Demo *(~4 min)*

### Typischer Workflow Schritt für Schritt

### Häufig verwendete Befehle / Funktionen

### Integration in das Projekt

## 5. Vor- und Nachteile *(~1 min)*

### Stärken des Tools

### Bekannte Einschränkungen oder Alternativen

## 6. Zusammenfassung & Fragen *(~1 min)*

### Die drei wichtigsten Takeaways

### Weiterführende Ressourcen / Dokumentation

# Tool - Visual Paradigm

## 1. Einleitung *(~1 min)*

### Was ist das Tool und wofür wird es eingesetzt?
Visual Paradigm ist eine umfassende Modellierungsplattform für Software-Engineering. Es wird primär eingesetzt, um UML-Diagramme (Unified Modeling Language) wie Klassendiagramme, Sequenzdiagramme oder Use-Case-Diagramme zu erstellen. Im Gegensatz zu reinen Code-to-Diagram-Tools ermöglicht es ein visuelles, drag-and-drop-basiertes Design, das sowohl vor der Implementierung (Design-First) als auch zur Dokumentation bestehender Systeme genutzt wird.

### Warum ist es für unser Projekt relevant?
Für unser Projekt ist Visual Paradigm relevant, da die **Community Edition** kostenlos für nicht-kommerzielle Zwecke nutzbar ist (erfüllt die Anforderung "Demo/Free" dauerhaft).

## 2. Kernkonzepte *(~2 min)*

### Wichtigste Begriffe und Konzepte
*   **Modell-Getrieben (Model-Driven):** Das Diagramm ist nicht nur eine Zeichnung, sondern ein echtes Modell. Änderungen im Modell können teilweise in Code synchronisiert werden (Round-Trip Engineering).
*   **VP AI:** Eine integrierte KI-Komponente, die natürliche Sprachbefehle (z. B. "Erstelle eine Klasse 'User' mit Attributen 'name' und 'email'") in UML-Elemente umwandelt.
*   **XMI-Export:** Standardisiertes Austauschformat, um Modelle mit anderen Tools (z. B. Eclipse Papyrus) kompatibel zu halten.

### Architektur / Funktionsweise auf hohem Niveau
Visual Paradigm ist eine Desktop-Anwendung (Java-basiert), die lokal läuft.
1.  **Projektstruktur:** Organisiert Diagramme in logischen Ordnern.
2.  **Repository:** Speichert Modelle in einem proprietären Format (`.vpp`), verwaltet aber Beziehungen zwischen Elementen streng nach UML-Spezifikation.
3.  **Integration:** Besitzt Plugins für VS Code, IntelliJ und Eclipse, um vom Code ins Modell und zurück zu navigieren.
4.  **KI-Layer:** Sendet Prompt-Anfragen an einen Cloud-Service (optional), um Modellvorschläge zu generieren, die lokal verfeinert werden. (nicht in Community-Edition)

## 4. Live-Demo *(~4 min)*

### Typischer Workflow Schritt für Schritt
1.  **Projektinitialisierung:** Starten der Community Edition und Anlegen eines neuen Projekts ("Software Development").
2.  **Diagrammerstellung:** Auswahl "Create Diagram" -> "Class Diagram".
3.  **Klassen und Assoziationen:** Klassen und deren Assoziationen über das Tool hinzufügen. 
4.  **Verfeinerung:** Manuelles Hinzufügen von Sichtbarkeiten (public/private) und Methoden.
5.  **Code-Generierung:** Rechtsklick auf das Paket -> "Generate Code" -> Wahl von Java oder Python. (nicht in Community-Edition)

## 5. Vor- und Nachteile *(~1 min)*

### Vorteile
1.	Bietet eine große Vielfalt verschiedener Diagrammtypen
2.	Einfache Modellierung durch visuelle Bedienung Leicht zu erlernen

### Nachteile
1.	Steilere Lernkurve bei den zusätzlichen Features
2.	Viele Features (z.B. Code-Generierung, KI-Unterstützung) nicht verfügbar in der Community Edition 
3.	Versionsverwaltung ist bei code-basierten Tools einfacher

## 6. Zusammenfassung & Fragen *(~1 min)*

### Die drei wichtigsten Takeaways
1.  **Visual Paradigm Community Edition** ist eine stabile kostenlose Option für visuelles UML-Modeling ohne Ablaufdatum (im Gegensatz zu Trials).
2.  **Versionsverwaltung** ist schwieriger als bei code-basierten Diagrammtools.
3.  **Einschränkungen der Community Edition**: viele Features wie Code-Generierung und KI-Untersstützung nicht in Community-Edition verfügbar.

### Weiterführende Ressourcen / Dokumentation
*   **Offizielle Docs:** [visual-paradigm.com/guide](https://www.visual-paradigm.com/guide/)
*   **UML Spezifikation:** [omg.org/spec/UML](https://www.omg.org/spec/UML/)
```
