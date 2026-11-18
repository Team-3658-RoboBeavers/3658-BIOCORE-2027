package frc.robot.auto;

import choreo.trajectory.SwerveSample;
import frc.robot.Constants;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.kinematics.ChassisVelocities;
import org.wpilib.math.util.MathUtil;

/** Field-relative velocity feedforward plus proportional pose feedback; gains need commissioning. */
public final class ChoreoController {
  private ChoreoController() {}

  public static ChassisVelocities calculate(Pose2d pose, SwerveSample sample) {
    double vx = sample.vx + Constants.Autonomous.TRANSLATION_KP * (sample.x - pose.getX());
    double vy = sample.vy + Constants.Autonomous.TRANSLATION_KP * (sample.y - pose.getY());
    double omega = sample.omega + Constants.Autonomous.HEADING_KP
        * MathUtil.angleModulus(sample.heading - pose.getRotation().getRadians());
    double scale = Math.max(1.0, Math.hypot(vx, vy) / Constants.Autonomous.MAX_SPEED_MPS);
    return new ChassisVelocities(vx / scale, vy / scale,
        Math.clamp(omega, -Constants.Autonomous.MAX_OMEGA_RAD_PER_SEC,
            Constants.Autonomous.MAX_OMEGA_RAD_PER_SEC));
  }

  public static boolean atGoal(Pose2d pose, SwerveSample goal) {
    return pose.getTranslation().getDistance(goal.getPose().getTranslation())
            <= Constants.Autonomous.POSITION_TOLERANCE_METERS
        && Math.abs(MathUtil.angleModulus(goal.heading - pose.getRotation().getRadians()))
            <= Constants.Autonomous.HEADING_TOLERANCE_RADIANS;
  }

  public static boolean finite(Pose2d pose) {
    return Double.isFinite(pose.getX()) && Double.isFinite(pose.getY())
        && Double.isFinite(pose.getRotation().getRadians());
  }
}
