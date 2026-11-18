# Robot Commissioning Checklist

Do not mark this project deployable until every applicable item is checked, dated, and reviewed by
the team's electrical, mechanical, and programming leads.

## Software baseline

- [ ] Record WPILib, GradleRIO, Systemcore OS, Driver Station, Phoenix API, Phoenix firmware,
      Tuner X, PhotonVision, and PhotonLib versions.
- [ ] Confirm the versions are explicitly compatible in current vendor documentation.
- [ ] Run `gradlew build` and archive the successful CI run.
- [ ] Run desktop simulation and inspect for exceptions.

## Mechanical measurements

- [ ] Confirm all four modules are SDS MK5i with the L2 gear set actually installed.
- [ ] Confirm 4-inch wheel type and measure effective wheel radius under robot weight.
- [ ] Measure front-left to front-right module-center distance (trackwidth).
- [ ] Measure front-left to back-left module-center distance (wheelbase).
- [ ] Determine the MK5i coupling ratio required by Phoenix.
- [ ] Enter measured mass and inertias if simulation accuracy is needed.

## Phoenix Tuner X generation

- [ ] Discover and name all 13 `CAN_S0` devices.
- [ ] Confirm proposed IDs or update code/docs to match the approved inventory.
- [ ] Update every device to compatible firmware.
- [ ] Generate a fresh swerve project/constants file using the physical robot.
- [ ] Replace `generated/TunerConstants.java`; do not merely set the verification boolean.
- [ ] Record each CANcoder offset and verify absolute direction.
- [ ] Verify drive and steer inversions one module at a time.
- [ ] Validate X60 and X44 current limits with electrical leadership.
- [ ] Characterize drive/steer gains and measured speed at 12 V.

## First motion tests

- [ ] Put the robot securely on blocks with an accessible emergency stop.
- [ ] Test each module independently at very low output.
- [ ] Confirm positive X drives forward and positive Y drives left.
- [ ] Confirm positive rotation is counterclockwise.
- [ ] Verify the Pigeon2 yaw sign and field-centric heading reset.
- [ ] Test brake behavior and disabled behavior.
- [ ] Hold A while pressing heading reset; verify the brake remains active.
- [ ] Repeatedly disable and enable with teleop still selected; verify one response per button press.
- [ ] Verify drive and steer motors neutralize when requests exit and utility mode is selected.
- [ ] Perform slow robot-centric floor tests before field-centric driving.
- [ ] Recheck fasteners, wheel tread, wiring, temperatures, and logs.

## Vision

- [ ] Complete every step in `PhotonVision-Setup.md`.
- [ ] Load the official 2027 field layout.
- [ ] Measure and enter all four camera transforms.
- [ ] Validate each camera alone, then both processors together.
- [ ] Add pose rejection rules and uncertainty tuning.
- [ ] Verify pose fusion while stationary, moving, rotating, and losing camera connections.

## Unfinished mechanisms

- [ ] Select intake hardware and document all `CAN_S1` IDs, sensors, gearing, limits, and safe states.
- [ ] Select indexer hardware and document all `CAN_S2` IDs, sensors, gearing, limits, and jam logic.
- [ ] Design the scoring mechanism on `CAN_S3`.
- [ ] Inventory any auxiliary devices on `CAN_S4`.
- [ ] Add operator bindings only after commands have interruption behavior and safe end actions.

## Release decision

- [ ] Remove or resolve all commissioning TODOs that affect installed hardware.
- [ ] Complete a code review by at least one student and one mentor.
- [ ] Tag the first hardware-verified commit and record the robot configuration it matches.
