package frc.robot;

import static org.wpilib.units.Units.MetersPerSecond;
import static org.wpilib.units.Units.RadiansPerSecond;
import static org.wpilib.units.Units.RotationsPerSecond;

import com.ctre.phoenix6.swerve.SwerveModule.DriveRequestType;
import com.ctre.phoenix6.swerve.SwerveRequest;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import frc.robot.subsystems.IndexerSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.VisionSubsystem;
import org.wpilib.command2.Command;
import org.wpilib.command2.Commands;
import org.wpilib.command2.button.CommandXboxController;
import org.wpilib.math.util.MathUtil;

public class RobotContainer {
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
  @SuppressWarnings("unused")
  private final VisionSubsystem vision = new VisionSubsystem();
  @SuppressWarnings("unused")
  private final IntakeSubsystem intake = new IntakeSubsystem();
  @SuppressWarnings("unused")
  private final IndexerSubsystem indexer = new IndexerSubsystem();

  private final SwerveRequest.FieldCentric driveRequest =
      new SwerveRequest.FieldCentric().withDriveRequestType(DriveRequestType.OpenLoopVoltage);
  private final SwerveRequest.SwerveDriveBrake brakeRequest =
      new SwerveRequest.SwerveDriveBrake();

  public RobotContainer() {
    configureBindings();
  }

  private void configureBindings() {
    drivetrain.setDefaultCommand(
        drivetrain.applyRequest(
            () ->
                driveRequest
                    .withVelocityX(-deadband(driver.getLeftY()) * MAX_SPEED_METERS_PER_SECOND)
                    .withVelocityY(-deadband(driver.getLeftX()) * MAX_SPEED_METERS_PER_SECOND)
                    .withRotationalRate(
                        -deadband(driver.getRightX()) * MAX_ANGULAR_RATE_RADIANS_PER_SECOND)));

    driver.a().whileTrue(drivetrain.applyRequest(() -> brakeRequest));
    driver.leftBumper().onTrue(drivetrain.runOnce(drivetrain::seedFieldCentric));
  }

  private static double deadband(double input) {
    return MathUtil.applyDeadband(input, Constants.Operator.DEADBAND);
  }

  public Command getAutonomousCommand() {
    // TODO: add 2027 autonomous routines after the game and path library compatibility are known.
    return Commands.none();
  }
}
