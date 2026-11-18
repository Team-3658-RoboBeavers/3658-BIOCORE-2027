// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.framework.TimedRobot;

public class Robot extends TimedRobot {
  private final RobotContainer container = new RobotContainer();
  private final Scheduler scheduler = Scheduler.getDefault();
  private Command autonomousCommand;

  @Override
  public void robotPeriodic() {
    // Mechanisms no longer receive automatic Subsystem.periodic() callbacks in v3.
    container.updateInputs();
    scheduler.run();
  }

  @Override
  public void autonomousInit() {
    autonomousCommand = container.getAutonomousCommand();
    scheduler.schedule(autonomousCommand);
  }

  @Override
  public void autonomousExit() {
    if (autonomousCommand != null) {
      scheduler.cancel(autonomousCommand);
    }
  }

  @Override
  public void utilityInit() {
    scheduler.cancelAll();
  }

  @Override
  public void close() {
    scheduler.cancelAll();
    container.close();
    super.close();
  }
}
