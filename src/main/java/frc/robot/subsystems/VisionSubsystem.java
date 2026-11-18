package frc.robot.subsystems;

import frc.robot.Constants;
import java.util.List;
import org.photonvision.PhotonCamera;
import org.wpilib.command3.Mechanism;
import org.wpilib.system.Timer;

/**
 * Owns the four PhotonVision clients. Pose fusion is intentionally disabled until camera transforms
 * and the official 2027 field layout are verified.
 */
public class VisionSubsystem implements Mechanism, AutoCloseable {
  private static final double STALE_SECONDS = 1.0;

  private final List<CameraHealth> cameras =
      List.of(
          new CameraHealth(Constants.Vision.FRONT_NAME, "Orange Pi A"),
          new CameraHealth(Constants.Vision.REAR_NAME, "Orange Pi A"),
          new CameraHealth(Constants.Vision.LEFT_NAME, "Orange Pi B"),
          new CameraHealth(Constants.Vision.RIGHT_NAME, "Orange Pi B"));

  public void updateInputs() {
    for (CameraHealth camera : cameras) {
      if (!camera.camera.getAllUnreadResults().isEmpty()) {
        camera.lastFrameSeconds = Timer.getTimestamp();
      }
    }
  }

  @Override
  public void close() {
    cameras.forEach(camera -> camera.camera.close());
  }

  public List<CameraStatus> getCameraStatuses() {
    double now = Timer.getTimestamp();
    return cameras.stream()
        .map(
            camera ->
                new CameraStatus(
                    camera.camera.getName(),
                    camera.processor,
                    now - camera.lastFrameSeconds <= STALE_SECONDS))
        .toList();
  }

  public record CameraStatus(String name, String processor, boolean receivingFrames) {}

  private static final class CameraHealth {
    private final PhotonCamera camera;
    private final String processor;
    private double lastFrameSeconds = Double.NEGATIVE_INFINITY;

    private CameraHealth(String cameraName, String processor) {
      camera = new PhotonCamera(cameraName);
      this.processor = processor;
    }
  }
}
