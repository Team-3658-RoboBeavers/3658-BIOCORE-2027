package frc.robot.auto;

import static org.junit.jupiter.api.Assertions.*;

import choreo.trajectory.EventMarker;
import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import frc.robot.Constants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.wpilib.driverstation.Alliance;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;

class ChoreoTest {
  private SwerveSample sample(double t, double x, double vx, double heading) {
    return new SwerveSample(t, x, 0, heading, vx, 0, 0, 0, 0, 0,
        new double[4], new double[4]);
  }

  private Trajectory<SwerveSample> path(SwerveSample... samples) {
    return new Trajectory<>("Test", List.of(samples), List.of(0), List.of());
  }

  @Test
  void loadsCompatibleDeployExportAndInterpolatesWithoutFieldFlipping() throws Exception {
    assertTrue(HAL.initialize());
    try (var files = Files.list(Path.of("src/main/deploy/choreo"))) {
      for (var file : files.filter(p -> p.toString().endsWith(".traj")).toList()) {
        String filename = file.getFileName().toString();
        assertDoesNotThrow(() -> ChoreoPaths.load(filename.substring(0, filename.length() - 5)));
      }
    }
    var trajectory = ChoreoPaths.load("SimulationStraight");
    assertEquals(2, trajectory.getTotalTime());
    assertEquals(0.03125, trajectory.sampleAt(0.5, false).orElseThrow().x, 1e-9);
    assertEquals(0.25, trajectory.getFinalPose(false).orElseThrow().getX(), 1e-9);
    assertThrows(IllegalArgumentException.class, () -> ChoreoPaths.load("../unsafe"));
    assertThrows(IllegalArgumentException.class, () -> ChoreoPaths.load("Missing-3658-Test"));
  }

  @Test
  void rejectsUnsafeTimingMotionAndUnsupportedEvents() {
    assertThrows(IllegalArgumentException.class, () -> ChoreoPaths.validate(
        path(sample(0, 0, 0, 0), sample(0, 0, 0, 0))));
    assertThrows(IllegalArgumentException.class, () -> ChoreoPaths.validate(
        path(sample(0, 0, 0, 0), sample(1, 1, 0.3, 0))));
    assertThrows(IllegalArgumentException.class, () -> ChoreoPaths.validate(
        path(sample(0, 0, 0, 0), sample(1, 0.3, 2, 0), sample(2, 0.5, 0, 0))));
    assertThrows(IllegalArgumentException.class, () -> ChoreoPaths.validate(
        path(sample(0, Double.NaN, 0, 0), sample(1, 0, 0, 0))));
    assertThrows(IllegalArgumentException.class, () -> ChoreoPaths.validate(
        path(sample(0, 0, 0, 0), sample(16, 0, 0, 0))));
    assertThrows(IllegalArgumentException.class, () -> ChoreoPaths.validate(
        new Trajectory<>("Events", List.of(sample(0, 0, 0, 0), sample(1, 0, 0, 0)),
            List.of(0), List.of(new EventMarker(0.5, "Shoot")))));
    assertFalse(Constants.Autonomous.PATH_FOLLOWING_VERIFIED);
    assertFalse(Constants.Autonomous.FORCE_FEEDFORWARD_VERIFIED);
  }

  @Test
  void feedbackUsesShortestHeadingAndCapsCombinedVelocity() {
    var pose = new Pose2d(0, 0, Rotation2d.fromDegrees(179));
    var target = sample(0, 2, 0.2, Math.toRadians(-179));
    var output = ChoreoController.calculate(pose, target);
    assertEquals(Constants.Autonomous.MAX_SPEED_MPS, Math.hypot(output.vx, output.vy), 1e-9);
    assertTrue(output.omega > 0 && output.omega < 0.2, "Use the short +2 degree heading error");
    assertTrue(ChoreoController.atGoal(pose, sample(0, 0, 0, Math.toRadians(-179))));
    assertFalse(ChoreoController.atGoal(pose, sample(0, 0.2, 0, Math.toRadians(-179))));
  }

  @Test
  void validatesSequenceContinuityTimeBudgetAndV3OnlyRuntime() {
    var first = path(sample(0, 0, 0, 0), sample(1, 0.2, 0, 0));
    var second = path(sample(0, 0.2, 0, 0), sample(1, 0.4, 0, 0));
    assertDoesNotThrow(() -> ChoreoAutos.validateSequence(List.of(first, second)));
    assertThrows(IllegalArgumentException.class, () -> ChoreoAutos.validateSequence(List.of(first, first)));
    var tooLong = path(sample(0, 0, 0, 0), sample(15, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> ChoreoAutos.validateSequence(List.of(tooLong)));
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("org.wpilib.command2.Command", false, getClass().getClassLoader()));
  }

  @Test
  void allianceSelectionDoesNotAssumeOldFieldDimensionsOrDefaultUnknownToBlue() {
    var blue = List.of("Blue-A", "Blue-B");
    var red = List.of("Red-A", "Red-B");
    assertEquals(blue, ChoreoAutos.routesForAlliance(Optional.of(Alliance.BLUE), blue, red).orElseThrow());
    assertEquals(red, ChoreoAutos.routesForAlliance(Optional.of(Alliance.RED), blue, red).orElseThrow());
    assertTrue(ChoreoAutos.routesForAlliance(Optional.empty(), blue, red).isEmpty());
  }
}
