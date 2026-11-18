# Team 3658 bench test and commissioning guide

For FIRST Systemcore; four SDS MK5i L2 modules with Kraken X60 drive, Kraken X44 steer,
four CANcoders and Pigeon2; four PhotonVision cameras on two Orange Pi 5 processors;
driver Xbox USB 0 and operator Xbox USB 1. FL/FR/BL/BR mean the robot's own corners.

This procedure does not certify completed tests. Copy the blank worksheets below into a dated
team test log. Record the exact code commit, hardware revision, operator, reviewer, measurements
with units, and evidence for each test. Retain configuration exports, photos, and logs.

## Test rules and prerequisites

1. Read the setup and acceptance criteria before each test.
2. Mark each step PASS, FAIL, BLOCKED, or N/A. Blank means untested.
3. On FAIL, disable, remove power if needed, repair, and repeat the affected test before advancing.
   BLOCKED means missing equipment/software/data; it is not a pass. N/A needs an explicit reason.
4. Use a student operator and a separate observer ready to disable/remove main power during motion.
   Secure the chassis on stable supports, with wheels and steering travel clear of people/cables.
   Disable before touching hardware. A held A brake is not a mechanical support or parking brake.
5. Approve output, duration, current, temperature, voltage, and stop-response limits before motion.
   Blank required limits block motion. Limits depend on installed wiring, protection, and vendor
   ratings; proposed **team starting targets** below are not manufacturer specifications.
6. Review electrical, mechanical, and programming results with the relevant leads and a mentor.

The normal robot currently refuses real drivetrain startup while
HARDWARE_CONFIGURATION_VERIFIED is false. For early discovery/calibration, use a separately
reviewed Systemcore-compatible Phoenix diagnostic project or supported Tuner commissioning
workflow. Its initial server must initialize the required devices without commanding motion.
Retain its source/commit; do not bypass the main startup guard just to discover devices.

The repository has no dedicated bench speed governor, robot-centric driver binding, hardware
test command suite, or live camera-health dashboard. Required diagnostic instrumentation and
bench caps need a separate reviewed commissioning change. Mark unavailable tests BLOCKED.

## Step 0 — Freeze software, tools, and limits

1. Record robot revision, battery ID, tested commit, diagnostic commit, operators, and limits.
2. Collect meter, tape/calipers, angle reference, wheel marks, supports, a flat camera-calibration
   board, known-size/tag-family AprilTag target, and compatible programming/Tuner tools.
3. Record actual versions using [Software Compatibility](Software-Compatibility.md):
   WPILib/GradleRIO 2027 Alpha 8, Java 25, Systemcore image 14 or later for its hardware revision,
   FIRST Driver Station Alpha 7 or later, Phoenix API 26.70.0-alpha-2 and firmware 26.70.x.
   Record Tuner X Preview and feature licenses. Both Orange Pis must match the pinned PhotonLib
   development build. Recheck the official matrix before upgrading.
4. Run the normal build/tests and record its CI link. Identify the Tuner plots, Driver Station
   logs, PhotonVision UI, or reviewed logger/debugger used to observe each later measurement.

**PASS:** compatible versions, successful desktop build/tests, identified observers, approved limits.
**FAIL/BLOCKED:** incompatible software, missing diagnostic tools/telemetry, or startup/native errors.
**Record:** session/software worksheets, build/CI links, diagnostic source, and limits.

## Step 1 — Inspect unpowered hardware

1. Disconnect battery and other power sources. Identify the actual distribution/protection hardware.
2. Confirm each corner has MK5i L2, X60 drive, X44 steer, and CANcoder. Record part/serial numbers,
   wheel/tread, magnet installation, and Pigeon2 mounting orientation.
3. Label all corners and both ends of each power/CAN cable. Inspect polarity, protection, strain
   relief, connectors, fasteners, full steering clearance, and free movement without binding.
