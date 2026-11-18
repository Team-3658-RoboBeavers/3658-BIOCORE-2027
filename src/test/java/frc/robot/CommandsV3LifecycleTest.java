package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctre.phoenix6.signals.ControlModeValue;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.SchedulerEvent;
import org.wpilib.driverstation.internal.DriverStationBackend;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.hardware.hal.RobotMode;
import org.wpilib.simulation.DriverStationSim;
import org.wpilib.simulation.XboxControllerSim;
import org.wpilib.system.Timer;

/** Exercises the actual Phoenix wrapper and controller bindings with desktop native libraries. */
class CommandsV3LifecycleTest {
  private final Scheduler scheduler = Scheduler.getDefault();

  @Test
  void controlsAreScopedAndDrivetrainStopsAcrossModeTransitions() {
    assertTrue(HAL.initialize());
    DriverStationBackend.clearUserProgramStarted();
    DriverStationSim.resetData();
    setMode(RobotMode.UNKNOWN, false);
    var scheduled = new ArrayList<String>();
    scheduler.addEventListener(
        event -> {
          if (event instanceof SchedulerEvent.Scheduled e) {
            scheduled.add(e.command().name());
          }
        });
    try (var container = new RobotContainer()) {
      // TimedRobot.startCompetition() normally marks construction complete before mode init.
      DriverStationBackend.observeUserProgramStarting();
      var driver = new XboxControllerSim(Constants.Operator.DRIVER_PORT);
      var operator = new XboxControllerSim(Constants.Operator.OPERATOR_PORT);
      tick(container);
      assertTrue(runningNames().isEmpty(), "No actuator commands may run disabled");

      setMode(RobotMode.TELEOPERATED, true);
      tick(container);
      assertEquals(Set.of("Swerve.TeleopDrive"), runningNames());
      var drive = runningCommand("Swerve.TeleopDrive");
      assertEquals(Command.LOWEST_PRIORITY, drive.priority());
      var drivetrain = (CommandSwerveDrivetrain) drive.requirements().iterator().next();
      driver.setLeftY(-0.4);
      tick(container);
      awaitMotorModes(drivetrain, false);

      operator.setAButton(true);
      tick(container);
      assertEquals(Set.of("Swerve.TeleopDrive"), runningNames(), "Operator stays reserved");

      driver.setAButton(true);
      tick(container);
      assertEquals(Set.of("Swerve.Brake"), runningNames());
      var brake = runningCommand("Swerve.Brake");
      assertEquals(drive.requirements(), brake.requirements());
      assertEquals(1, brake.requirements().size());

      driver.setLeftBumperButton(true);
      tick(container);
      assertEquals(Set.of("Swerve.Brake"), runningNames(), "Heading reset must not release brake");
      driver.setLeftBumperButton(false);
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
      awaitMotorModes(drivetrain, true);
      var autonomous = container.getAutonomousCommand();
      assertTrue(scheduler.schedule(autonomous).successful());
      tick(container);
      assertEquals(Set.of("Autonomous.HoldStopped"), runningNames());
      scheduler.cancel(autonomous);

      setMode(RobotMode.UTILITY, true);
      tick(container);
      assertEquals(Set.of("Swerve.Idle"), runningNames());
      awaitMotorModes(drivetrain, true);

      driver.setAButton(false);
      setMode(RobotMode.TELEOPERATED, true);
      tick(container);
      assertEquals(Set.of("Swerve.TeleopDrive"), runningNames(), "Teleop can be reentered");
      setMode(RobotMode.TELEOPERATED, false);
      tick(container);
      assertTrue(runningNames().isEmpty(), "Disabled cancels actuator commands");
      assertFalse(scheduler.isScheduledOrRunning(drive));

      // Repeat disable/enable while the selected mode stays TELEOPERATED. Each A press must
      // schedule exactly one brake command, with no stale bindings from earlier enable periods.
      for (int cycle = 0; cycle < 3; cycle++) {
        setMode(RobotMode.TELEOPERATED, true);
        tick(container);
        assertEquals(Set.of("Swerve.TeleopDrive"), runningNames());
        long brakesBefore = scheduled.stream().filter("Swerve.Brake"::equals).count();
        driver.setAButton(true);
        tick(container);
        assertEquals(Set.of("Swerve.Brake"), runningNames());
        assertEquals(
            brakesBefore + 1, scheduled.stream().filter("Swerve.Brake"::equals).count(),
            "One brake binding per enable period");
        setMode(RobotMode.TELEOPERATED, false);
        tick(container);
        assertTrue(runningNames().isEmpty());
        driver.setAButton(false);
        tick(container);
      }
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

  private void awaitMotorModes(CommandSwerveDrivetrain drivetrain, boolean neutral) {
    double deadline = Timer.getTimestamp() + 2.0;
    boolean matching;
    do {
      matching = true;
      for (var module : drivetrain.getModules()) {
        var drive = module.getDriveMotor().getControlMode().refresh();
        var steer = module.getSteerMotor().getControlMode().refresh();
        matching &= drive.getStatus().isOK() && steer.getStatus().isOK();
        matching &= (drive.getValue() == ControlModeValue.NeutralOut) == neutral;
        matching &= (steer.getValue() == ControlModeValue.NeutralOut) == neutral;
      }
      if (matching) {
        return;
      }
      Timer.delay(0.01);
    } while (Timer.getTimestamp() < deadline);
    assertTrue(matching, neutral ? "All eight motors must be neutral" : "Drive must first be active");
  }

  private Command runningCommand(String name) {
    return scheduler.getRunningCommands().stream()
        .filter(command -> command.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private Set<String> runningNames() {
    return scheduler.getRunningCommands().stream()
        .filter(command -> !command.requirements().isEmpty())
        .map(Command::name)
        .collect(Collectors.toSet());
  }
}
