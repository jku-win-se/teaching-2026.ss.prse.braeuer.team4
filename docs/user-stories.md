# SmartHome Orchestrator — Grouped User Stories

This document presents the user stories derived from the functional requirements of the **SmartHome Orchestrator** system.

The stories are grouped into five functional categories to improve readability, backlog planning, and architectural alignment.  
The **Related FR** column ensures traceability to the requirements document.

---

## 1. Identity & Access Control

| Story ID | Related FR | User Story |
|---|---|---|
| US-01 | FR-01 | As a **new user**, I want to **register with a unique email address and password**, so that **I can create my own account**. |
| US-02 | FR-02 | As a **registered user**, I want to **log in securely**, so that **I can access my smart home environment**. |
| US-03 | FR-02 | As a **logged-in user**, I want to **log out securely**, so that **my session ends safely**. |
| US-23 | FR-13 | As an **Owner**, I want to **have full access to device, rule, room, and user management**, so that **I can fully configure the system**. |
| US-24 | FR-13 | As a **Member**, I want to **control devices without being able to change device or rule configuration**, so that **I can use the smart home safely within my permissions**. |
| US-33 | FR-20 | As an **Owner**, I want to **invite additional Members by email address**, so that **other people can access the smart home system**. |
| US-34 | FR-20 | As an **Owner**, I want to **revoke a Member’s access at any time**, so that **unauthorized users can no longer use the system**. |

---

## 2. Structure / Resource Management

| Story ID | Related FR | User Story |
|---|---|---|
| US-04 | FR-03 | As an **authenticated user**, I want to **create rooms**, so that **my devices can be organized by location**. |
| US-05 | FR-03 | As an **authenticated user**, I want to **rename rooms**, so that **the system matches my actual home layout**. |
| US-06 | FR-03 | As an **authenticated user**, I want to **delete rooms**, so that **unused rooms are removed from the system**. |
| US-07 | FR-04 | As a **user**, I want to **add a virtual smart device to a room**, so that **I can model and manage my smart home setup**. |
| US-08 | FR-04 | As a **user**, I want to **define a device’s type and name when adding it**, so that **the system can represent the device correctly and I can identify it easily**. |
| US-09 | FR-05 | As a **user**, I want to **rename an existing device**, so that **it is easier to recognize in the interface**. |
| US-10 | FR-05 | As a **user**, I want to **remove an existing device**, so that **obsolete devices no longer appear in the system**. |
| US-29 | FR-17 | As a **user**, I want to **create named scenes with predefined device states**, so that **I can prepare recurring smart home situations such as “Movie Night” or “Away”**. |
| US-30 | FR-17 | As a **user**, I want to **activate a scene with a single action**, so that **multiple devices change to the desired states at once**. |

---

## 3. Logging & Notification

| Story ID | Related FR | User Story |
|---|---|---|
| US-17 | FR-08 | As a **user**, I want **every manual or automated device state change to be recorded with timestamp, device, and actor**, so that **I can review what happened in the system later**. |
| US-18 | FR-08 | As a **user**, I want to **view an activity log**, so that **I can understand past actions and device changes**. |
| US-22 | FR-12 | As a **user**, I want to **receive in-app notifications when a rule is executed or when its execution fails**, so that **I know whether the automation worked as intended**. |
| US-28 | FR-16 | As a **user**, I want to **export the activity log and energy usage summary as a CSV file**, so that **I can analyze the data outside the system**. |

---

## 4. Device Operation & State Monitoring

| Story ID | Related FR | User Story |
|---|---|---|
| US-11 | FR-06 | As a **user**, I want to **turn switch devices on and off manually**, so that **I can control connected appliances directly**. |
| US-12 | FR-06 | As a **user**, I want to **set dimmer brightness between 0% and 100%**, so that **I can adjust lighting intensity manually**. |
| US-13 | FR-06 | As a **user**, I want to **set a thermostat target temperature**, so that **I can control room climate manually**. |
| US-14 | FR-06 | As a **user**, I want to **open and close covers or blinds**, so that **I can control privacy and incoming light**. |
| US-15 | FR-06 | As a **user**, I want to **set or inject the current value of a sensor for testing purposes**, so that **I can try out automation scenarios in the simulated environment**. |
| US-16 | FR-07 | As a **user**, I want to **see the current state of each device in real time in the UI**, so that **I always know the current status of my smart home**. |
| US-25 | FR-14 | As a **user**, I want to **view an energy usage dashboard**, so that **I can monitor estimated power consumption in my smart home**. |
| US-26 | FR-14 | As a **user**, I want to **see estimated energy usage per device, per room, and for the whole household, aggregated by day and by week**, so that **I can identify patterns and high energy consumption areas**. |
| US-31 | FR-18 | As a **user**, I want to **connect supported physical smart devices through the system’s integration layer**, so that **I can manage real devices in addition to simulated ones**. |

---

## 5. Rules & Scheduling

| Story ID | Related FR | User Story |
|---|---|---|
| US-19 | FR-09 | As a **user**, I want to **configure recurring time-based schedules for device actions**, so that **routine actions happen automatically at defined times**. |
| US-20 | FR-10 | As a **user**, I want to **create condition-action rules of the form IF trigger THEN action**, so that **devices react automatically to relevant events or conditions**. |
| US-21 | FR-11 | As a **user**, I want to **use time-based, threshold-based, and event-based triggers in rules**, so that **I can build flexible automation scenarios**. |
| US-27 | FR-15 | As a **user**, I want the system to **detect and warn me about scheduling conflicts that would put the same device into contradictory states at the same time**, so that **I can correct invalid automation setups before they cause problems**. |
| US-32 | FR-19 | As a **user**, I want to **configure and run a full-day simulation with defined start conditions, active rules, and accelerated replay of resulting device state changes**, so that **I can test my automation safely without affecting the live system**. |
| US-35 | FR-21 | As a **user**, I want to **enable a vacation mode that applies a named schedule for a specified date range and overrides normal daily schedules during that period**, so that **my home follows a different automated routine while I am away**. |

---