4. Record each Orange Pi 5 power supply and verify its ratings against the installed processor.
   Record camera model/lens/serial and physical USB port; the code does not specify camera models.
5. Isolate uncommissioned mechanism actuators and confirm supports cannot tip.

**PASS:** exact hardware inventory agrees with installation; electrical/mechanical inspection signed.
**FAIL:** wrong part, loose magnet, binding, damaged/unknown wiring or protection, unstable supports.
**Record:** inventory, power/protection ratings, Pigeon orientation, photos, and signoff.

## Step 2 — Trace CAN and measure termination with power removed

Never measure resistance on a powered bus. Do not connect the independent Systemcore buses together.

1. Trace each populated bus and draw its chain, actual connector pin assignments, and both
   termination locations. Verify built-in/switched termination on the actual hardware revision.
   Do not copy roboRIO connector/termination assumptions to Systemcore.
2. With all power removed and stored energy discharged, measure CAN-H to CAN-L.
   Two 120-ohm end terminations in parallel produce approximately 60 ohms. Investigate about
   120 ohms, 40 ohms, near-zero, or open readings. Record approved tolerance and actual reading.
3. For an unused connector, record unpopulated/N/A; an empty reserved bus is not required to
   measure 60 ohms. Remove the meter before reconnecting power.

| Port | Allocation | Expected current population |
| --- | --- | --- |
| CAN_S0 | Swerve | 8 motor controllers, 4 CANcoders, Pigeon2 |
| CAN_S1 | Intake | Hardware unselected; inventory any actual installation |
| CAN_S2 | Indexer | Hardware unselected; inventory any actual installation |
| CAN_S3 | Scoring reserve | Reserved |
| CAN_S4 | Auxiliary reserve | Reserved |

**PASS:** reviewed topology/polarity and correct end termination for every populated bus.
**FAIL:** cross-connected ports, shorts, unexplained resistance, or unknown termination.
**Record:** bus sketches, endpoints, ohms/tolerance, and repair/retest evidence.

## Step 3 — Power up disabled and discover all 13 CAN_S0 devices

1. Start disabled, wheels raised, sticks centered, with the reviewed non-actuating diagnostic
   server. Record battery/Systemcore voltage, idle current if measurable, and boot time.
   Stop for unexpected movement, resets, hot connectors, smoke, or voltage outside approved limits.
2. Set Systemcore/Driver Station team number 3658. Record hardware revision, image, hostname/IP,
   connection method, and stable disabled communications.
3. Discover devices in the compatible Tuner X Preview. Identify one physical device at a time.
   Duplicate factory IDs may require connecting/configuring devices individually.
4. Confirm model, serial, bus, unique ID, firmware, license, and faults for each row below.
   Save Tuner configurations/self-test exports. Reboot disabled and recheck the full inventory.

| Device | Proposed CAN_S0 ID |
| --- | ---: |
| Pigeon2 (external drivetrain gyro, not Systemcore onboard IMU) | 0 |
| FL X60 / X44 / CANcoder | 1 / 2 / 3 |
| FR X60 / X44 / CANcoder | 4 / 5 / 6 |
| BL X60 / X44 / CANcoder | 7 / 8 / 9 |
| BR X60 / X44 / CANcoder | 10 / 11 / 12 |

5. Observe CAN_S0 disabled for 60 seconds (**team starting duration**). Record utilization,
   error-counter deltas, resets, stale signals, faults, and reconnect times. Preserve historical
   fault evidence before clearing faults.

**PASS:** all 13 correct devices return after reboot; no motion, new communication errors,
resets, or stale signals; utilization within the approved limit with margin.
**FAIL:** missing/duplicate/wrong-bus devices, growing errors, or unstable power.
**Record:** device/software/power/CAN worksheets and exports. Proposed IDs are not discovered IDs.

## Step 4 — Measure geometry and verify gearing

