package frc.robot.auto;

import choreo.Choreo;
import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import frc.robot.Constants;
import java.io.IOException;
import java.nio.file.Files;
import org.wpilib.system.Filesystem;

/** Low-level Choreo APIs only: Alpha 3's auto helpers still use Commands v2. */
public final class ChoreoPaths {
  // Allow numerical export/solver rounding; motor velocity commands retain exact hard caps.
  private static final double EXPORT_VELOCITY_EPSILON = 1e-4;

  private ChoreoPaths() {}

  /** Load during robot construction/registration, never from a running control loop. */
  public static Trajectory<SwerveSample> load(String name) {
    if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9_-]*")) {
      throw new IllegalArgumentException("Use a simple trajectory filename stem: " + name);
    }
    var file = Filesystem.getDeployDirectory().toPath().resolve("choreo").resolve(name + ".traj");
    try (var reader = Files.newBufferedReader(file)) {
      validateExport(JsonParser.parseReader(reader).getAsJsonObject());
    } catch (IOException | RuntimeException ex) {
      throw new IllegalArgumentException("Invalid Choreo export " + name + ": " + ex.getMessage(), ex);
    }
    var trajectory = Choreo.<SwerveSample>loadTrajectory(name)
        .orElseThrow(() -> new IllegalArgumentException("Missing/unreadable Choreo trajectory: " + name));
    validate(trajectory);
    return trajectory;
  }

  /**
   * Check the raw export before Choreo's deserializer can discard unsupported event markers
   * or substitute zero/default values for missing sample fields.
   */
  static void validateExport(JsonObject document) {
    var events = document.get("events");
    if (events == null || !events.isJsonArray() || !events.getAsJsonArray().isEmpty()) {
      throw new IllegalArgumentException("Embedded events are unsupported; use v3 compositions");
    }
    var trajectory = document.getAsJsonObject("trajectory");
    if (!"Swerve".equals(trajectory.get("sampleType").getAsString())) {
      throw new IllegalArgumentException("Only Swerve exports are supported");
    }
    for (var value : trajectory.getAsJsonArray("samples")) {
      var sample = value.getAsJsonObject();
      for (var key : new String[] {"t", "x", "y", "heading", "vx", "vy", "omega", "ax", "ay", "alpha"}) {
        requireNumber(sample.get(key), key);
      }
      for (var key : new String[] {"fx", "fy"}) {
        var forces = sample.get(key);
        if (forces == null || !forces.isJsonArray() || forces.getAsJsonArray().size() != 4) {
          throw new IllegalArgumentException(key + " must have four FL/FR/BL/BR forces");
        }
        for (var force : forces.getAsJsonArray()) requireNumber(force, key);
      }
    }
  }

  private static void requireNumber(com.google.gson.JsonElement value, String field) {
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
        || !Double.isFinite(value.getAsDouble())) {
      throw new IllegalArgumentException("Missing/non-finite numeric sample field: " + field);
    }
  }

  /** Check every knot and the acceleration-integrated intervals used by Choreo Alpha 3. */
  public static void validate(Trajectory<?> trajectory) {
    if (trajectory == null || trajectory.samples().size() < 2 || !trajectory.events().isEmpty()) {
      throw new IllegalArgumentException("Need at least two samples and no embedded events; "
          + "use explicit Commands v3 compositions for mechanism actions");
    }
    SwerveSample previous = null;
    for (Object value : trajectory.samples()) {
      if (!(value instanceof SwerveSample sample)) {
        throw new IllegalArgumentException("Only Swerve trajectories are supported");
      }
      validateSample(sample);
      if (sample.t < 0 || (previous != null && sample.t <= previous.t)) {
        throw new IllegalArgumentException("Non-monotonic trajectory time");
      }
      if (previous != null) validateInterval(previous, sample);
      previous = sample;
    }
    var first = (SwerveSample) trajectory.samples().getFirst();
    var last = (SwerveSample) trajectory.samples().getLast();
    if (Math.abs(first.t) > 1e-6 || last.t <= 0
        || last.t > Constants.Autonomous.MAX_DURATION_SECONDS
        || Math.hypot(first.vx, first.vy) > 0.02 || Math.abs(first.omega) > 0.02
        || Math.hypot(last.vx, last.vy) > 0.02 || Math.abs(last.omega) > 0.02) {
      throw new IllegalArgumentException("Trajectory must start at t=0, start/end stopped, "
          + "and fit the configured time budget");
    }
  }

  private static void validateInterval(SwerveSample start, SwerveSample end) {
    // Velocity is linear within each interval, so its norm/absolute maximum is at an end.
    // Check the left-limit end as well as the stored end; these need not be identical.
    validateSample(start.interpolate(end, end.t));
    validateSample(start.interpolate(end, (start.t + end.t) / 2));
    double duration = end.t - start.t;
    // Check position/heading polynomial extrema too, including potential numeric overflow.
    double[] extrema = {-start.vx / start.ax, -start.vy / start.ay, -start.omega / start.alpha};
    for (double offset : extrema) {
      if (Double.isFinite(offset) && offset > 0 && offset < duration) {
        validateSample(start.interpolate(end, start.t + offset));
      }
    }
  }

  /** Also called on the actual runtime sample before any motor request is issued. */
  public static void validateSample(SwerveSample sample) {
    if (sample == null) throw new IllegalArgumentException("Missing Swerve sample");
    double[] scalars = {sample.t, sample.x, sample.y, sample.heading, sample.vx, sample.vy,
        sample.omega, sample.ax, sample.ay, sample.alpha};
    for (double scalar : scalars) {
      if (!Double.isFinite(scalar)) throw new IllegalArgumentException("Non-finite Choreo sample");
    }
    for (double force : sample.moduleForcesX()) {
      if (!Double.isFinite(force)) throw new IllegalArgumentException("Non-finite X force");
    }
    for (double force : sample.moduleForcesY()) {
      if (!Double.isFinite(force)) throw new IllegalArgumentException("Non-finite Y force");
    }
    if (Math.hypot(sample.vx, sample.vy) > Constants.Autonomous.MAX_SPEED_MPS + EXPORT_VELOCITY_EPSILON
        || Math.abs(sample.omega) > Constants.Autonomous.MAX_OMEGA_RAD_PER_SEC + EXPORT_VELOCITY_EPSILON) {
      throw new IllegalArgumentException("Interpolated trajectory exceeds autonomous velocity limits");
    }
  }
}
