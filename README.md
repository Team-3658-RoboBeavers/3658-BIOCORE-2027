# Team 3658 BIOCORE 2027

Java robot project for Team 3658's FIRST Systemcore robot, using WPILib Commands v3 and
CTRE Phoenix 6 swerve.

## Status and completed work

As of November 17, 2026, this is a **preseason scaffold verified by desktop builds and simulation**.
It is not commissioned for physical robot operation.

Completed work includes:

- Systemcore deployment configuration, team number 3658, Java 25, and a checked-in Gradle wrapper.
- Migration from Commands v2 to Commands v3 and upgrade to WPILib/GradleRIO Alpha 8.
- Mechanism ownership for the drivetrain, intake, indexer, and vision; named command factories,
  explicit scheduler execution, and input polling before commands run.
- Teleop controls contained in one enabled-session command. Its bindings and default command
  are removed on disable as well as mode changes, preventing duplicate bindings on re-enable.
- Brake priority above heading reset, so pressing left bumper cannot release a held A brake.
- Explicit neutral output to all drive and steer motors when a drivetrain request exits and
  whenever the idle default runs. Phoenix's Idle request alone leaves module state unchanged.
- Four PhotonVision camera clients on two Orange Pi 5 processors, with frame-receipt health checks.
- The official CameraServer dependency, supplying cscore and OpenCV native libraries required
  for PhotonLib startup on desktop and Systemcore.
- Choreo Alpha 3 trajectory loading, a native Commands v3 follower, routine selection, explicit
  blue/red route pairs, a simulation fixture, and an unverified GUI starter project.
- Native lifecycle regression coverage and GitHub Actions builds on Windows and Ubuntu.
- CAN wiring, vision setup, software compatibility, and hardware commissioning documentation.

