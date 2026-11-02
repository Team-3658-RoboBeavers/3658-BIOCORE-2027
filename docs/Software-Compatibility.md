# Software Compatibility Snapshot

Checked November 1, 2026. Alpha software changes quickly; recheck these sources before every
toolchain upgrade.

## Pinned set

| Component | Repository version | Reason |
| --- | --- | --- |
| WPILib / GradleRIO | `2027.0.0-alpha-7` | Matches the installed WPILib toolchain used for local verification |
| Systemcore OS | image 14 or later | WPILib Alpha 7 compatibility requirement |
| Phoenix 6 | `26.70.0-alpha-2` | CTRE's documented match for WPILib Alpha 7 |
| Phoenix firmware | `26.70.x` | CTRE's documented match for the pinned API |
| PhotonLib | `dev-v2027.0.0-alpha-2-89-g4b8d9b50` | Current development manifest declaring `2027_alpha7` compatibility |
| Java | 25 | Required by the WPILib Alpha 7 project template |

## Findings and constraints

- WPILib `2027.0.0-alpha-8` was released on October 30, 2026. It is a newer quality-of-life update,
  but the installed local toolchain is Alpha 7. Do not change only the Gradle version: update the
  installer, Systemcore image, Driver Station, vendor dependencies, and CI together.
- PhotonVision's published Alpha 2 release targets an older WPILib alpha. For Alpha 7, the WPILib
  Systemcore testing matrix specifies the latest development release. This repository therefore
  pins the exact development manifest instead of following a floating URL.
- PathPlanner is intentionally absent: the Systemcore compatibility matrix does not list an Alpha 7
  compatible release at this snapshot. Autonomous is a safe no-op until a compatible path solution
  is deliberately selected.
- FIRST and CTRE describe all of these Systemcore integrations as alpha/pre-release. A clean build is
  not hardware validation.

## Primary references

- [WPILib releases](https://github.com/wpilibsuite/allwpilib/releases)
- [Systemcore testing and compatibility matrix](https://github.com/wpilibsuite/SystemcoreTesting)
- [CTRE Phoenix compatibility for Systemcore](https://github.com/wpilibsuite/SystemcoreTesting/blob/main/CTR-Phoenix.md)
- [CTRE Phoenix 6 swerve overview](https://v6.docs.ctr-electronics.com/en/latest/docs/api-reference/mechanisms/swerve/swerve-overview.html)
- [PhotonVision releases](https://github.com/PhotonVision/photonvision/releases)
- [PhotonLib installation](https://docs.photonvision.org/en/latest/docs/programming/photonlib/adding-vendordep.html)
- [SDS MK5i product information](https://www.swervedrivespecialties.com/products/mk5i-swerve-module)

## Upgrade procedure

1. Read WPILib, Systemcore, Phoenix, and PhotonVision release notes.
2. Make a branch dedicated to the toolchain upgrade.
3. Install the new WPILib distribution and import/upgrade the project using WPILib tools.
4. Replace vendor manifests with versions explicitly compatible with that WPILib release.
5. Update the Systemcore image, Driver Station, Phoenix firmware, Tuner X, and PhotonVision images.
6. Run `build`, unit tests, desktop simulation, bench CAN discovery, and disabled hardware checks.
7. Merge only after a mentor records the verified version set in this document.
