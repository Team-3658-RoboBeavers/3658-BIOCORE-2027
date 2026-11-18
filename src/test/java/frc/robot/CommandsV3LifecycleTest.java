package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.driverstation.internal.DriverStationBackend;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.hardware.hal.RobotMode;
import org.wpilib.simulation.DriverStationSim;
import org.wpilib.simulation.XboxControllerSim;

/** Exercises the actual Phoenix wrapper and controller bindings with desktop native libraries. */
class CommandsV3LifecycleTest {
  private final Scheduler scheduler = Scheduler.getDefault();

  @Test
  void controlsAreScopedAndDrivetrainStopsAcrossModeTransitions() {
    assertTrue(HAL.initialize());
    DriverStationBackend.clearUserProgramStarted();
    DriverStationSim.resetData();
    setMode(RobotMode.UNKNOWN, false);
    try (var container = new RobotContainer()) {
      // TimedRobot.startCompetition() normally marks construction complete before mode init.
      DriverStationBackend.observeUserProgramStarting();
      var driver = new XboxControllerSim(Constants.Operator.DRIVER_PORT);
      var operator = new XboxControllerSim(Constants.Operator.OPERATOR_PORT);
      tick(container);
      assertTrue(runningNames().isEmpty(), "No actuator commands may run disabled");

      setMode(RobotMode.TELEOPERATED, true);
      container.configureTeleop();
      tick(container);
      assertEquals(Set.of("Swerve.TeleopDrive"), runningNames());
      var drive = scheduler.getRunningCommands().iterator().next();
      assertEquals(Command.LOWEST_PRIORITY, drive.priority());

      operator.setAButton(true);
      tick(container);
      assertEquals(Set.of("Swerve.TeleopDrive"), runningNames(), "Operator stays reserved");

      driver.setAButton(true);
      tick(container);
      assertEquals(Set.of("Swerve.Brake"), runningNames());
      var brake = scheduler.getRunningCommands().iterator().next();
      assertEquals(drive.requirements(), brake.requirements());
      assertEquals(1, brake.requirements().size());

      driver.setAButton(false);
      tick(container);
      assertEquals(Set.of("Swerve.TeleopDrive"), runningNames());

      driver.setLeftBumperButton(true);
      tick(container);
      driver.setLeftBumperButton(false);
      tick(container);
      assertEquals(Set.of("Swerve.TeleopDrive"), runningNames(), "Heading reset releases ownership");

      setMode(RobotMode.AUTONOMOUS, true);
      // Hold driver buttons through autonomous: teleop bindings must have been removed.
      driver.setAButton(true);
      tick(container);
      assertEquals(Set.of("Swerve.Idle"), runningNames());
      var autonomous = container.getAutonomousCommand();
      assertTrue(scheduler.schedule(autonomous).successful());
      tick(container);
      assertEquals(Set.of("Autonomous.HoldStopped"), runningNames());
      scheduler.cancel(autonomous);

      setMode(RobotMode.UTILITY, true);
      tick(container);
      assertEquals(Set.of("Swerve.Idle"), runningNames());

      driver.setAButton(false);
      setMode(RobotMode.TELEOPERATED, true);
      container.configureTeleop();
      tick(container);
      assertEquals(Set.of("Swerve.TeleopDrive"), runningNames(), "Teleop can be reentered");
      setMode(RobotMode.TELEOPERATED, false);
      tick(container);
      assertTrue(runningNames().isEmpty(), "Disabled cancels actuator commands");
      assertFalse(scheduler.isScheduledOrRunning(drive));
    } finally {
      scheduler.cancelAll();
      setMode(RobotMode.UNKNOWN, false);
      scheduler.run(); // Remove the teleop scope and restore the global idle default.
      DriverStationBackend.clearUserProgramStarted();
    }
  }

  private void setMode(RobotMode mode, boolean enabled) {
    DriverStationSim.setRobotMode(mode);
    DriverStationSim.setDsAttached(true);
    DriverStationSim.setEnabled(enabled);
    DriverStationSim.notifyNewData();
    assertEquals(mode, org.wpilib.driverstation.RobotState.getRobotMode());
    assertEquals(0, org.wpilib.driverstation.RobotState.getOpModeId(), "Plain TimedRobot scope");
  }

  private void tick(RobotContainer container) {
    DriverStationSim.notifyNewData();
    // Default commands may be queued for the following scheduler iteration.
    for (int i = 0; i < 3; i++) {
      container.updateInputs();
      scheduler.run();
    }
  }

  private Set<String> runningNames() {
    return scheduler.getRunningCommands().stream().map(Command::name).collect(Collectors.toSet());
  }
}