1. Define origin at the center of the module steering axes: +X forward, +Y left, +Z up.
2. Measure front/rear trackwidth, left/right wheelbase, and both diagonals between steering axes.
   Repeat, record units and precision, and derive each corner's signed X/Y coordinates.
   Use half wheelbase/trackwidth only if the measured chassis is rectangular and centered.
3. Verify installed drive gearing. The code's proposed L2 ratio is
   (54/14) × (25/32) × (30/15), about 6.0268 motor rotations per wheel rotation.
   Verify the proposed 26:1 steer ratio against installed hardware.
4. Establish the actual Phoenix coupling ratio from the compatible module/Tuner workflow.
   The scaffold's zero coupling value is not a measurement.
5. Record tire model, tread, nominal diameter/circumference. Loaded effective radius is measured
   in step 12; the nominal 2-inch radius in code is provisional.

**PASS:** repeatable geometry, correct corner signs, verified ratios/coupling, resolved asymmetry.
**FAIL/BLOCKED:** placeholder dimensions, swapped axes/corners, unknown ratios, inconsistent readings.
**Record:** geometry worksheet, uncertainty, gear evidence, and later loaded-radius follow-up.

## Step 5 — Calibrate CANcoders and check Pigeon2

1. Keep outputs disabled. Use the compatible Tuner MK5i alignment/offset workflow; check each
   CANcoder's magnet health and direction.
2. Align to the physical forward reference; record raw absolute position, offset in rotations,
   inversion, and resulting angle. Never paste degrees/radians into a rotations field.
3. Compare forward/left/back/right headings to a physical angle reference. Suggested **team
   starting targets:** error within 2 degrees, repeatability within 1 degree. Approve or replace
   these beforehand. Reboot disabled and repeat forward alignment.
4. Record stationary Pigeon2 drift over 60 seconds. With motor outputs disabled, use a safe
   manual floor-orientation check or suitable fixture for a known 90-degree counterclockwise
   rotation. Reported yaw must have the correct axis/sign; record measured error/drift against
   the session's approved tolerance. Return to supports before motor tests.

**PASS:** healthy magnets, correct signs, repeatable absolute alignment after reboot, correct gyro.
**FAIL:** unstable/jumping readings, wrong axis/sign, excessive error/drift, or missing selected
feedback-mode licensing. **Record:** offsets, inversions, heading errors, drift, and calibration export.

## Step 6 — Test FL, FR, BL, BR individually at limited output

Prerequisites: steps 0–5 PASS, approved limits applied/read back, supports secure, observer ready.

1. Configure X60 drive and X44 steer current limits and feedback using the reviewed Tuner/
   commissioning workflow. Distinguish supply versus stator current. The scaffold's 60 A drive
   supply and 40 A steer stator settings are placeholders, not approved limits for this robot.
2. Select FL only. At the lowest useful output within the approved voltage/duty/pulse cap,
   pulse drive positive, stop, then negative. Record output/units/duration, direction, sensor
   sign, peak current, minimum voltage, temperature, and faults.
3. At zero steering reference, positive drive must correspond to forward wheel travel.
   Test small steering increments before larger angles. Stop if angle error grows, binding
   occurs, or closed-loop control runs away/oscillates.
4. Repeat FR, BL, BR. Correct inversions in the vendor workflow, not by reversing global sticks.
   Retest changed modules. Record commanded/measured angles, settling, gains, and current limits.
5. Disable and verify output neutralizes. Zero commanded output is different from a wheel that
   has physically stopped; a free wheel can coast.

**PASS:** correct individual motor/sensor signs, stable limited response, all approved limits met,
neutral on disable. **FAIL:** wrong corner, runaway, binding, sustained oscillation, excessive
current/heat, or output after disable. **Record:** module worksheet and plots.

## Step 7 — Generate constants and qualify an integrated candidate

