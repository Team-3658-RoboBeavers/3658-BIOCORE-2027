package frc.robot;

import static org.wpilib.units.Units.MetersPerSecond;
import static org.wpilib.units.Units.RadiansPerSecond;
import static org.wpilib.units.Units.RotationsPerSecond;

import com.ctre.phoenix6.swerve.SwerveModule.DriveRequestType;
import com.ctre.phoenix6.swerve.SwerveRequest;
import frc.robot.auto.ChoreoAutos;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import frc.robot.subsystems.IndexerSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.VisionSubsystem;
import org.wpilib.command3.Command;
import org.wpilib.command3.button.CommandXboxController;
import org.wpilib.command3.button.RobotModeTriggers;
import org.wpilib.math.util.MathUtil;

public class RobotContainer implements AutoCloseable {
  private static final double MAX_SPEED_METERS_PER_SECOND =
      MetersPerSecond.of(5.0).in(MetersPerSecond);
  private static final double MAX_ANGULAR_RATE_RADIANS_PER_SECOND =
      RotationsPerSecond.of(0.75).in(RadiansPerSecond);

  private final CommandXboxController driver =
      new CommandXboxController(Constants.Operator.DRIVER_PORT);
  @SuppressWarnings("unused")
  private final CommandXboxController operator =
      new CommandXboxController(Constants.Operator.OPERATOR_PORT);

  private final CommandSwerveDrivetrain drivetrain = TunerConstants.createDrivetrain();
  private final VisionSubsystem vision = new VisionSubsystem();
  @SuppressWarnings("unused")
  private final IntakeSubsystem intake = new IntakeSubsystem();
  @SuppressWarnings("unused")
  private final IndexerSubsystem indexer = new IndexerSubsystem();

  private final SwerveRequest.FieldCentric driveRequest =
      new SwerveRequest.FieldCentric().withDriveRequestType(DriveRequestType.OpenLoopVoltage);
  private final SwerveRequest.SwerveDriveBrake brakeRequest =
      new SwerveRequest.SwerveDriveBrake();

  private final ChoreoAutos autos = new ChoreoAutos(drivetrain, this::holdStopped);

  public RobotContainer() {
    drivetrain.setDefaultCommand(drivetrain.idle());
    // One enabled teleop session owns all teleop bindings/defaults. Its command scope also
    // ends on disable, even when the Driver Station leaves the selected robot mode unchanged.
    RobotModeTriggers.teleop()
        .whileTrue(
            Command.noRequirements(
                    coroutine -> {
                      configureTeleop();
                      coroutine.park();
                    })
                .named("Controls.Teleop"));
  }

  private void configureTeleop() {
    drivetrain.setDefaultCommand(
        drivetrain.applyRequest(
            () ->
                driveRequest
                    .withVelocityX(-deadband(driver.getLeftY()) * MAX_SPEED_METERS_PER_SECOND)
                    .withVelocityY(-deadband(driver.getLeftX()) * MAX_SPEED_METERS_PER_SECOND)
                    .withRotationalRate(
                        -deadband(driver.getRightX()) * MAX_ANGULAR_RATE_RADIANS_PER_SECOND),
            "Swerve.TeleopDrive",
            Command.LOWEST_PRIORITY));

    driver.a()
        .whileTrue(
            drivetrain.applyRequest(() -> brakeRequest, "Swerve.Brake", Command.DEFAULT_PRIORITY + 1));
    driver.leftBumper().onTrue(drivetrain.resetFieldHeading());
  }

  /** Poll all four cameras before commands run, including while disabled. */
  public void updateInputs() {
    vision.updateInputs();
  }

  @Override
  public void close() {
    autos.close();
    vision.close();
    drivetrain.close();
  }

  private static double deadband(double input) {
    return MathUtil.applyDeadband(input, Constants.Operator.DEADBAND);
  }

  public Command getAutonomousCommand() {
    return autos.selectedCommand();
  }

  private Command holdStopped() {
    return drivetrain.applyRequest(
        () -> brakeRequest, "Autonomous.HoldStopped", Command.DEFAULT_PRIORITY);
  }
}
