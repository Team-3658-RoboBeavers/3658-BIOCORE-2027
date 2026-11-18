# Choreo authoring files

Open 3658-BIOCORE-2027-UNVERIFIED.chor in Choreo GUI 2027.0.0-alpha-3.
It is an empty Swerve project with provisional model numbers, not a characterized robot.

Follow [the authoring and commissioning guide](../docs/Choreo-Autonomous.md) before solving paths.
Record measured geometry, loaded radius, mass/inertia, X60 drive model/current limits, traction,
and bumper extents. Verify the field frame and author explicit blue/red paths.

Save editable trajectories here, then copy reviewed, solved .traj exports into
src/main/deploy/choreo and register them in ChoreoAutos. Keep source and deploy versions in sync.
The build does not launch Choreo or solve trajectories.
