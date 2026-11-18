# Software Compatibility Snapshot

Checked November 17, 2026 against the official Systemcore testing matrix and release notes.

## Pinned set

| Component | Repository version | Requirement |
| --- | --- | --- |
| WPILib / GradleRIO | 2027.0.0-alpha-8 | Latest release listed by SystemcoreTesting; includes Commands v3 mode scopes and disabled safety fixes |
| Commands | v3, bundled with Alpha 8 | Official CommandsV3 manifest; CommandsV2 removed |
| CameraServer / OpenCV | Alpha 8 / 2027-4.13.0-3 | Official CameraServer manifest supplies cscore and OpenCV native dependencies needed by PhotonLib |
| Systemcore OS | Image 14 or later | Alpha 8 release requirement; select an image for the actual hardware revision |
| FIRST Driver Station | Alpha 7 or later | Alpha 8 release requirement; use the 2027 Driver Station for OpMode support |
| Phoenix 6 | 26.70.0-alpha-2 | Official compatibility matrix lists this for WPILib Alpha 7/8 |
| Phoenix firmware | 26.70.x | Firmware match for the pinned Phoenix API |
| PhotonLib | dev-v2027.0.0-alpha-2-89-g4b8d9b50 | Exact manifest currently published in PhotonVision's Dev release |
| PhotonVision | Matching development image on both Orange Pi 5 processors | Keep coprocessors and PhotonLib on the same build |
| Java | 25 | Project language/runtime baseline; tested with the installed WPILib JDK 25 |
| Gradle | 9.4.1 | Existing checked-in wrapper |

The vendor manifests retain their official 2027_alpha7 compatibility identifier: Alpha 8 is an
in-place compatible upgrade of Alpha 7. The WPILib projectYear value likewise identifies the import
generation; build.gradle pins the actual Alpha 8 dependency version. An Alpha 8 desktop build does
not install or upgrade the IDE, robot image, Driver Station, or firmware.

## Current findings

- FIRST's November 17 update supersedes the September sales announcement: Systemcore ordering now
  opens December 3, 2026, with shipping later in December. The 2 GB model is delayed to 2027.
- FIRST requires Systemcore as the main controller for official FRC events in 2027. The September
  announcement specifies a 4 GB unit in each team's Season Specific Box.
- Complete Molex SL cable kits will not be included in the Black Tote. FIRST says KitBot wiring
  cables and components for making Molex SL cables remain included; a crimper is not included.
- Five native CAN buses remain assigned by RobotCAN: CAN_S0 swerve, CAN_S1 intake, CAN_S2 indexer,
  CAN_S3 scoring reserve, CAN_S4 auxiliary. No CANivore or Motioncore is introduced.
- FIRST's rule preview permits CANivore for additional buses in 2027 as a transition measure;
  consult the final Game Manual and Team Updates at Kickoff.
- The testing matrix now lists compatible PathPlanner and Choreo releases for Alpha 7/8.
  Neither is added: this project has no selected autonomous routes or released 2027 field layout.
  Autonomous holds the drivetrain stopped in the brake posture until routines are designed.
- Phoenix and PhotonLib remain pinned because the current official matrix and PhotonVision Dev
  manifest still identify the existing versions. PhotonLib's floating latest URL is not used.
- All of this remains prerelease software. The existing real-hardware startup guard and
  pose-fusion guard remain false pending commissioning.

## Primary references

- [FIRST November 17 Systemcore update](https://www.firstinspires.org/community-blog/2026-systemcore-on-sale-date-update)
- [FIRST Systemcore pricing and kit inclusion](https://community.firstinspires.org/2026-systemcore-pricing-and-availability)
- [FIRST 2027 rule preview](https://www.firstinspires.org/community-blog/2026-event-robot-rule-preview-for-2027-season-part-1)
- [WPILib Alpha 8 release and requirements](https://github.com/wpilibsuite/allwpilib/releases/tag/v2027.0.0-alpha-8)
- [Systemcore testing and compatibility matrix](https://github.com/wpilibsuite/SystemcoreTesting)
- [Phoenix compatibility](https://github.com/wpilibsuite/SystemcoreTesting/blob/main/CTR-Phoenix.md)
- [PhotonVision development release](https://github.com/PhotonVision/photonvision/releases/tag/Dev)
- [Official CameraServer manifest](https://github.com/wpilibsuite/allwpilib/blob/v2027.0.0-alpha-8/cameraserver/CameraServer.json)
- [Commands v3 documentation](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/index.html)
- [Commands v2 migration guide](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/migration-guide.html)
- [SDS MK5i information](https://www.swervedrivespecialties.com/products/mk5i-swerve-module)

## Upgrade and commissioning procedure

1. Review the current FIRST, WPILib, and vendor release notes and compatibility matrix.
2. Install the matching WPILib distribution on programming machines.
3. Update manifests only to versions explicitly compatible with that WPILib release.
4. Record the Systemcore image, Driver Station, firmware, Tuner X, and both PhotonVision images.
5. Run build, native lifecycle simulation tests, and desktop simulation.
6. Complete bench CAN discovery, disabled checks, and the Robot Commissioning checklist.
7. Record hardware validation separately from desktop build results.
## Migration validation (November 17, 2026)

- Windows desktop: gradlew.bat build --no-daemon passed with GradleRIO/WPILib Alpha 8
  and the installed WPILib JDK 25.
- All four JUnit tests passed, including the native Phoenix/PhotonVision lifecycle test.
- The test covers teleop default priority, brake/release, heading reset, reserved operator
  controls, autonomous and utility transitions, disabled cancellation, and teleop reentry.
- Actual PhotonCamera construction exposed missing cscore/OpenCV runtime libraries.
  Adding the official Alpha 8 CameraServer manifest resolved the native load failure.
- No physical coprocessors were connected during desktop testing; PhotonVision's
  disconnected-camera diagnostics are expected. Live frames and pose fusion remain unverified.
- CI now runs the build and tests on Linux and Windows.
- No deployment or hardware motion testing was performed. The CAN assignments, swerve constants,
  camera names/transforms, controller ports, and hardware verification gates are unchanged.