1. Export compatible Tuner constants after individual module validation. Cross-check bus, IDs,
   offsets/units, coordinates, inversions, X60/X44 arrangements, ratios/coupling, feedback/license,
   gains/current limits, wheel radius, and speed-at-12-V assumptions against the record.
2. Integrate hardware constants without replacing the Commands v3 wrapper, teleop session,
   neutral cleanup, CAN map, camera clients, or controllers with a generated Commands v2 sample.
3. Add/review conservative bench caps before integrated movement. Suggested **team starting
   caps:** 0.25 m/s translation and 0.25 rad/s rotation, or lower as appropriate. Current normal
   limits of 5 m/s and 0.75 rotations/s are not bench caps. This guide adds no such code.
4. Keep HARDWARE_CONFIGURATION_VERIFIED false until its documented sensor/configuration/current/
   gains/firmware prerequisites pass. If verification depends on later floor measurements,
   continue with diagnostics; integrated application tests remain BLOCKED.
5. Once verified, make a separate reviewed change to the hardware flag and constants.
   ArchitectureTest currently asserts the flag is false: update it to validate the commissioned
   configuration/evidence in that change. Do not skip the test or merely flip the boolean.
   Run build/CI and record the candidate commit before deployment.

**PASS:** exported constants match measured evidence; guarded prerequisites and bench limits are
reviewed and candidate tests pass. **FAIL/BLOCKED:** unknown required values, missing limits/
licenses/instrumentation, or overwritten v3 code. Do not run full-speed on-blocks characterization.
**Record:** export/diff, commit, caps, build/CI, student/mentor review.

## Step 8 — Integrated startup while disabled

1. Deploy only the reviewed candidate. Start disabled with wheels raised and all controls released.
   Confirm team 3658, expected commit, all devices, supply health, and no motor output.
2. Observe for the step 3 duration. Record errors, resets, voltage, CAN counters, and motor modes.
3. Check startup with both Orange Pis present, then absent. Camera absence must not crash robot
   startup or disabled polling. Restore processors before camera acceptance.
4. A startup-guard refusal is expected for the uncommissioned repository; resolve prerequisites,
   rather than treating it as permission to bypass the guard.

**PASS:** stable disabled startup, neutral motors, healthy CAN, graceful absent-camera handling.
**FAIL:** native/startup errors, wrong commit, motion, resets, or stale drivetrain signals.
**Record:** deployed commit, logs, power/CAN readings, and neutral evidence.

## Step 9 — Xbox and Commands v3 transitions on supports

Use bench caps and a reviewed scheduler event logger/debugger. No command dashboard is supplied.

1. Verify driver USB 0 and operator USB 1. While disabled, record each stick/button mapping, sign,
   and centered residual. The 0.08 deadband must reject centered noise.
2. Enable teleop centered: one Controls.Teleop session and one Swerve.TeleopDrive default.
   Apply small capped translation/rotation separately; inspect module direction on supports.
3. Hold driver A: Swerve.Brake owns the drivetrain, with zero drive speed demand. Steering may
   move to brake posture. Press left bumper while holding A; brake must retain ownership.
4. Release A: one drive default resumes. Press/release left bumper without A; heading reset occurs
   once, releases ownership, and driving resumes. Check field-relative travel later on the floor.
5. Disable during a nonzero capped request. Verify all eight motors neutral by control-mode/
   output telemetry, not just command names. Measure response time against the approved limit.
6. Perform five disable/enable cycles with teleop still selected (**team starting count**).
   Each A press produces one brake response; repeat a cycle holding A through disable/re-enable.
7. Enabled autonomous must hold Autonomous.HoldStopped without translation or driver control.
   Utility must use neutral-output Swerve.Idle with no teleop button responses.
8. Exercise operator controls: they currently have no bindings and must not drive any mechanism.
   Finish disabled before touching the setup.

**PASS:** expected ownership/output in every state, no duplicate bindings or latched drive request.
**FAIL:** wrong controller, reset releases held brake, duplicated action, wrong-mode motion,
non-neutral disable/utility, or excessive response time. **Record:** transition worksheet and logs.

