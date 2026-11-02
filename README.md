# Team 3658 BIOCORE 2027

Java robot project for Team 3658's 2027 FIRST Systemcore robot.

## Current status

This repository is a **pre-season, compile-target scaffold**, not deployable robot code. As of
November 1, 2026:

- WPILib 2027 is still alpha software. This project is pinned to the locally verified
  `2027.0.0-alpha-7` toolchain; WPILib Alpha 8 is newer and should be evaluated as a coordinated
  upgrade.
- Phoenix 6 is pinned to `26.70.0-alpha-2`, the release documented for WPILib Alpha 7.
- PhotonLib is pinned to a development build for `2027_alpha7` because no stable compatible
  PhotonLib release is available.
- The 2027 game, official field AprilTag layout, final robot dimensions, CANcoder offsets, and
  intake/indexer/scoring details are not yet known.

The drivetrain can run in desktop simulation once dependencies resolve. Real-hardware startup is
blocked in code while `HARDWARE_CONFIGURATION_VERIFIED` is false. Do **not** remove that guard or
deploy until every item in [Robot Commissioning](docs/Robot-Commissioning.md) is complete and the
provisional Tuner constants have been replaced with output generated from the assembled robot.

## Architecture

| Area | Hardware / assignment | Status |
| --- | --- | --- |
| Robot controller | FIRST Systemcore | Alpha toolchain |
| Drivetrain | SDS MK5i L2; Kraken X60 drive; Kraken X44 steer | Provisional constants |
| Swerve sensors | Pigeon2 + four CANcoders | `CAN_S0` |
| Intake | Dedicated mechanism bus | Reserved on `CAN_S1`; unfinished |
| Indexer | Dedicated mechanism bus | Reserved on `CAN_S2`; unfinished |
| Scoring | Future mechanism | Reserved on `CAN_S3` |
| Auxiliary | Future devices | Reserved on `CAN_S4` |
| Driver controls | Xbox controller, USB 0 | Drive bindings present |
| Operator controls | Xbox controller, USB 1 | Reserved; no mechanism bindings |
| Vision processor A | Orange Pi 5, front + rear cameras | Names configured |
| Vision processor B | Orange Pi 5, left + right cameras | Names configured |

PhotonVision camera names are `3658-front`, `3658-rear`, `3658-left`, and `3658-right`.

## Build

Install the matching 2027 WPILib alpha toolchain, then run:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

The checked-in wrapper and vendor manifests make CI repeatable, subject to the availability of the
alpha Maven artifacts. A successful desktop build proves source/API compatibility only. It does not
verify Systemcore deployment, CAN wiring, firmware, directions, calibration, or mechanism safety.

## Project map

- `RobotCAN.java` assigns Systemcore CAN ports.
- `generated/TunerConstants.java` is a deliberately provisional Phoenix configuration.
- `CommandSwerveDrivetrain.java` is the Phoenix command wrapper.
- `VisionSubsystem.java` opens all four PhotonVision clients but does not fuse poses yet.
- `IntakeSubsystem.java` and `IndexerSubsystem.java` define ownership boundaries without guessing
  unselected hardware.
- `docs/` contains wiring, vision, commissioning, and software compatibility notes.

## Required next steps

1. Upgrade WPILib, Phoenix, PhotonVision, firmware, and the Systemcore image as one tested set.
2. Assemble and inventory the CAN devices, then confirm the proposed IDs in Phoenix Tuner X.
3. Generate swerve constants from the physical robot; replace the provisional file.
4. Measure wheelbase, trackwidth, effective wheel radius, camera transforms, and CANcoder offsets.
5. Select and implement intake/indexer/scoring hardware with limits and interlocks.
6. Load the official 2027 AprilTag layout before enabling vision pose fusion.
7. Complete disabled-on-blocks tests, then controlled floor tests.

See [Software Compatibility](docs/Software-Compatibility.md) for the sources behind the pinned
versions.
