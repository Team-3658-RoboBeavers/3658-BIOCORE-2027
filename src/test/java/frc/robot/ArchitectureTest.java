package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  @Test
  void cameraNamesAreUnique() {
    Set<String> names =
        Set.of(
            Constants.Vision.FRONT_NAME,
            Constants.Vision.REAR_NAME,
            Constants.Vision.LEFT_NAME,
            Constants.Vision.RIGHT_NAME);
    assertEquals(4, names.size());
  }

  @Test
  void unfinishedCalibrationCannotBeMistakenForVerified() {
    // Each gate may change only in a reviewed commissioning PR that replaces its assertion with a
    // check of the recorded evidence (see docs/Robot-Commissioning.md and Choreo-Autonomous.md).
    assertFalse(
        Constants.Swerve.HARDWARE_CONFIGURATION_VERIFIED,
        "Drivetrain hardware is not commissioned");
    assertFalse(
        Constants.Autonomous.PATH_FOLLOWING_VERIFIED,
        "Physical autonomous path following is not commissioned");
    assertFalse(
        Constants.Autonomous.FORCE_FEEDFORWARD_VERIFIED,
        "Choreo wheel-force feedforward model is not approved");
    assertFalse(Constants.Vision.POSE_FUSION_VERIFIED, "Vision pose fusion is not implemented");
  }

  @Test
  void proposedSwerveIdsAreUniqueOnCanS0() {
    Set<Integer> ids =
        Set.of(
            Constants.Swerve.PIGEON_ID,
            Constants.Swerve.FRONT_LEFT.driveId(),
            Constants.Swerve.FRONT_LEFT.steerId(),
            Constants.Swerve.FRONT_LEFT.encoderId(),
            Constants.Swerve.FRONT_RIGHT.driveId(),
            Constants.Swerve.FRONT_RIGHT.steerId(),
            Constants.Swerve.FRONT_RIGHT.encoderId(),
            Constants.Swerve.BACK_LEFT.driveId(),
            Constants.Swerve.BACK_LEFT.steerId(),
            Constants.Swerve.BACK_LEFT.encoderId(),
            Constants.Swerve.BACK_RIGHT.driveId(),
            Constants.Swerve.BACK_RIGHT.steerId(),
            Constants.Swerve.BACK_RIGHT.encoderId());
    assertEquals(13, ids.size());
  }
}
