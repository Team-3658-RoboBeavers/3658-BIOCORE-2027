package frc.robot;

import static org.wpilib.units.Units.Inches;
import static org.wpilib.units.Units.Rotations;

import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.geometry.Translation3d;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.Distance;

/** Values that are not produced by Phoenix Tuner X or mechanism-specific classes. */
public final class Constants {
  private Constants() {}

  public static final class Operator {
    public static final int DRIVER_PORT = 0;
    public static final int OPERATOR_PORT = 1;
    public static final double DEADBAND = 0.08;

    private Operator() {}
  }

  public static final class Autonomous {
    /** Separate release gate: drivetrain commissioning alone does not qualify autonomous motion. */
    public static final boolean PATH_FOLLOWING_VERIFIED = false;
    public static final boolean FORCE_FEEDFORWARD_VERIFIED = false;
    // Conservative initial limits; replace only with measured/approved limits and regenerated paths.
    public static final double MAX_SPEED_MPS = 0.5;
    public static final double MAX_OMEGA_RAD_PER_SEC = 0.5;
    public static final double MAX_DURATION_SECONDS = 15.0;
    public static final double TRANSLATION_KP = 1.5;
    public static final double HEADING_KP = 3.0;
    public static final double POSITION_TOLERANCE_METERS = 0.10;
    public static final double HEADING_TOLERANCE_RADIANS = 0.10;
    public static final double SETTLE_SECONDS = 0.5;
    public static final double MAX_TRACKING_ERROR_METERS = 0.75;
    public static final double MAX_ODOMETRY_AGE_SECONDS = 0.25;
    private Autonomous() {}
  }

  public static final class Vision {
    public static final String FRONT_NAME = "3658-front";
    public static final String REAR_NAME = "3658-rear";
    public static final String LEFT_NAME = "3658-left";
    public static final String RIGHT_NAME = "3658-right";

    /**
     * Safety gate. Keep false until the official 2027 field layout and every measured camera
     * transform have been installed and validated.
     */
    public static final boolean POSE_FUSION_VERIFIED = false;

    // TODO(commissioning): replace every zero transform with tape-measured translation and a
    // calibrated rotation. Coordinates follow WPILib: +X forward, +Y left, +Z up.
    public static final Transform3d ROBOT_TO_FRONT = unknownTransform();
    public static final Transform3d ROBOT_TO_REAR = unknownTransform();
    public static final Transform3d ROBOT_TO_LEFT = unknownTransform();
    public static final Transform3d ROBOT_TO_RIGHT = unknownTransform();

    private static Transform3d unknownTransform() {
      return new Transform3d(new Translation3d(), new Rotation3d());
    }

    private Vision() {}
  }

  public static final class Swerve {
    public static final int PIGEON_ID = 0;

    // Proposed unique IDs. Confirm in Phoenix Tuner before enabling real hardware.
    public static final Module FRONT_LEFT = new Module(1, 2, 3, Rotations.of(0));
    public static final Module FRONT_RIGHT = new Module(4, 5, 6, Rotations.of(0));
    public static final Module BACK_LEFT = new Module(7, 8, 9, Rotations.of(0));
    public static final Module BACK_RIGHT = new Module(10, 11, 12, Rotations.of(0));

    // TODO(commissioning): measure center-to-center module spacing on the finished chassis.
    public static final Distance HALF_WHEELBASE = Inches.of(10);
    public static final Distance HALF_TRACKWIDTH = Inches.of(10);

    /** Derived from the published MK5i L2 gear tooth counts; verify the installed gear set. */
    public static final double DRIVE_GEAR_RATIO_L2 = (54.0 / 14.0) * (25.0 / 32.0) * (30.0 / 15.0);
    public static final double STEER_GEAR_RATIO = 26.0;
    public static final Distance WHEEL_RADIUS = Inches.of(2.0);

    /**
     * Keep false until IDs, offsets, dimensions, gearing, inversions, gains, firmware, and current
     * limits have all passed the commissioning checklist.
     */
    public static final boolean HARDWARE_CONFIGURATION_VERIFIED = false;

    private Swerve() {}

    public record Module(int driveId, int steerId, int encoderId, Angle encoderOffset) {}
  }
}
