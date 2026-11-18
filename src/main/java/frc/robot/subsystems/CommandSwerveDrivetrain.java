package frc.robot.subsystems;

import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.swerve.SwerveDrivetrain;
import com.ctre.phoenix6.swerve.SwerveDrivetrainConstants;
import com.ctre.phoenix6.swerve.SwerveModuleConstants;
import com.ctre.phoenix6.swerve.SwerveRequest;
import java.util.function.Supplier;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.numbers.N1;
import org.wpilib.math.numbers.N3;
import org.wpilib.system.Notifier;
import org.wpilib.system.RobotController;

/** Minimal command-based wrapper around the Phoenix 6 swerve drivetrain. */
public class CommandSwerveDrivetrain extends SwerveDrivetrain<TalonFX, TalonFX, CANcoder>
    implements Mechanism {
  private static final double SIM_LOOP_PERIOD_SECONDS = 0.004;
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
        .whenExited(() -> setControl(new SwerveRequest.Idle()))
        .named(name);
  }

  /** Runs once while owning the drivetrain, so heading reset cannot race another command. */
  public Command resetFieldHeading() {
    return run(coroutine -> seedFieldCentric()).named("Swerve.ResetFieldHeading");
  }

  @Override
  public Command idle() {
    return runRepeatedly(() -> setControl(new SwerveRequest.Idle()))
        .withPriority(Command.LOWEST_PRIORITY)
        .named("Swerve.Idle");
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
