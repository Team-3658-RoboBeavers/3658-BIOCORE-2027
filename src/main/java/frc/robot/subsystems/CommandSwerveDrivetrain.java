package frc.robot.subsystems;

import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.swerve.SwerveDrivetrain;
import com.ctre.phoenix6.swerve.SwerveDrivetrainConstants;
import com.ctre.phoenix6.swerve.SwerveModuleConstants;
import com.ctre.phoenix6.swerve.SwerveRequest;
import frc.robot.Constants;
import frc.robot.auto.ChoreoController;
import frc.robot.auto.ChoreoPaths;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.driverstation.RobotState;
import org.wpilib.framework.RobotBase;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.system.Notifier;
import org.wpilib.system.RobotController;
import org.wpilib.system.Timer;

/** Minimal command-based wrapper around the Phoenix 6 swerve drivetrain. */
public class CommandSwerveDrivetrain extends SwerveDrivetrain<TalonFX, TalonFX, CANcoder>
    implements Mechanism {
  private static final double SIM_LOOP_PERIOD_SECONDS = 0.004;
  // A custom request replaces the previous odometry-thread request and explicitly neutralizes
  // both axes. Phoenix Idle alone does not change the modules' previously applied state.
  private final NeutralOut neutralOutput = new NeutralOut();
  private final SwerveRequest neutralRequest =
      (parameters, modules) -> {
        for (var module : modules) {
          module.apply(neutralOutput, neutralOutput);
        }
        return StatusCode.OK;
      };
  private Notifier simNotifier;
  private double lastSimTime;

  public CommandSwerveDrivetrain(
      SwerveDrivetrainConstants drivetrainConstants,
      SwerveModuleConstants<?, ?, ?>... modules) {
    super(TalonFX::new, TalonFX::new, CANcoder::new, drivetrainConstants, modules);
    if (Utils.isSimulation()) {
      startSimThread();
    }
  }

  public Command applyRequest(Supplier<SwerveRequest> request, String name, int priority) {
    return runRepeatedly(() -> setControl(request.get()))
        .withPriority(priority)
        .whenExited(() -> setControl(neutralRequest))
        .named(name);
  }

  /** Runs once while owning the drivetrain, so heading reset cannot race another command. */
  public Command resetFieldHeading() {
    return run(coroutine -> seedFieldCentric()).named("Swerve.ResetFieldHeading");
  }

  @Override
  public Command idle() {
    return runRepeatedly(() -> setControl(neutralRequest))
        .withPriority(Command.LOWEST_PRIORITY)
        .named("Swerve.Idle");
  }

  /**
   * Owns the drivetrain for one preloaded Choreo segment. No automatic field flipping:
   * callers choose an explicitly authored blue/red trajectory before starting the routine.
   * The success callback runs only after the endpoint tolerance is met.
   */
  public Command followChoreo(
      Trajectory<SwerveSample> trajectory, boolean resetAtStart, boolean simulationOnly,
      Consumer<String> report, Runnable onSuccess) {
    return run(coroutine -> {
          if (!RobotState.isAutonomousEnabled()
              || (RobotBase.isReal() && (simulationOnly
                  || !Constants.Swerve.HARDWARE_CONFIGURATION_VERIFIED
                  || !Constants.Autonomous.PATH_FOLLOWING_VERIFIED))) {
            report.accept("Blocked: autonomous enable and trajectory commissioning required");
            return;
          }
          try {
            ChoreoPaths.validate(trajectory);
          } catch (IllegalArgumentException ex) {
            report.accept("Blocked: " + ex.getMessage());
            return;
          }
          var initial = trajectory.getInitialPose(false).orElseThrow();
          var goal = trajectory.getFinalSample(false).orElseThrow();
          if (resetAtStart) resetPose(initial);
          var request = new SwerveRequest.ApplyFieldVelocity()
              .withDriveRequestType(com.ctre.phoenix6.swerve.SwerveModule.DriveRequestType.Velocity)
              .withForwardPerspective(SwerveRequest.ForwardPerspectiveValue.BlueAlliance);
          double start = Timer.getMonotonicTimestamp();
          report.accept("Running: " + trajectory.name());
          while (RobotState.isAutonomousEnabled()) {
            double elapsed = Timer.getMonotonicTimestamp() - start;
            // A late scheduler iteration must not report success after the deadline.
            if (ChoreoController.timedOut(elapsed, trajectory.getTotalTime())) {
              report.accept("Aborted: endpoint timeout in " + trajectory.name());
              return;
            }
            var state = getState();
            double age = Utils.getCurrentTimeSeconds() - state.Timestamp;
            if (!Double.isFinite(age) || age < 0
                || age > Constants.Autonomous.MAX_ODOMETRY_AGE_SECONDS) {
              report.accept("Aborted: stale drivetrain odometry");
              return;
            }
            var pose = state.Pose;
            if (!ChoreoController.finite(pose)) {
              report.accept("Aborted: invalid drivetrain pose");
              return;
            }
            var sample = trajectory.sampleAt(Math.min(elapsed, trajectory.getTotalTime()), false)
                .orElseThrow();
            try {
              ChoreoPaths.validateSample(sample);
            } catch (IllegalArgumentException ex) {
              report.accept("Aborted: " + ex.getMessage());
              return;
            }
            if (pose.getTranslation().getDistance(sample.getPose().getTranslation())
                > Constants.Autonomous.MAX_TRACKING_ERROR_METERS) {
              report.accept("Aborted: excessive tracking error in " + trajectory.name());
              return;
            }
            if (elapsed >= trajectory.getTotalTime() && ChoreoController.atGoal(pose, goal)) {
              report.accept("Completed: " + trajectory.name());
              onSuccess.run();
              return;
            }

            var velocity = ChoreoController.calculate(pose, sample);
            // Suppress force feedforward at the velocity cap; do not counteract limiting with
            // an unscaled optimizer force. Force use also needs separate physical-model approval.
            boolean forcesAllowed = Constants.Autonomous.FORCE_FEEDFORWARD_VERIFIED
                && Math.hypot(velocity.vx, velocity.vy) < Constants.Autonomous.MAX_SPEED_MPS - 1e-6
                && Math.abs(velocity.omega) < Constants.Autonomous.MAX_OMEGA_RAD_PER_SEC - 1e-6;
            setControl(request.withVelocity(velocity)
                .withWheelForceFeedforwardsX(forcesAllowed ? sample.moduleForcesX() : new double[4])
                .withWheelForceFeedforwardsY(forcesAllowed ? sample.moduleForcesY() : new double[4]));
            coroutine.yield();
          }
          report.accept("Canceled: autonomous no longer enabled");
        })
        .whenExited(() -> setControl(neutralRequest))
        .whenCanceled(() -> report.accept("Canceled: " + trajectory.name()))
        .named("Swerve.Choreo[" + trajectory.name() + "]");
  }

  public void acceptVisionMeasurement(
      org.wpilib.math.geometry.Pose2d pose,
      double timestampSeconds,
      Matrix<N3, N1> standardDeviations) {
    addVisionMeasurement(pose, timestampSeconds, standardDeviations);
  }

  @Override
  public void close() {
    if (simNotifier != null) {
      simNotifier.close();
      simNotifier = null;
    }
    super.close();
  }

  private void startSimThread() {
    lastSimTime = Utils.getCurrentTimeSeconds();
    simNotifier =
        new Notifier(
            () -> {
              double now = Utils.getCurrentTimeSeconds();
              updateSimState(now - lastSimTime, RobotController.getBatteryVoltage());
              lastSimTime = now;
            });
    simNotifier.startPeriodic(SIM_LOOP_PERIOD_SECONDS);
  }
}