## Step 10 — Four-camera PhotonVision bench tests

Keep motors disabled; use [PhotonVision Setup](PhotonVision-Setup.md) alongside this step.

1. Record both Orange Pi 5 serials, network identities, power, temperature, and exact software.
   Both must match the robot's PhotonLib development build.
2. Connect/identify one physical camera at a time:
   A front = 3658-front; A rear = 3658-rear; B left = 3658-left; B right = 3658-right.
   Record processor USB port, camera model/lens/serial; confirm unique mapping after reboot.
3. Secure focus/lens. At intended resolution, calibrate each camera independently in PhotonVision
   using a flat, measured board and varied coverage. Save calibration exports; do not copy
   intrinsics between cameras. Record board dimensions, sample count, mean error (pixels), FOV,
   resolution, exposure, and gain. Approve a calibration error threshold before testing;
   PhotonVision generally recommends subpixel mean error, but image/model quality also matters.
4. Measure robot-origin-to-camera X/Y/Z in meters and roll/pitch/yaw with stated angle units.
   Use the documented coordinate conventions and optical reference, not an arbitrary housing face.
5. Present a known-size/tag-family target at measured near/far distances and left/center/right.
   Record detection continuity, FPS, and latency against approved targets. This is not validation
   of 2027 field pose accuracy.
6. Run all four simultaneously for 60 seconds (**team starting duration**). Reboot both processors;
   verify calibration/name persistence and stream recovery.
7. Disconnect/reconnect each camera, then each entire processor, while disabled.
   Unaffected clients must keep working and robot polling must not stall.
8. Observe getCameraStatuses() through a reviewed logger/debugger; it is not currently published
   to a dashboard. The one-second stale threshold measures time since last unread frame receipt,
   not pose validity. Record actual stale/recovery times including loop/network delay.

**PASS:** four unique mappings, accepted independent calibrations/transforms, acceptable simultaneous
FPS/latency, and isolated disconnect/recovery. **FAIL/BLOCKED:** mismatched software, duplicate
mapping, unmeasured calibration/transform, missing measurement tools, stalled polling, or disrupted
unaffected streams. **Record:** camera worksheet, calibration/processor exports, test samples/logs.

## Step 11 — Audit reserved buses

1. Reconfirm CAN_S1 intake, CAN_S2 indexer, CAN_S3 scoring, CAN_S4 auxiliary.
2. If empty, mark actuator testing N/A and reservation/isolation inspection PASS.
3. For installed hardware, inventory IDs/controllers/sensors and require its own reviewed procedure:
   protection, gearing, directions, current/output limits, interlocks, jam response, and neutral
   actions before any actuation or operator binding.
4. Confirm temporary wiring did not move devices silently between buses.

**PASS:** intended allocations and ownership documented. **BLOCKED:** installed mechanism lacks
its procedure. Empty scaffolds do not pass functional commissioning.
**Record:** population/isolation, owner, and follow-up procedure.

## Step 12 — Deferred floor validation and release

Bench success alone does not authorize full-speed use or complete robot release.

1. In a cleared area, validate low-speed robot-centric travel first, then field-centric +X forward,
   +Y left, counterclockwise rotation, and heading reset at several orientations. Xbox bindings
   are field-centric only; robot-centric checks require a reviewed diagnostic routine.
2. With robot disabled and safely hand-rolled under operating load, measure travel over multiple
   marked wheel revolutions with steering aligned. Effective radius = distance /
   (2 × pi × wheel revolutions). Record load, tread, direction, distance, revolutions, and repeat
   trials; avoid slip and steering-coupling contamination.
3. Gradually validate loaded gains, velocity, straight/rotation odometry, stopping, temperatures,
   currents, and speed-at-12-V assumptions. Record actual voltage with speed. Unloaded wheel speed
   is not loaded robot speed. Approve floor tolerances/limits first; retest affected bench steps
   after constants or hardware change.
