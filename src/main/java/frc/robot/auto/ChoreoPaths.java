package frc.robot.auto;

import choreo.Choreo;
import choreo.trajectory.SwerveSample;
import choreo.trajectory.Trajectory;
import frc.robot.Constants;

/** Low-level Choreo APIs only: Alpha 3's auto helpers still use Commands v2. */
public final class ChoreoPaths {
  private ChoreoPaths() {}

  /** Load during robot construction/registration, never from a running control loop. */
  public static Trajectory<SwerveSample> load(String name) {
    if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9_-]*")) {
      throw new IllegalArgumentException("Use a simple trajectory filename stem: " + name);
    }
    var trajectory = Choreo.<SwerveSample>loadTrajectory(name)
        .orElseThrow(() -> new IllegalArgumentException("Missing/unreadable Choreo trajectory: " + name));
    validate(trajectory);
    return trajectory;
  }

  public static void validate(Trajectory<?> trajectory) {
    if (trajectory.samples().size() < 2 || !trajectory.events().isEmpty()) {
      throw new IllegalArgumentException("Need at least two samples and no embedded events; "
          + "use explicit Commands v3 compositions for mechanism actions");
    }
    double previous = -1;
    for (Object value : trajectory.samples()) {
      if (!(value instanceof SwerveSample s)) {
        throw new IllegalArgumentException("Only Swerve trajectories are supported");
      }
      double[] scalars = {s.t, s.x, s.y, s.heading, s.vx, s.vy, s.omega, s.ax, s.ay, s.alpha};
      for (double scalar : scalars) {
        if (!Double.isFinite(scalar)) {
          throw new IllegalArgumentException("Non-finite Choreo sample");
        }
      }
      for (double force : s.moduleForcesX()) {
        if (!Double.isFinite(force)) throw new IllegalArgumentException("Non-finite X force");
      }
      for (double force : s.moduleForcesY()) {
        if (!Double.isFinite(force)) throw new IllegalArgumentException("Non-finite Y force");
      }
      if (s.t <= previous || s.t < 0
          || Math.hypot(s.vx, s.vy) > Constants.Autonomous.MAX_SPEED_MPS + 1e-6
          || Math.abs(s.omega) > Constants.Autonomous.MAX_OMEGA_RAD_PER_SEC + 1e-6) {
        throw new IllegalArgumentException("Non-monotonic time or trajectory exceeds autonomous limits");
      }
      previous = s.t;
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
}
