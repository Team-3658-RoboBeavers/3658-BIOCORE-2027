# Writing autonomous routines with Choreo

Team 3658 uses ChoreoLib/GUI **2027.0.0-alpha-3** with WPILib Alpha 8, Systemcore,
Phoenix 26.70.0-alpha-2, and Commands v3. The version is pinned in
[ChoreoLib2027Alpha.json](../vendordeps/ChoreoLib2027Alpha.json).

This is ready for authoring and desktop simulation. Physical autonomous motion is not
commissioned. The existing drivetrain startup gate and a separate autonomous release gate remain
false; no 2027 game routine or official field geometry is invented.

## 1. Install the matching authoring tool

Download the GUI for your computer from the
[Choreo Alpha 3 release](https://github.com/SleipnirGroup/Choreo/releases/tag/v2027.0.0-alpha-3).
Do not substitute the 2026 stable vendordep or import CommandsV2.json.

This transitional Choreo release supports the Systemcore math/packages, but its AutoFactory,
AutoRoutine, AutoChooser, and related command helpers still target Commands v2. We use only
Choreo.loadTrajectory and the trajectory/sample APIs. ChoreoAutos and the drivetrain follower
provide native Commands v3 ownership, composition, selection, cancellation, and neutral cleanup.
The runtime regression test checks that Commands v2 is absent.

## 2. Open the unverified robot project and replace every model placeholder

Open [3658-BIOCORE-2027-UNVERIFIED.chor](../choreo/3658-BIOCORE-2027-UNVERIFIED.chor).
It is a schema-2 Swerve project for the matching GUI with no game trajectories. Java code generation
is disabled because this integration reads exported trajectory files directly.

| Choreo model input | Starter value | Required source before physical use |
| --- | --- | --- |
| FL / BL module positions | (+0.254,+0.254) / (-0.254,+0.254) m | Measured steering-axis geometry; confirm mirrored right-side assumption |
| Drive gearing | (54/14) × (25/32) × (30/15), about 6.0268 | Verify installed MK5i L2 gears |
| Wheel radius | 0.0508 m | Measured loaded rolling radius and tread |
| Mass | 60 kg placeholder | Weigh the complete operating robot |
| Yaw moment of inertia | 6 kg m² placeholder | Measured or reviewed CAD estimate |
| Motor free angular speed | 400 rad/s placeholder | X60 drive motor model at actual voltage |
| Motor torque limit | 1 N m placeholder | X60 model constrained by approved current/protection |
| Friction coefficient | 1 placeholder | Measured/approved traction model |
| Bumper extents from origin | 0.45 m front/side/back placeholders | Actual bumper-inclusive footprint |
| Differential trackwidth | 0.508 m unused Swerve schema field | Does not configure swerve kinematics |

These numbers are editable placeholders, not a characterized X60 model or a finished robot.
X44 steer settings remain in Phoenix/Tuner, not the Choreo drive torque entry. Validate steering
response and current limits separately. Record the final model, units, source, and configuration
hash in the [bench record](Robot-Commissioning.md).

The right module coordinates are mirrored by this GUI model; verify that matches the measured
robot. If the installed geometry cannot be represented accurately, resolve that before solving.

## 3. Establish the coordinate frame and write trajectories

1. Confirm the editor background, field origin, dimensions, and axes. The transitional GUI's older
   field is a practice background, not the official BIOCORE map. Do not treat it as 2027 geometry.
2. Use one documented global field frame shared by Choreo, odometry, future vision, and starting
   placements. Field velocity means +X/+Y in that frame, not the driver's current forward direction.
3. Create separate explicitly authored blue and red paths. This code deliberately does not use
   Choreo's built-in alliance flip or hardcoded previous-year field size/symmetry.
4. Use simple filename stems containing letters, numbers, underscores, or hyphens, for example
   Blue-Leave and Red-Leave. Do not put paths in subfolders or pass a .traj suffix to registration.
5. Start each leg at t=0 and at rest; end each leg at rest. Use continuous endpoint/start poses
   across separate legs. This initial integration supports stop-to-stop segments, not rolling
   transitions or arbitrary Choreo split boundaries.
6. Constrain paths to the configured autonomous limits: initially 0.5 m/s translation and
   0.5 rad/s rotation. Limits/gains are provisional in Constants.Autonomous. A faster trajectory
   is rejected, not silently rescheduled to a slower time.
7. Keep the routine's nominal durations plus up to 0.5 s settling per leg within the configured
   15 s budget. Verify the final competition timing rules when available.
8. Do not add embedded Choreo event markers yet: nonempty runtime events are rejected.
   Mechanism hardware/actions remain unfinished; use explicitly reviewed v3 compositions once
   those commands exist. Events are never silently used to actuate unknown hardware.

For a first physical acceptance path, use a reviewed short, low-speed straight path in a cleared
area. The default limits are not permission to skip the lower bench caps in the commissioning guide.

## 4. Solve, save, and deploy the generated samples

Keep editable project/trajectory source in choreo/. Solve in the matching GUI and save its
trajectory, including generated Swerve samples. Copy the reviewed exports to:

    src/main/deploy/choreo/Blue-Leave.traj
    src/main/deploy/choreo/Red-Leave.traj

For example, from the repository root in PowerShell:

~~~powershell
Copy-Item -LiteralPath choreo/Blue-Leave.traj -Destination src/main/deploy/choreo/Blue-Leave.traj
Copy-Item -LiteralPath choreo/Red-Leave.traj -Destination src/main/deploy/choreo/Red-Leave.traj
.gradlew.bat build --no-daemon
~~~

Copying/solving is explicit; the build does not run the optimizer or automatically copy authoring
files. Commit the authoring project, editable trajectories, and matching deploy exports together.
Review changed samples and the model that generated them.

The existing Systemcore static-file target sends src/main/deploy to /home/systemcore/deploy.
Choreo loads /home/systemcore/deploy/choreo on the robot. Build alone does not deploy.
The deploy configuration does not delete old files; registration is explicit, so an obsolete file
does not automatically appear in the chooser. Remove obsolete files deliberately during release.

ChoreoPaths preloads/validates registered exports during robot construction. Missing files, bad
schema/type, invalid samples, nonfinite data, non-monotonic times, unsupported events, unsafe
start/end motion, or excessive speed/duration make that option hold stopped with a status reason.
No file parsing occurs in the periodic trajectory-following loop.

## 5. Register the routine in Commands v3

In the ChoreoAutos constructor, before Tunables.publish, add a reviewed registration:

~~~java
addAllianceRoutine(
    "Leave",
    List.of("Blue-Leave"),
    List.of("Red-Leave"));
~~~

For multiple stop-to-stop legs:

~~~java
addAllianceRoutine(
    "Two legs",
    List.of("Blue-Leave", "Blue-Return"),
    List.of("Red-Leave", "Red-Return"));
~~~

Both lists must be nonempty and have the same number of legs. Each leg is preloaded; successive
poses must agree within the configured position/heading tolerances. Registration failures keep
the option selectable only as a stopped fallback, with an explanatory status.

At enable, the routine snapshots the chosen option and alliance. Unknown alliance holds stopped.
Blue selects the blue files; red selects the red files. Paths are sampled without additional
flipping. No dashboard/alliance change can switch the route midway through an enabled routine.

The root v3 coroutine awaits each drivetrain command. Pose resets once, to the first leg's initial
pose; later legs continue odometry. Completion requires endpoint position/heading tolerance.
Failure does not advance to the next leg. After completion/failure the routine awaits the stopped
brake command until autonomous exits. Disabling, changing mode, or canceling the root cancels its
children and neutralizes the drivetrain.

When adding scoring/intake/indexer actions later, create requirement-owning mechanism commands
and compose with coroutine.await/fork according to their intended lifetime. The alpha Choreo v2
auto helpers are not a substitute for those v3 factories.

## 6. Select and monitor an auto

Use a dashboard compatible with WPILib Alpha 8 Selectable at:

    /Tunables/Autonomous/Routine

Hold stopped is the default. The simulation example is published only in simulation; it is not
an option on real hardware. Real routines appear only after explicit code registration.

For a modern NetworkTables editor, write the requested label to:

    /Tunables/Autonomous/Routine/selected/tune

Read the accepted label at selected/value and inspect the chooser's options/default. Do not write
selected/value as a request. Alpha 8 also has compatibility support for older chooser dashboards;
verify the visible selection before enabling.

Read execution/fallback messages at /Autonomous/status. A failed physical commissioning gate,
unknown alliance, invalid file, tracking fault, or timeout explains why the robot stayed stopped.

## 7. Run the included simulation

~~~powershell
.gradlew.bat build --no-daemon
.gradlew.bat simulateJava
~~~

In the simulation Driver Station choose autonomous, select Simulation: 0.25 m straight in the
dashboard, and enable. The analytic fixture starts at (0,0,0), moves 0.25 m in +X over 2 seconds,
and ends stopped. It is a hand-authored runtime/test fixture, not a solver-produced game route.
Default Hold stopped remains available. Disable/re-enable to restart from the first sample.

The native regression test uses the same deploy fixture and Phoenix desktop physics. It checks
successful completion, explicit cancellation, and motor neutralization. Other tests check
interpolation, limits, endpoint/sequence validation, shortest heading feedback, capped combined
velocity, explicit alliance choice, unknown-alliance rejection, and absence of Commands v2.

Simulation validates code behavior, not physical tuning, field collision clearance, or an
accurate drivetrain model. The GUI itself is not launched by the build/test task.

## 8. Qualify physical autonomous operation

Keep both HARDWARE_CONFIGURATION_VERIFIED and PATH_FOLLOWING_VERIFIED false until their separate
requirements pass in reviewed commissioning changes. FORCE_FEEDFORWARD_VERIFIED also remains false.

1. Complete the drivetrain bench/floor prerequisites. Confirm CAN_S0, Pigeon2, wheel radius,
   ratios/coupling, measured geometry, offsets/inversions, gains, licenses, and current limits.
2. Approve the Choreo mass/inertia, X60 torque/speed/current model, traction, bumpers, field frame,
   both alliance paths, obstacle clearances, and actual starting placements.
3. Verify field-relative axes and odometry reset on the real robot. Auto uses a fixed field
   perspective, independent of the teleop heading/operator perspective.
4. Measure and tune autonomous feedback with conservative caps and force feedforward off.
   Initially P gains are 1.5 for translation and 3.0 for heading. Endpoint tolerance is 0.10 m /
   0.10 rad, with a 0.5 s settling window. Tracking error above 0.75 m, invalid pose, or odometry
   older than 0.25 s aborts following. Record/approve these values; they are not hardware ratings.
5. The summed velocity feedforward and pose correction are vector-capped; angular output is capped.
   Phoenix receives closed-loop field velocity. Optimizer wheel force feedforward is optional,
   defaults off, and uses [FL, FR, BL, BR] forces in newtons only after separate model approval.
   Forces are suppressed at output caps. Record current/voltage/temperature before approving it.
6. Test missing/invalid paths, unknown alliance, both alliance placements, mode change/disable
   during each leg, interruption, endpoint timeout, and stale odometry. Require stopped output,
   useful status, and no unintended next leg.
7. Record start/final/measured pose, peak tracking/heading errors, duration, settle time, stop time,
   peak current, minimum voltage, fault logs, and path/model/code versions for each trial.

Physical motion tests require an authorized team at the robot. This repository update performs
no hardware deployment and does not clear those verification gates. Four-camera pose fusion
remains separately unimplemented; the follower currently uses Phoenix drivetrain odometry.

## Primary references

- [Official Systemcore Choreo compatibility notes](https://github.com/wpilibsuite/SystemcoreTesting/blob/main/ChoreoLib.md)
- [Matching Choreo GUI/library release](https://github.com/SleipnirGroup/Choreo/releases/tag/v2027.0.0-alpha-3)
- [Pinned Choreo low-level loader source](https://github.com/SleipnirGroup/Choreo/blob/v2027.0.0-alpha-3/choreolib/src/main/java/choreo/Choreo.java)
- [Pinned Swerve sample format](https://github.com/SleipnirGroup/Choreo/blob/v2027.0.0-alpha-3/choreolib/src/main/java/choreo/trajectory/SwerveSample.java)
- [Commands v3 documentation](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/index.html)
