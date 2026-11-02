# Four-Camera PhotonVision Setup

## Assignment

| Processor | Camera position | PhotonVision name |
| --- | --- | --- |
| Orange Pi A | Front | `3658-front` |
| Orange Pi A | Rear | `3658-rear` |
| Orange Pi B | Left | `3658-left` |
| Orange Pi B | Right | `3658-right` |

The Orange Pis communicate over Ethernet. The cameras do not connect to a CAN bus.

## Bench setup

1. Install the same PhotonVision development image/version on both Orange Pi 5 processors. The
   processor application and the robot's PhotonLib must match.
2. Give each processor a unique, stable network identity. Record addresses in the team network
   sheet; camera discovery still uses the camera names above.
3. Connect and label one camera at a time. Confirm the correct physical USB camera receives the
   intended PhotonVision name.
4. Test front and rear simultaneously on processor A, then left and right on processor B.
5. Tune resolution, exposure, gain, and AprilTag settings on the final hardware. Start with the
   vendor's documented Orange Pi/OV9281 guidance, then measure latency and frame rate.
6. Calibrate every camera separately after the final lens is installed. Do not copy calibration
   between nominally identical cameras.

## Robot measurements still required

For each camera, measure the transform from the robot coordinate origin to the optical frame:

- X: forward positive
- Y: left positive
- Z: up positive
- roll, pitch, yaw using WPILib conventions

Enter those values in `Constants.Vision`. All four transforms are currently zero placeholders.

## Pose-fusion gate

`Constants.Vision.POSE_FUSION_VERIFIED` intentionally remains `false`. Enable fusion only after:

- the official 2027 AprilTag field layout is available and loaded;
- all four camera calibrations and robot-to-camera transforms are measured;
- timestamps and network latency are validated;
- implausible poses, high ambiguity, excessive tag distance, and out-of-field estimates are
  rejected;
- single-camera and multi-camera tests agree with known field measurements;
- disconnecting any camera does not disturb odometry or robot control.

The initial `VisionSubsystem` only verifies that frames arrive under all four configured names. Pose
estimation and drivetrain fusion are deliberately unfinished.
