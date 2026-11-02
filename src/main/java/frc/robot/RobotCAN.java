package frc.robot;

import com.ctre.phoenix6.CANBus;
import org.wpilib.hardware.bus.CANPort;

/** One named object per Systemcore CAN port. Device IDs only need to be unique within a port. */
public final class RobotCAN {
  public static final CANBus SWERVE = new CANBus(CANPort.CAN_S0);
  public static final CANBus INTAKE = new CANBus(CANPort.CAN_S1);
  public static final CANBus INDEXER = new CANBus(CANPort.CAN_S2);
  public static final CANBus SCORING = new CANBus(CANPort.CAN_S3);
  public static final CANBus AUXILIARY = new CANBus(CANPort.CAN_S4);

  private RobotCAN() {}
}
