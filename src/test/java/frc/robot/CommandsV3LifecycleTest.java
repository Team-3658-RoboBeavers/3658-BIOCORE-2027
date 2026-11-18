package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import com.ctre.phoenix6.signals.ControlModeValue;
import frc.robot.auto.ChoreoPaths;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.wpilib.backend.NetworkTablesTunableBackend;
import org.wpilib.command3.Command;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.SchedulerEvent;
import org.wpilib.driverstation.internal.DriverStationBackend;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.hardware.hal.RobotMode;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.tunable.TunableRegistry;
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
    TunableRegistry.reset();
    var tunableBackend = new NetworkTablesTunableBackend(NetworkTableInstance.getDefault(), "/Tunables");
    TunableRegistry.registerBackend("", tunableBackend);
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

      var selection = NetworkTableInstance.getDefault()
          .getTable("Tunables/Autonomous/Routine").getEntry("selected/tune");
      selection.setString("Simulation: 0.25 m straight");
      TunableRegistry.update();
      assertEquals("Autonomous.Choreo[SimulationStraight]", container.getAutonomousCommand().name());
      selection.setString("Unknown routine");
      TunableRegistry.update();
      assertEquals("Autonomous.HoldStopped", container.getAutonomousCommand().name());

      // Run the real v3 follower against Phoenix's desktop physics, then verify cleanup.
      var trajectory = ChoreoPaths.load("SimulationStraight");
      var messages = new ArrayList<String>();
      var completed = new AtomicBoolean(false);
      var follow = drivetrain.followChoreo(
          trajectory, true, true, messages::add, () -> completed.set(true));
      assertTrue(scheduler.schedule(follow).successful());
      double deadline = Timer.getMonotonicTimestamp() + 4;
      while (scheduler.isScheduledOrRunning(follow) && Timer.getMonotonicTimestamp() < deadline) {
        tick(container);
        Timer.delay(0.02);
      }
      assertTrue(completed.get(), messages.toString());
      assertFalse(scheduler.isScheduledOrRunning(follow));
      awaitMotorModes(drivetrain, true);

      // Cancel mid-path. Cancellation must neutralize the motors and never signal success.
      completed.set(false);
      follow = drivetrain.followChoreo(
          trajectory, true, true, messages::add, () -> completed.set(true));
      assertTrue(scheduler.schedule(follow).successful());
      tick(container);
      Timer.delay(0.05);
      tick(container);
      scheduler.cancel(follow);
      assertFalse(completed.get());
      awaitMotorModes(drivetrain, true);

      // Own the drivetrain for the full routine and cancel its nested child as a unit.
      selection.setString("Simulation: 0.25 m straight");
      TunableRegistry.update();
      var routine = container.getAutonomousCommand();
      assertTrue(routine.requires(drivetrain));
      assertTrue(scheduler.schedule(routine).successful());
      tick(container);
      assertTrue(scheduler.getRunningCommands().stream()
          .anyMatch(command -> command.name().startsWith("Swerve.Choreo[")));
      scheduler.cancel(routine);
      tick(container);
      assertFalse(scheduler.isScheduledOrRunning(routine));
      assertFalse(scheduler.getRunningCommands().stream()
          .anyMatch(command -> command.name().startsWith("Swerve.Choreo[")));
      awaitMotorModes(drivetrain, true);

      // Resume the scheduler after a stopped path's deadline. Being at the goal is not
      // success when the deadline has already passed.
      var stationary = new Trajectory<>("Deadline",
          List.of(
              new SwerveSample(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new double[4], new double[4]),
              new SwerveSample(0.1, 0, 0, 0, 0, 0, 0, 0, 0, 0, new double[4], new double[4])),
          List.of(0), List.of());
      completed.set(false);
      messages.clear();
      var late = drivetrain.followChoreo(
          stationary, true, true, messages::add, () -> completed.set(true));
      assertTrue(scheduler.schedule(late).successful());
      scheduler.run();
      assertTrue(scheduler.isScheduledOrRunning(late));
      Timer.delay(stationary.getTotalTime() + Constants.Autonomous.SETTLE_SECONDS + 0.15);
      tick(container);
      assertFalse(completed.get());
      assertTrue(messages.stream().anyMatch(message -> message.contains("endpoint timeout")),
          messages.toString());
      awaitMotorModes(drivetrain, true);

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
      TunableRegistry.reset();
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
    TunableRegistry.update();
    // Default commands may be queued for the following scheduler iteration.
    for (int i = 0; i < 3; i++) {
      container.updateInputs();
      scheduler.run();
    }
  }

  private void awaitMotorModes(CommandSwerveDrivetrain drivetrain, boolean neutral) {
    double deadline = Timer.getMonotonicTimestamp() + 2.0;
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
    } while (Timer.getMonotonicTimestamp() < deadline);
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
