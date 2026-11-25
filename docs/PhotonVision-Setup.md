# Four-Camera PhotonVision Setup

Step-by-step guide for imaging both **Orange Pi 5** boards (the plain Orange Pi 5, not the 5B,
Pro, Plus, or Max) and bringing up four **Arducam OV9281** global-shutter USB cameras.

Everything in this guide is off-robot bench work. **No motors are involved.** It feeds Step 10 of
the [Bench Test and Commissioning Guide](Robot-Commissioning.md).

Sources: [PhotonVision docs](https://docs.photonvision.org/en/latest/) (read 2026-11-25). The docs
labeled "latest" do not mention 2027 or Systemcore yet. Re-check anything marked **VERIFY** at
kickoff.

## Assignment

| Processor | Camera position | PhotonVision camera name (must match `Constants.Vision`) |
| --- | --- | --- |
| Orange Pi A | Front | `3658-front` |
| Orange Pi A | Rear | `3658-rear` |
| Orange Pi B | Left | `3658-left` |
| Orange Pi B | Right | `3658-right` |

The Orange Pis communicate over Ethernet. The cameras do not connect to a CAN bus.

## What you need

| Item | Notes |
| --- | --- |
| 2× Orange Pi 5 | Label them **A** and **B** with tape before you start. |
| 2× microSD card, 8 GB or larger | PhotonVision recommends industrial-grade cards for robots (fewer corrupted settings/logs). |
| 4× Arducam OV9281 USB camera + lens | Label each one **front / rear / left / right**. Cases are recommended (see Step 6). |
| Bench power for each Pi | A wall power brick is fine off-robot. |
| Robot power for each Pi (later) | A regulated 5 V supply, not raw battery. PhotonVision recommends the Pololu S13V30F5 or Redux Zinc-V regulator, with 18 AWG (or 20 AWG) wire. Size the regulator to the Pi's real draw. |
| Ethernet cables and a network switch or router | For the bench test, use a router with DHCP (school or home network). |
| Laptop with the WPILib 2027 tools | Used to flash the cards, open the web UI, and run robot simulation. |
| ChArUco calibration board | Print PhotonVision's board at 100 % scale on flat, rigid stock. Measure it with calipers. |
| Printed AprilTag of known size | For detection and FPS checks. |
| Painter's tape and a marker | For labeling USB ports and cables. |

## Step 1 — Download the image and the matching PhotonLib (once, for both boards)

PhotonVision's [Dev release](https://github.com/PhotonVision/photonvision/releases/tag/Dev) is a
**rolling** build: its files are replaced whenever a new development build is published. The image
on both Pis and the PhotonLib version in this repository **must be the exact same build**.

1. From the Dev release, download **both** of these at the same time:
   - `photonvision-dev-<version>-linuxarm64_orangepi5.img.xz`. Use the plain `orangepi5` image,
     not `orangepi5b`, `orangepi5pro`, `orangepi5plus`, or `orangepi5max`.
   - `photonlib-<version>.json`
2. Check that `<version>` is identical in both file names. Write it in the record table at the end.
3. Store both files in the team's shared drive so the same build can be re-flashed later.
4. Open a pull request that replaces `vendordeps/photonlib.json` with the downloaded
   `photonlib-<version>.json` (keep the file name `photonlib.json`). CI must pass. Ask a
   programmer, or Claude, to do this step.

> As of 2026-11-25 the repo pins `dev-v2027.0.0-alpha-2-89-g4b8d9b50`, but the Dev release only
> offers `-90-g36248706`. Plan on updating the vendordep in step 4.

## Step 2 — Flash both microSD cards

1. Install **Raspberry Pi Imager version 2.0.0 or earlier**. PhotonVision warns that Imager 2.0.2
   and later fail to write the image. Do **not** use Balena Etcher; PhotonVision no longer
   recommends it.
2. In Imager: **Choose OS → Use custom** → select the `.img.xz` file. **Choose Storage** → select the
   microSD card. Skip any "customize settings" prompts. Click **Write**.
3. Label the card **A**. Repeat with the second card and label it **B**.

**Check:** both cards finish writing and verifying without errors.

## Step 3 — First boot, one board at a time

Do Pi A completely, then Pi B. If both boards boot at once, they will have the same default
hostname.

1. Insert card A into Pi A. Connect Ethernet to the bench router. **No cameras yet.**
2. Power on. Wait about a minute.
3. On the laptop (same network), browse to `http://photonvision.local:5800`.
4. In **Settings**:
   - **Hostname:** `photon-3658-a`. Hostnames may only use letters, numbers, and `-`.
   - **Team number:** leave blank for now (Step 7 sets it).
   - **IP:** leave on **DHCP**. PhotonVision says to use a static IP only on the robot radio, never
     at home or on the bench.
5. Save, reboot, and confirm the UI is now at `http://photon-3658-a.local:5800`.
6. Record the software version shown in the UI. It must match the version from Step 1.
7. Repeat for Pi B with hostname `photon-3658-b`.

**Check:** both UIs load at their own hostnames and show the same version as Step 1.

## Step 4 — Label USB ports before any camera is plugged in

PhotonVision tells two identical cameras apart **only by the physical USB port** they are plugged
into. If two OV9281s are swapped between ports, each one silently loads the *other* camera's
settings and calibration. That gives wrong poses with no error message.

1. On each Pi, pick the two USB ports you will use and keep them permanent. Use the same choice on
   A and B. Tape-label the ports `FRONT`/`REAR` on Pi A and `LEFT`/`RIGHT` on Pi B.
2. Label both ends of each camera cable with the same name.
3. Optional but recommended: give each camera a unique USB name with the
   [Arducam Serial Number Tool](https://docs.arducam.com/UVC-Camera/Serial-Number-Tool-Guide/),
   as PhotonVision's [Arducam page](https://docs.photonvision.org/en/latest/docs/camera-specific-configuration/arducam-cameras.html)
   suggests. Also look for PhotonVision's **strict matching** setting.
   **VERIFY** what both do in the installed version before relying on them.

## Step 5 — Add cameras one at a time

For Pi A, then Pi B:

1. Plug in **only** the first camera (for example, front → `FRONT` port on Pi A).
2. In the **Cameras** tab, select it. Use the **Arducam model selector** to choose the OV9281 model.
3. Set the camera **nickname** to exactly `3658-front`. Copy it from the table above: the robot
   code finds cameras by this exact text.
4. Cover the lens with your hand and confirm the stream goes dark. This proves you're looking at
   the right camera.
5. Plug in the second camera (`3658-rear` → `REAR` port) and repeat.
6. Reboot the Pi. Confirm both cameras come back with the **same names on the same ports** and
   no "Camera Mismatch" status.

**Check:** four uniquely named cameras, each on its labeled port, surviving a reboot.

## Step 6 — Pipeline, focus, and exposure

1. Create an **AprilTag** pipeline for each camera. PhotonVision recommends the AprilTag detector
   for most uses: it is more accurate, and about 2× slower than ArUco.
2. Choose one processing resolution and use it for all four cameras. PhotonVision lists the
   Orange Pi 5 (4 GB) as handling two AprilTag streams at **1280×800, 30 FPS**. Start there.
3. Focus each lens on a tag at your typical scoring distance, then lock the lens so it can't turn.
   **Calibration is invalidated if the focus moves.**
4. Exposure: set it as low as it goes while tags still track reliably. Raise gain/brightness to
   compensate. Low exposure reduces motion blur.
5. Arducam bare-board cameras can randomly disconnect because of static discharge (ESD).
   Mount each camera in a case.

## Step 7 — Calibrate every camera separately

Calibration belongs to **one physical camera at one resolution**. Never copy it between cameras,
even identical ones. If you change resolution later, recalibrate.

1. Use a **ChArUco** board (PhotonVision strongly recommends it over a chessboard). Enter the
   board's *measured* square and marker size.
2. Take **at least 12** snapshots that cover the whole image: corners, edges, near, and far. Keep
   tilt under 45°. Hold the board still for each snapshot.
3. Accept the calibration only if the **mean error is under 1 pixel** and the reported FOV is
   within ±10° of the camera's spec.
4. Export and save each calibration file to the shared drive. Name each file after its camera.

## Step 8 — Laptop link test (no Systemcore needed)

This proves PhotonVision → NetworkTables → robot code works before the real robot can run. The
real-robot code currently stops at startup until the drivetrain is commissioned.

1. Keep the laptop and both Pis on the bench router. Find the laptop's IP address.
2. On each Pi, in **Settings → Team number**, enter the **laptop's IP address**. PhotonVision
   accepts an IP or hostname here. Save.
3. On the laptop, from the repo folder, start simulation:

   ```powershell
   $env:JAVA_HOME = 'C:\Users\Public\wpilib\2027_alpha7\jdk'
   .\gradlew.bat simulateJava
   ```

4. Open AdvantageScope, OutlineViewer, or another NetworkTables viewer pointed at `localhost`.
   Under `/photonvision/`, you should see all four camera names updating, plus
   `/photonvision/.timesync/` entries for each Pi.
5. Hold an AprilTag in front of each camera in turn. Confirm targets appear for **that** camera's
   name only.
6. Unplug one camera, then one whole Pi. The other cameras must keep updating, and the simulation
   must keep running. Plug it back in and record how long it takes to recover.
7. When you're done, clear the team-number field, or set it to `3658` once the robot network
   scheme is known.

**Check:** all four names publish, tags show up under the right name, and losing a camera or Pi
doesn't stop the others.

## Step 9 — Moving to the robot (after kickoff)

**VERIFY** the 2027 Systemcore network addressing first. PhotonVision's current docs use the
roboRIO-era scheme: static coprocessor IPs at `10.TE.AM.xx`, with `xx` unique in the `.6`–`.19`
range. For team 3658 that would be `10.36.58.11` (A) and `10.36.58.12` (B). Don't apply this until
the Systemcore network documentation confirms it.

Then: set the team number to `3658`, set static IPs (only on the robot network), power each Pi from
its regulator, and repeat the Step 8 checks against the real robot.

## Robot measurements still required

For each camera, measure the transform from the robot coordinate origin to the optical frame:

- X: forward positive
- Y: left positive
- Z: up positive
- roll, pitch, yaw using WPILib conventions

Enter those values in `Constants.Vision`. All four transforms are currently zero placeholders.

## Pose-fusion gate

`Constants.Vision.POSE_FUSION_VERIFIED` intentionally remains `false`. Enable fusion only after:

- the official 2027 AprilTag field layout is available and loaded;
- all four camera calibrations and robot-to-camera transforms are measured;
- timestamps and network latency are validated;
- implausible poses, high ambiguity, excessive tag distance, and out-of-field estimates are
  rejected;
- single-camera and multi-camera tests agree with known field measurements;
- disconnecting any camera does not disturb odometry or robot control.

The initial `VisionSubsystem` only verifies that frames arrive under all four configured names. Pose
estimation and drivetrain fusion are deliberately unfinished.

## Record sheet

Copy this table into the dated team test log.

| Field | Pi A | Pi B |
| --- | --- | --- |
| Board serial / label | | |
| microSD brand / size | | |
| PhotonVision version (UI) | | |
| Image file name | | |
| `photonlib.json` version in repo | | |
| Hostname | `photon-3658-a` | `photon-3658-b` |
| USB port → camera | FRONT → `3658-front`, REAR → `3658-rear` | LEFT → `3658-left`, RIGHT → `3658-right` |
| Camera serials / lens | | |
| Resolution / exposure / gain | | |
| Calibration error (px) / FOV per camera | | |
| FPS / latency (ms) per camera | | |
| Recovery time: camera unplug / Pi reboot (s) | | |
| Tested by / reviewed by / date | | |
