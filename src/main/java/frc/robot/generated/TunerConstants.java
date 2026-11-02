package frc.robot.generated;

import static org.wpilib.units.Units.Amps;
import static org.wpilib.units.Units.KilogramSquareMeters;
import static org.wpilib.units.Units.MetersPerSecond;
import static org.wpilib.units.Units.Volts;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.StaticFeedforwardSignValue;
import com.ctre.phoenix6.swerve.SwerveDrivetrainConstants;
import com.ctre.phoenix6.swerve.SwerveModuleConstants;
import com.ctre.phoenix6.swerve.SwerveModuleConstants.ClosedLoopOutputType;
import com.ctre.phoenix6.swerve.SwerveModuleConstants.DriveMotorArrangement;
import com.ctre.phoenix6.swerve.SwerveModuleConstants.SteerFeedbackType;
import com.ctre.phoenix6.swerve.SwerveModuleConstants.SteerMotorArrangement;
import com.ctre.phoenix6.swerve.SwerveModuleConstantsFactory;
import frc.robot.Constants;
import frc.robot.RobotCAN;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import org.wpilib.framework.RobotBase;

/**
 * Provisional Phoenix constants for four SDS MK5i L2 modules with X60 drive motors, X44 steer
 * motors, CANcoders, and a Pigeon2 on Systemcore CAN_S0.
 *
 * <p>Replace this file with Phoenix Tuner X output after the assembled drivetrain is available.
 */
public final class TunerConstants {
  private TunerConstants() {}

  private static final Slot0Configs STEER_GAINS =
      new Slot0Configs()
          .withKP(100)
          .withKI(0)
          .withKD(0.5)
          .withKS(0.1)
          .withKV(1.91)
          .withKA(0)
          .withStaticFeedforwardSign(StaticFeedforwardSignValue.UseClosedLoopSign);

  private static final Slot0Configs DRIVE_GAINS =
      new Slot0Configs().withKP(0.2).withKI(0).withKD(0).withKS(0).withKV(0.124);

  private static final TalonFXConfiguration DRIVE_CONFIG =
      new TalonFXConfiguration()
          .withCurrentLimits(
              new CurrentLimitsConfigs()
                  .withSupplyCurrentLimit(Amps.of(60))
                  .withSupplyCurrentLimitEnable(true));

  private static final TalonFXConfiguration STEER_CONFIG =
      new TalonFXConfiguration()
          .withCurrentLimits(
              new CurrentLimitsConfigs()
                  .withStatorCurrentLimit(Amps.of(40))
                  .withStatorCurrentLimitEnable(true));

  public static final SwerveDrivetrainConstants DRIVETRAIN_CONSTANTS =
      new SwerveDrivetrainConstants()
          .withNetwork(RobotCAN.SWERVE)
          .withPigeon2Id(Constants.Swerve.PIGEON_ID);

  private static final SwerveModuleConstantsFactory<
          TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>
      FACTORY =
          new SwerveModuleConstantsFactory<
                  TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>()
              .withDriveMotorGearRatio(Constants.Swerve.DRIVE_GEAR_RATIO_L2)
              .withSteerMotorGearRatio(Constants.Swerve.STEER_GEAR_RATIO)
              // TODO(commissioning): determine MK5i azimuth/drive coupling and replace zero.
              .withCouplingGearRatio(0)
              .withWheelRadius(Constants.Swerve.WHEEL_RADIUS)
              .withSteerMotorGains(STEER_GAINS)
              .withDriveMotorGains(DRIVE_GAINS)
              .withSteerMotorClosedLoopOutput(ClosedLoopOutputType.Voltage)
              .withDriveMotorClosedLoopOutput(ClosedLoopOutputType.Voltage)
              .withSlipCurrent(Amps.of(120))
              // TODO(commissioning): measure free speed at 12 V; this is a conservative placeholder.
              .withSpeedAt12Volts(MetersPerSecond.of(5.0))
              .withDriveMotorType(DriveMotorArrangement.TalonFX_Integrated)
              .withSteerMotorType(SteerMotorArrangement.TalonFX_Integrated)
              .withFeedbackSource(SteerFeedbackType.FusedCANcoder)
              .withDriveMotorInitialConfigs(DRIVE_CONFIG)
              .withSteerMotorInitialConfigs(STEER_CONFIG)
              .withEncoderInitialConfigs(new CANcoderConfiguration())
              .withSteerInertia(KilogramSquareMeters.of(0.01))
              .withDriveInertia(KilogramSquareMeters.of(0.035))
              .withSteerFrictionVoltage(Volts.of(0.2))
              .withDriveFrictionVoltage(Volts.of(0.2));

  public static final SwerveModuleConstants<
          TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>
      FRONT_LEFT =
          create(
              Constants.Swerve.FRONT_LEFT,
              Constants.Swerve.HALF_WHEELBASE,
              Constants.Swerve.HALF_TRACKWIDTH,
              false);

  public static final SwerveModuleConstants<
          TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>
      FRONT_RIGHT =
          create(
              Constants.Swerve.FRONT_RIGHT,
              Constants.Swerve.HALF_WHEELBASE,
              Constants.Swerve.HALF_TRACKWIDTH.unaryMinus(),
              true);

  public static final SwerveModuleConstants<
          TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>
      BACK_LEFT =
          create(
              Constants.Swerve.BACK_LEFT,
              Constants.Swerve.HALF_WHEELBASE.unaryMinus(),
              Constants.Swerve.HALF_TRACKWIDTH,
              false);

  public static final SwerveModuleConstants<
          TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>
      BACK_RIGHT =
          create(
              Constants.Swerve.BACK_RIGHT,
              Constants.Swerve.HALF_WHEELBASE.unaryMinus(),
              Constants.Swerve.HALF_TRACKWIDTH.unaryMinus(),
              true);

  private static SwerveModuleConstants<
          TalonFXConfiguration, TalonFXConfiguration, CANcoderConfiguration>
      create(
          Constants.Swerve.Module module,
          org.wpilib.units.measure.Distance x,
          org.wpilib.units.measure.Distance y,
          boolean driveInverted) {
    // Steer and encoder inversion values are placeholders until Tuner X commissioning.
    return FACTORY.createModuleConstants(
        module.steerId(),
        module.driveId(),
        module.encoderId(),
        module.encoderOffset(),
        x,
        y,
        driveInverted,
        true,
        false);
  }

  public static CommandSwerveDrivetrain createDrivetrain() {
    if (RobotBase.isReal() && !Constants.Swerve.HARDWARE_CONFIGURATION_VERIFIED) {
      throw new IllegalStateException(
          "Real drivetrain startup blocked: complete commissioning and replace provisional Tuner constants.");
    }
    return new CommandSwerveDrivetrain(
        DRIVETRAIN_CONSTANTS, FRONT_LEFT, FRONT_RIGHT, BACK_LEFT, BACK_RIGHT);
  }
}
