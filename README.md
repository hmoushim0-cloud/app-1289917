# تطبيق ركاب لتأجير السيارات - إدارة الأسطول والمفتاح الرقمي (`rekab-telematics-car-rental-1289917`)
> منظومة car_rental المتكاملة مع ربط OBD-II CAN-Bus Telematics + BLE Digital Key Fob

- **Project ID**: `#1289917`
- **Commercial Archetype**: `car_rental`
- **Bound Hardware Peripheral**: `OBD-II CAN-Bus Telematics + BLE Digital Key Fob`
- **Package Name**: `sa.factory.rekabtelematicscar`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

## [Sidekick Autonomous Update]
- **Hardware Integration**:
  - `ObdCanBusProtocolEngine`: Full SAE J1979 OBD-II standard PID telemetry decoder (Engine RPM PID 0x0C, Vehicle Speed PID 0x0D, Coolant Temp PID 0x05, Fuel Level PID 0x2F, Ambient Temp PID 0x46), Diagnostic Trouble Code (DTC) evaluation, bus load tracking, and CRC-16 frame integrity check.
  - `AutonomousDigitalKeyLeaseEngine`: Fleet vehicle inventory (Lucid Air, Genesis G90, Toyota Land Cruiser 300, Mercedes S-Class), BLE keyless unlock proximity verification (threshold >= -70 dBm), Riyadh metropolitan geofence boundary validation, dynamic speed limit enforcement (140 km/h), and automated lease contract billing.
- **Hardware Driver**:
  - `HardwareSensorDriver`: Reactive Kotlin Coroutines Flow streaming live vehicle speed, engine RPM, fuel percentage, digital key state, and fleet occupancy rate.
- **Quality & Governance**:
  - 10 comprehensive unit regression tests covering OBD PID formulas, CRC-16 integrity, DTC health status, geofencing, keyless entry, lease settlement, and driver streams (100% assertions green).
  - 100% Zero-Emoji compliance in code, manifests, and UI strings.
