package frc.robot.auto;

import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import frc.robot.Constants;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.wpilib.command3.Command;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;
import org.wpilib.framework.RobotBase;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StringPublisher;
import org.wpilib.tunable.Selectable;
import org.wpilib.tunable.Tunables;

/** V3 selection and composition; deliberately does not load choreo.auto's Commands v2 helpers. */
public final class ChoreoAutos implements AutoCloseable {
  public static final String CHOOSER_KEY = "Autonomous/Routine";
  private final CommandSwerveDrivetrain drivetrain;
  private final Supplier<Command> holdStopped;
  private final Selectable<Supplier<Command>> chooser = new Selectable<>();
  private final StringPublisher status = NetworkTableInstance.getDefault()
      .getTable("Autonomous").getStringTopic("status").publish();

  public ChoreoAutos(CommandSwerveDrivetrain drivetrain, Supplier<Command> holdStopped) {
    this.drivetrain = drivetrain;
    this.holdStopped = holdStopped;
    status.set("Ready: Hold stopped is the default; hardware paths require commissioning");
    chooser.addDefault("Hold stopped", () -> fallback("Holding stopped (default)"));
    if (RobotBase.isSimulation()) {
      try {
        var demo = List.of(ChoreoPaths.load("SimulationStraight"));
        chooser.add("Simulation: 0.25 m straight", () -> sequence("SimulationStraight", demo, true));
      } catch (RuntimeException ex) {
        status.set("Simulation example unavailable: " + ex.getMessage());
      }
    }
    // Register real routes here only after exporting/reviewing both alliance versions:
    // addAllianceRoutine("Example", List.of("Blue-Example"), List.of("Red-Example"));
    Tunables.publish(CHOOSER_KEY, chooser);
  }

  /** Preloads explicit routes while constructing the robot, with one reset at routine start. */
  public void addAllianceRoutine(String displayName, List<String> blueNames, List<String> redNames) {
    try {
      if (blueNames.isEmpty() || blueNames.size() != redNames.size()) {
        throw new IllegalArgumentException("Provide equal, non-empty blue/red segment lists");
      }
      var blue = blueNames.stream().map(ChoreoPaths::load).toList();
      var red = redNames.stream().map(ChoreoPaths::load).toList();
      validateSequence(blue);
      validateSequence(red);
      chooser.add(displayName, () -> {
        var selected = routesForAlliance(MatchState.getAlliance(), blue, red);
        if (selected.isEmpty()) return fallback("Blocked: alliance is unknown");
        if (RobotBase.isReal() && !Constants.Autonomous.PATH_FOLLOWING_VERIFIED) {
          return fallback("Blocked: autonomous trajectory commissioning is incomplete");
        }
        return sequence(displayName, selected.get(), false);
      });
    } catch (RuntimeException ex) {
      String error = "Blocked: " + displayName + ": " + ex.getMessage();
      chooser.add(displayName, () -> fallback(error));
      status.set(error);
    }
  }

  static <T> Optional<List<T>> routesForAlliance(
      Optional<Alliance> alliance, List<T> blue, List<T> red) {
    return alliance.map(value -> value == Alliance.BLUE ? blue : red);
  }

  static void validateSequence(List<Trajectory<SwerveSample>> paths) {
    double duration = 0;
    for (int i = 0; i < paths.size(); i++) {
      duration += paths.get(i).getTotalTime() + Constants.Autonomous.SETTLE_SECONDS;
      if (i > 0 && !ChoreoController.atGoal(
          paths.get(i - 1).getFinalPose(false).orElseThrow(),
          paths.get(i).getInitialSample(false).orElseThrow())) {
        throw new IllegalArgumentException("Segment poses are discontinuous");
      }
    }
    if (duration > Constants.Autonomous.MAX_DURATION_SECONDS) {
      throw new IllegalArgumentException("Routine plus settling exceeds configured time budget");
    }
  }

  private Command sequence(String name, List<Trajectory<SwerveSample>> paths, boolean simulationOnly) {
    return Command.noRequirements(coroutine -> {
          for (int i = 0; i < paths.size(); i++) {
            var succeeded = new AtomicBoolean(false);
            coroutine.await(drivetrain.followChoreo(
                paths.get(i), i == 0, simulationOnly, status::set, () -> succeeded.set(true)));
            if (!succeeded.get()) {
              // Keep drivetrain ownership stopped after failure; never continue to the next leg.
              coroutine.await(holdStopped.get());
              return;
            }
          }
          status.set("Completed routine: " + name);
          coroutine.await(holdStopped.get());
        }).named("Autonomous.Choreo[" + name + "]");
  }

  /** Snapshot the dashboard choice once at autonomous enable. */
  public Command selectedCommand() {
    var selected = chooser.getSelected();
    return selected == null ? fallback("Holding stopped: invalid selection") : selected.get();
  }

  private Command fallback(String message) {
    status.set(message);
    return holdStopped.get();
  }

  @Override
  public void close() {
    Tunables.remove(CHOOSER_KEY);
    status.close();
  }
}
