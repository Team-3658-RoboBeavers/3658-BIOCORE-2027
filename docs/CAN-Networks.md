# CAN Network Plan

Systemcore provides independent CAN ports. Device IDs may repeat across ports, but every device on
one port must have a unique ID.

| Port | Owner | Planned devices |
| --- | --- | --- |
| `CAN_S0` | Drivetrain | 8 Talon FX-class controllers (X60/X44), 4 CANcoders, 1 Pigeon2 |
| `CAN_S1` | Intake | Unselected; do not add devices in code until documented |
| `CAN_S2` | Indexer | Unselected; do not add devices in code until documented |
| `CAN_S3` | Scoring | Reserved |
| `CAN_S4` | Auxiliary | Reserved |

## Proposed drivetrain IDs on CAN_S0

| Device | ID |
| --- | ---: |
| Pigeon2 | 0 |
| Front-left X60 / X44 / CANcoder | 1 / 2 / 3 |
| Front-right X60 / X44 / CANcoder | 4 / 5 / 6 |
| Back-left X60 / X44 / CANcoder | 7 / 8 / 9 |
| Back-right X60 / X44 / CANcoder | 10 / 11 / 12 |

These are proposals, not discovered hardware IDs. Label both ends of every CAN cable and every
device before powering the completed network.

## Commissioning sequence

1. Power one bus at a time with the robot disabled and wheels off the floor.
2. Confirm termination and inspect bus health.
3. Use the Systemcore-compatible Phoenix Tuner X preview to discover, name, and update devices.
4. Confirm that every device reports on the intended `CAN_Sx` port.
5. Assign IDs from the table or update the code and this document to match the approved inventory.
6. Export the final inventory and record device serial numbers and firmware.
7. Verify motor directions at low output one module at a time.

Never move a mechanism to another CAN port without updating `RobotCAN.java`, this document, and the
commissioning record together.
