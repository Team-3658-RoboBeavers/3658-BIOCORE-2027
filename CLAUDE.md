# Team 3658 BIOCORE 2027 — instructions for AI coding agents

Robot code for FIRST Systemcore. Student programmers maintain this code, so keep changes small,
readable, and explained. See README.md and docs/ for full detail; this file is the short version.

## Stack (check build.gradle and vendordeps/ before relying on this)

- Java 25, WPILib / GradleRIO **2027.0.0-alpha-8**, Gradle wrapper 9.4.1.
- **Commands v3** (`org.wpilib.command3`). Not Commands v2: do not import CommandsV2, `Subsystem`,
  or v2 examples. Mechanisms implement `Mechanism`; there is no automatic `periodic()`, so inputs
  are polled explicitly in `Robot.robotPeriodic()` via `RobotContainer.updateInputs()`.
- CTRE Phoenix 6 `26.70.0-alpha-2` swerve, ChoreoLib `2027.0.0-alpha-3` (low-level trajectory API
  only; its v2 AutoFactory helpers are intentionally unused), PhotonLib dev build, CameraServer.
- Package names are the 2027 ones (`org.wpilib.math`, `org.wpilib.units`, ...). Previous-season
  code and docs usually do not apply unchanged. Verify APIs against the pinned versions.
- Do not change dependency versions without checking the official Systemcore compatibility matrix
  (docs/Software-Compatibility.md).

## Build and test

Windows PowerShell, from this folder:

```powershell
$env:JAVA_HOME = 'C:\Users\Public\wpilib\2027_alpha7\jdk'   # WPILib-bundled JDK 25
.\gradlew.bat build --no-daemon      # compile + all JUnit tests
.\gradlew.bat simulateJava           # desktop simulation
```

Phoenix "CAN message is stale" warnings during tests are expected (no real bus). CI runs the same
build on Windows and Ubuntu (.github/workflows/build.yml). Never claim a build or test passed
unless you ran it and saw the result.

## Hardware (proposed, not yet discovered on a real robot)

- SDS MK5i L2 swerve: Kraken X60 drive, Kraken X44 steer, CANcoders, Pigeon2, all on `CAN_S0`.
- `CAN_S1` intake, `CAN_S2` indexer (empty mechanism stubs), `CAN_S3` scoring and `CAN_S4`
  auxiliary reserved. Bus objects live in `RobotCAN.java`; IDs are unique per bus.
- Four PhotonVision cameras (`3658-front/rear/left/right`) on two Orange Pi 5s.
- Driver Xbox USB 0; operator Xbox USB 1 (no bindings yet).

CAN IDs, offsets, geometry, gear ratios, gains, and current limits in `Constants.java` and
`generated/TunerConstants.java` are **placeholders**. Never invent or "fix" them with guessed
values; use clearly marked placeholders and point to the commissioning step that measures them.

## Safety gates: do not flip

These stay `false` until the evidence in docs/Robot-Commissioning.md is recorded and reviewed:

- `Constants.Swerve.HARDWARE_CONFIGURATION_VERIFIED`: blocks real drivetrain startup.
- `Constants.Autonomous.PATH_FOLLOWING_VERIFIED` and `FORCE_FEEDFORWARD_VERIFIED`.
- `Constants.Vision.POSE_FUSION_VERIFIED` (pose fusion is not implemented).

`ArchitectureTest.unfinishedCalibrationCannotBeMistakenForVerified` asserts all four are
`false`. Changing one needs a separate reviewed change that also updates
that test with the commissioning evidence. Never just flip the boolean or skip the test.

## Code conventions

- Every actuator command declares mechanism ownership, has a telemetry name (`"Swerve.Brake"`),
  and leaves its output in a defined safe state (neutral) when it ends or is interrupted.
- Teleop bindings and defaults live inside the single `Controls.Teleop` session command so they
  end on disable; do not add bindings in `teleopInit` or the constructor.
- Autonomous fails stopped: invalid file, unknown alliance, timeout, or stale odometry → hold stopped
  with a status message. No built-in Choreo alliance flipping; blue and red paths are explicit.
- Use WPILib units and geometry types. Document coordinate frames (+X forward, +Y left, +Z up,
  CCW positive), signs, and units (rotations vs. degrees vs. radians) where values enter the code.
- No blocking calls or file parsing in periodic or command loops.
- New mechanisms need current/output limits, travel limits or homing, interlocks, and a defined
  disabled behavior before any operator binding.

## Game rules and field data

The 2027 game details (rules, field dimensions, AprilTag layout, scoring) are unknown until
official FIRST releases, and Team Updates may change them. Cite the rule or section when it matters.
Do not reuse previous-season field geometry.

## Hardware actions

Never deploy, enable the robot, actuate hardware, flash firmware, or change device configuration
without explicit approval from a team member for that specific action.