The previous migration commit passed both GitHub Actions jobs. See the
[Actions history](https://github.com/Team-3658-RoboBeavers/3658-BIOCORE-2027/actions/workflows/build.yml)
for the current commit's results.

Real-hardware startup remains blocked while
Constants.Swerve.HARDWARE_CONFIGURATION_VERIFIED is false. Replace provisional Tuner constants
and complete [Bench Test and Commissioning Guide](docs/Robot-Commissioning.md) before enabling physical hardware.
Vision pose fusion is unimplemented and remains marked unverified.

## Hardware and CAN allocation

| Area | Hardware / assignment | Current implementation |
| --- | --- | --- |
| Main controller | FIRST Systemcore | Systemcore deploy target |
| Drivetrain | SDS MK5i L2; Kraken X60 drive; Kraken X44 steer | Phoenix swerve with provisional calibration |
| Swerve bus | CAN_S0 | 8 Talon FX-class controllers, 4 CANcoders, 1 Pigeon2 |
| Intake bus | CAN_S1 | Mechanism boundary; hardware and commands pending |
| Indexer bus | CAN_S2 | Mechanism boundary; hardware and commands pending |
| Scoring bus | CAN_S3 | Reserved |
| Auxiliary bus | CAN_S4 | Reserved |
| Driver controls | Xbox, USB 0 | Field-centric driving, brake, heading reset |
| Operator controls | Xbox, USB 1 | Instantiated and reserved; no mechanism bindings |
| Vision processor A | Orange Pi 5 | Front and rear cameras |
| Vision processor B | Orange Pi 5 | Left and right cameras |

RobotCAN uses typed CANPort.CAN_S0 through CAN_S4 objects. Device IDs must be unique within
each bus. No CANivore or Motioncore is added.

Proposed CAN_S0 IDs are Pigeon2 0; front-left drive/steer/encoder 1/2/3; front-right 4/5/6;
back-left 7/8/9; back-right 10/11/12. These are planned IDs, not a discovered hardware inventory.
See [CAN Networks](docs/CAN-Networks.md).

## Driver controls and command lifecycle

| Input or robot state | Behavior |
| --- | --- |
| Driver left stick Y / X | Forward/backward and sideways field-centric translation |
| Driver right stick X | Rotation |
| Hold driver A | Brake posture; takes priority over heading reset |
| Press driver left bumper | Reset field-centric heading when brake is not holding ownership |
| Operator Xbox | Reserved for future mechanism controls |
| Autonomous enabled | Hold stopped by default; explicitly registered Choreo routines after commissioning |
| Teleop enabled | Run the teleop session and lowest-priority driving default |
| Utility enabled | Neutral-output idle default |
| Disabled | Cancel actuator commands and end the teleop session |

Stick inputs use a 0.08 deadband. Provisional command limits are 5.0 m/s translation and
0.75 rotations/s rotation; these require physical commissioning.

Robot extends TimedRobot. Robot.robotPeriodic() polls vision inputs and calls
Scheduler.getDefault().run(). Commands v3 does not automatically invoke subsystem periodic
callbacks, so vision polling is explicit in every robot mode, including disabled.

The global RobotModeTriggers.teleop() binding starts one Controls.Teleop command per enabled
teleop period. That command installs the driving default and button bindings, then parks.
Its command scope ends when teleop is disabled or deselected, allowing a fresh session on
re-enable without accumulating defaults or bindings. Drivetrain commands declare ownership,
have telemetry names, and explicitly neutralize output on exit.

## Choreo autonomous authoring

Follow [Choreo Autonomous](docs/Choreo-Autonomous.md) to install the matching GUI, open
[choreo/3658-BIOCORE-2027-UNVERIFIED.chor](choreo/3658-BIOCORE-2027-UNVERIFIED.chor),
replace model placeholders, solve paths, copy exports into src/main/deploy/choreo, and register
blue/red stop-to-stop routine sequences in ChoreoAutos.

The dashboard selector is /Tunables/Autonomous/Routine; execution status is /Autonomous/status.
Hold stopped is the default. A 0.25 m simulation-only example exercises the follower without
inventing a 2027 game route. Registered routes preload and validate at startup; invalid files,
unknown alliance, tracking faults, or timeouts fail stopped. Built-in alliance flipping is not
used because the official 2027 geometry/symmetry is not verified.

The follower owns the drivetrain through Commands v3, uses capped field-relative velocity and
pose feedback, resets pose once per routine, checks endpoint completion, and neutralizes output
on exit/cancellation. The full routine owns the drivetrain across leg transitions and while holding
stopped; child followers share that ownership through Commands v3. Tests exercise real Phoenix
desktop physics, dashboard selection, and cancellation of the complete command tree.
Physical path following requires both drivetrain and autonomous commissioning; optimizer force
feedforward has its own approval gate and is off by default. No mechanism actions or pose fusion
are added. See the guide for the exact limits, measurements, and release checklist.

The Choreo compatibility review also hardened export validation and timing: raw exports must
contain every numeric sample field and four force entries per axis, and embedded events are
rejected before the library can discard them. Validation checks Choreo's acceleration-integrated
intervals as well as saved samples; the actual sampled command is checked again at runtime.
Small export rounding tolerance does not change the controller's hard velocity caps.

Autonomous uses WPILib's monotonic clock. Timeout takes precedence over endpoint success after
a delayed scheduler iteration; invalid timing or odometry timestamps abort with neutral output.
The regression suite covers those cases, malformed exports, unsafe interpolation, nested
cancellation, and the existing controller/mode transitions. The compatible Alpha 8 / Choreo
Alpha 3 / Phoenix pins, CAN assignments, cameras, controllers, and commissioning gates remain
unchanged.

## Four-camera vision

| Processor | Position | PhotonVision camera name |
| --- | --- | --- |
| Orange Pi 5 A | Front | 3658-front |
| Orange Pi 5 A | Rear | 3658-rear |
| Orange Pi 5 B | Left | 3658-left |
| Orange Pi 5 B | Right | 3658-right |

Both processors use Ethernet. Install the same PhotonVision development build as the robot's
PhotonLib on both processors. The robot drains unread camera results once per input update
and reports whether frames have arrived within the last second.

Camera transforms are placeholders. No poses are fused, no official 2027 AprilTag field layout
is installed, and Constants.Vision.POSE_FUSION_VERIFIED remains false. Follow
[PhotonVision Setup](docs/PhotonVision-Setup.md) before implementing pose fusion.

## Software baseline

| Component | Pinned version or requirement |
| --- | --- |
| WPILib / GradleRIO | 2027.0.0-alpha-8 |
| Commands | v3 from the matching WPILib release |
| ChoreoLib / Choreo GUI | 2027.0.0-alpha-3 |
| Java | 25 |
| Gradle wrapper | 9.4.1 |
| Systemcore image | 14 or later, for the actual hardware revision |
| FIRST Driver Station | Alpha 7 or later for the Alpha 8 baseline |
| Phoenix 6 API | 26.70.0-alpha-2 |
| Phoenix device firmware | 26.70.x |
| PhotonLib / PhotonVision | dev-v2027.0.0-alpha-2-89-g4b8d9b50 |
| CameraServer / OpenCV | Alpha 8 / 2027-4.13.0-3 |

The official manifests and project import metadata retain the 2027_alpha7 compatibility identifier.
Alpha 8 is an in-place compatible upgrade; build.gradle selects the actual Alpha 8 dependencies.
CommandsV2.json has been removed. Phoenix and PhotonLib remain pinned to the versions identified
by the official Alpha 7/8 compatibility matrix and PhotonVision Dev release.

Choreo Alpha 3 is now included through its low-level trajectory APIs. Its transitional Commands v2
auto helpers are intentionally unused. PathPlanner remains absent.

Version sources and upgrade requirements are in
[Software Compatibility](docs/Software-Compatibility.md), including the
[Alpha 8 release notes](https://github.com/wpilibsuite/allwpilib/releases/tag/v2027.0.0-alpha-8)
and [Systemcore compatibility matrix](https://github.com/wpilibsuite/SystemcoreTesting).

## Build, tests, and simulation

Install the matching WPILib toolchain and use its Java 25 JDK. If Java is not found, configure
JAVA_HOME and PATH to that JDK before running the wrapper.

Windows PowerShell:

~~~powershell
.\gradlew.bat build --no-daemon
~~~

Linux/macOS:

~~~bash
./gradlew build --no-daemon
~~~

The build compiles robot code, packages the application, resolves native dependencies, and runs
JUnit tests. Architecture tests check unique camera names, proposed swerve IDs, and unverified
commissioning flags.

The native lifecycle test constructs the actual Phoenix drivetrain and all four PhotonCamera
clients. It checks controller separation, command ownership and priority, brake/release, heading
reset, brake-plus-reset interaction, autonomous/utility transitions, disabled cancellation,
teleop reentry, repeated same-mode disable/re-enable, and neutral control modes on all eight
simulated drivetrain motors after active driving.

To start desktop simulation, use the WPILib simulation command or:

~~~powershell
.\gradlew.bat simulateJava
~~~

Disconnected-camera diagnostics are expected when the Orange Pi processors are absent. Simulation
does not validate live camera frames, electrical wiring, firmware, real CAN timing, physical
calibration, or drivetrain safety. GitHub Actions runs the same build and tests on Windows and
Ubuntu; no workflow deploys to hardware.

## Latest FIRST Systemcore information

FIRST's [November 17 update](https://www.firstinspires.org/community-blog/2026-systemcore-on-sale-date-update)
moves Systemcore ordering to December 3, 2026, with shipping later in December; the 2 GB model
will follow in 2027. It also updates Black Tote cabling contents. The earlier
[pricing announcement](https://community.firstinspires.org/2026-systemcore-pricing-and-availability)
describes the 4 GB Systemcore included in each team's Season Specific Box.

Systemcore is required as the main controller for official FRC competition in 2027.
Use the current Game Manual and Team Updates for final rules. These announcements do not
change this project's CAN assignments or selected hardware.

## Project map

- Robot.java: TimedRobot entry point, scheduler, autonomous lifecycle, and resource cleanup.
- RobotContainer.java: hardware ownership, controller setup, teleop session, input polling,
  and autonomous brake placeholder.
- RobotCAN.java: five Systemcore CAN bus assignments.
- auto/: Choreo loading/validation, feedback math, chooser, alliance selection, and v3 sequences.
- choreo/: editable, unverified Choreo GUI robot project and authoring instructions.
- src/main/deploy/choreo/: deployed trajectory exports and the simulation-only fixture.
- Constants.java: controller ports, camera names/transforms, proposed swerve IDs, and safety gates.
- generated/TunerConstants.java: provisional Phoenix MK5i L2 configuration.
- subsystems/CommandSwerveDrivetrain.java: Commands v3 wrapper, neutral output, and simulation thread.
- subsystems/VisionSubsystem.java: camera clients, health polling, and resource cleanup.
- subsystems/IntakeSubsystem.java and IndexerSubsystem.java: unfinished mechanism boundaries.
- src/test/java/frc/robot/: architecture and native lifecycle regression tests.
- vendordeps/: pinned Commands v3, CameraServer, Phoenix, PhotonLib, and Choreo manifests.
- .github/workflows/build.yml: Windows and Ubuntu CI.
- docs/: CAN, vision, commissioning, and software compatibility records.

## Step-by-step bench testing

Start with the [Bench Test and Commissioning Guide](docs/Robot-Commissioning.md). It replaces
the short checklist with an ordered procedure for our Systemcore, MK5i L2 X60/X44 drivetrain,
13 CAN_S0 devices, four cameras on two Orange Pi 5 processors, and two Xbox controllers.

The stages cover software and approved limits; unpowered inspection and CAN termination;
disabled discovery; geometry/gearing; CANcoder and Pigeon2 calibration; individual module
tests; constants review; integrated startup; controller and mode transitions; camera
calibration/performance/recovery; and reserved buses. Loaded floor measurements, future
vision fusion, and release approval are identified separately.

Each stage has PASS/FAIL/BLOCKED criteria and measurements to record. Copyable worksheets
capture actual versions, serials/IDs, resistance, voltage/current/temperature, geometry,
ratios/offsets/inversions, output limits, command transitions, camera transforms/calibration,
FPS/latency, recovery times, defects/retests, and student/mentor signoff. Blank values remain
unmeasured, and proposed team starting targets are distinguished from manufacturer ratings.

The guide preserves the real-hardware startup guard. Early discovery/calibration requires
a separately reviewed Systemcore-compatible diagnostic/Tuner workflow. Integrated motion
checks remain blocked until measured constants, approved limits, required instrumentation,
bench caps, and the guard's prerequisites are satisfied. The guide also explains how the
test that currently asserts the startup flag is false must be updated in a future reviewed
commissioning change. This documentation update does not add test-mode code or certify hardware.

The bench guide now links to the Choreo-specific authoring, model, field-frame, and autonomous
acceptance requirements. A passed general drivetrain test does not clear the autonomous gate.

## Remaining commissioning work

1. Install and record matching Systemcore OS, Driver Station, Phoenix firmware, Tuner X, and
   PhotonVision images on both processors.
2. Discover and label CAN devices; confirm bus assignments and IDs.
3. Replace provisional constants with Phoenix Tuner X output for the assembled MK5i L2 robot.
4. Measure geometry, effective wheel radius, CANcoder offsets, coupling, inversions, and camera
   transforms; tune gains and current limits. Check any Phoenix feature licensing requirements.
5. Select intake, indexer, and scoring hardware and implement commands with limits and interlocks.
6. Install the official field layout, implement pose rejection and uncertainty handling, and
   validate vision before enabling fusion.
7. Complete disabled-on-blocks checks, brake/reset and repeated enable tests, controlled floor
   tests, and the student/mentor release review in the bench guide.