4. Leave POSE_FUSION_VERIFIED false. Fusion is unimplemented: flipping that flag adds no estimator.
   After the official layout and reviewed estimator exist, validate known-position/yaw errors,
   stationary/rotating/moving tests, Phoenix timestamp conversion, uncertainty/rejection, and
   camera/processor loss. Record separate fusion acceptance; missing software/layout is BLOCKED.
5. Review evidence and record separate decisions for bench acceptance, drivetrain floor release,
   each mechanism, camera transport/calibration, and fusion. Document restrictions/issues/owners.
6. Record final configuration/commit/tag and student/mentor plus relevant lead signoffs.
   Reopen affected tests after wiring, firmware, calibration, hardware, limits, or code changes.

**PASS:** only the specific tested scope meets its recorded criteria. **FAIL/BLOCKED:** unresolved
faults, unmeasured behavior, or missing implementation. **Record:** floor data, scoped approvals,
release commit/tag, and exclusions.

## Copyable measurement worksheets

Blank cells mean unmeasured, not the provisional code value. Add rows for repeated trials/devices.

### Session, versions, and approved limits

| Field | Actual value / evidence |
| --- | --- |
| Date/time, robot revision, tested commit, diagnostic commit | |
| Operator, power/disable observer, reviewers | |
| Battery ID, distribution/protection hardware, support/setup photo | |
| Systemcore hardware/image/network/team number | |
| WPILib/GradleRIO/JDK/Driver Station/Tuner X Preview | |
| Phoenix API/firmware/license exports | |
| Orange Pi A and B image/PhotonVision/network/power | |
| PhotonLib/CameraServer/OpenCV; build/tests/CI link | |
| Output cap (V or duty), pulse cap (s), translation (m/s), rotation (rad/s) | |
| X60 and X44 supply/stator limits (A), enable flags, neutral modes | |
| Minimum voltage (V), temperature ceiling (C), CAN utilization ceiling (%) | |
| Angle/repeatability/gyro tolerance; maximum stop response (s) | |
| Camera FPS floor, latency ceiling (ms), calibration limit (pixels) | |

### Bus and power readings

| Bus | Actual devices / terminator endpoints | Unpowered ohms | Utilization min/max (%) | Error/reset counters start/end | Result/log |
| --- | --- | --- | --- | --- | --- |
| CAN_S0 | | | | | |
| CAN_S1 | | | | | |
| CAN_S2 | | | | | |
| CAN_S3 | | | | | |
| CAN_S4 | | | | | |

| Trial | Battery rest/min (V) | Systemcore min (V) | Idle/peak current (A), measurement point/type | Boot/recovery (s) | Result/log |
| --- | --- | --- | --- | --- | --- |
| First power-up | | | | | |
| Modules active | | | | | |
| Four cameras active | | | | | |

### Device identity and calibration

| Device | Proposed ID | Actual bus/ID | Part/serial | Firmware/license | Offset (rot) / inversions / config export |
| --- | --- | --- | --- | --- | --- |
| Pigeon2 | 0 | | | | |
| FL X60 | 1 | | | | |
| FL X44 | 2 | | | | |
| FL CANcoder | 3 | | | | |
| FR X60 | 4 | | | | |
| FR X44 | 5 | | | | |
| FR CANcoder | 6 | | | | |
| BL X60 | 7 | | | | |
| BL X44 | 8 | | | | |
| BL CANcoder | 9 | | | | |
| BR X60 | 10 | | | | |
| BR X44 | 11 | | | | |
| BR CANcoder | 12 | | | | |

### Geometry and module measurements

| Measurement | Trial 1 | Trial 2 | Units / precision / evidence |
| --- | --- | --- | --- |
| Trackwidth front/rear; wheelbase left/right; both diagonals | | | |
| Signed FL/FR/BL/BR module X/Y coordinates | | | |
| Actual drive/steer/coupling ratios | | | |
| Nominal wheel dimensions; loaded radius/load/distance/revolutions later | | | |
| Pigeon known rotation/reported change; 60-s drift | | | |

| Corner/trial | Output + units/duration | Direction/sign; angle request/actual/error (deg) | Before/after reboot (deg) | Peak supply/stator (A) | Voltage min (V), temp start/end (C) | Neutral / log |
| --- | --- | --- | --- | --- | --- | --- |
| FL | | | | | | |
| FR | | | | | | |
| BL | | | | | | |
| BR | | | | | | |

### Transitions and cameras

| Trial | Input/mode sequence | Expected/observed ownership/output | Neutral response (s) / event count | Result/evidence |
| --- | --- | --- | --- | --- |
| Startup disabled | | | | |
| Nonzero drive → disable | | | | |
| A + bumper, release, bumper alone | | | | |
| Five same-mode enable cycles, including A held | | | | |
| Autonomous / utility / operator isolation | | | | |

| Camera | Processor/USB/model/lens/serial | Resolution/exposure/gain | Board/sample count/error (px)/calibration export | X/Y/Z (m), roll/pitch/yaw + units | FPS/latency (ms)/target distances (m) | Stale/recovery (s)/reboot |
| --- | --- | --- | --- | --- | --- | --- |
| 3658-front | A / | | | | | |
| 3658-rear | A / | | | | | |
| 3658-left | B / | | | | | |
| 3658-right | B / | | | | | |

### Step log and release decision

| Step | PASS / FAIL / BLOCKED / N/A | Measurements/logs | Defect / retest | Operator / reviewer / date |
| --- | --- | --- | --- | --- |
| 0 Software/tools/limits | | | | |
| 1 Unpowered inspection | | | | |
| 2 CAN termination | | | | |
| 3 Disabled discovery | | | | |
| 4 Geometry/gearing | | | | |
| 5 Sensors/offsets | | | | |
| 6 Individual modules | | | | |
| 7 Constants/candidate | | | | |
| 8 Disabled startup | | | | |
| 9 Xbox/mode transitions | | | | |
| 10 Four cameras | | | | |
| 11 Reserved buses | | | | |
| 12 Floor/fusion/release | | | | |

| Release field | Decision / evidence |
| --- | --- |
| Bench-only / drivetrain floor / individual mechanism approval | |
| Vision transport/calibration / fusion approval or BLOCKED reason | |
| Open issues, restrictions, follow-up owners | |
| Final commit/tag/config exports, student/mentor/lead signoffs | |

## Vendor references

Use documentation compatible with the pinned prerelease software; current UI screenshots may differ.

- [Systemcore compatibility and diagnostics](https://github.com/wpilibsuite/SystemcoreTesting)
- [Phoenix Systemcore prerequisites](https://github.com/wpilibsuite/SystemcoreTesting/blob/main/CTR-Phoenix.md)
- [CTRE module configuration](https://v6.docs.ctr-electronics.com/en/latest/docs/tuner/tuner-swerve/configuring-your-modules.html)
- [CTRE drivetrain validation](https://v6.docs.ctr-electronics.com/en/latest/docs/tuner/tuner-swerve/validating-drivetrain.html)
- [CTRE feedback/licensing requirements](https://v6.docs.ctr-electronics.com/en/latest/docs/tuner/tuner-swerve/swerve-system-requirements.html)
- [WPILib CAN termination principles](https://docs.wpilib.org/en/latest/docs/hardware/hardware-basics/can-wiring-basics.html)
  (roboRIO/PDP examples are not Systemcore connector instructions)
- [PhotonVision calibration](https://docs.photonvision.org/en/latest/docs/calibration/calibration.html)
- [PhotonVision coordinate systems](https://docs.photonvision.org/en/latest/docs/apriltag-pipelines/coordinate-systems.html)
