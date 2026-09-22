# Build Scripts

This directory contains helper scripts for the KUKSA Android SDK development.

## Databroker Management

### Start Databroker

You can start the databroker using either the Gradle task or the shell script directly:

**Using Gradle (recommended):**
```bash
./gradlew startDatabroker
```

**Using the shell script directly:**
```bash
./buildscripts/start-databroker.sh
```

**Features:**
- Checks if the databroker container is already running (idempotent)
- Starts a stopped container if one exists
- Creates and starts a new container if needed
- Mounts the VSS spec files from the `vss/` directory

**Configuration:**

Environment variables can be used to customize the databroker:

```bash
# Use a specific databroker version (default: main)
export DATABROKER_TAG="0.5.0"

# Use a custom port (default: 55557)
export DATABROKER_PORT="55558"

# Use a custom VSS file (default: vss/vss_release_6.0.json)
export DATABROKER_VSS="vss/custom_vss.json"

./buildscripts/start-databroker.sh
```

**Container details:**
- Name: `kuksa-android-sdk-databroker-dev`
- Default port: `55557` (mapped to container port 55555)
- Image: `ghcr.io/eclipse-kuksa/kuksa-databroker`
- Mode: Insecure (for development)

### Stop Databroker

You can stop the databroker using either the Gradle task or the shell script directly:

**Using Gradle (recommended):**
```bash
./gradlew stopDatabroker
```

**Using the shell script directly:**
```bash
./buildscripts/stop-databroker.sh
```

## Run Configurations

Two Android Studio/IntelliJ run configurations are available that automatically start the databroker before deploying the app:

1. **app (with databroker)** - Runs the test app with automatic databroker startup
2. **samples (with databroker)** - Runs the samples app with automatic databroker startup

These configurations are located in `.run/` and will appear in your IDE's run configuration dropdown.

### How it works

When you run either configuration:
1. The `start-databroker.sh` script is executed as a "Before launch" task
2. The script checks if the databroker is already running
3. If not running, it starts the databroker container
4. The Android app is built and deployed
5. The app can connect to the databroker at `localhost:55557`

### Connecting from the App

When using these run configurations on an emulator or device, you may need to configure port forwarding:

**For Android Emulator:**
```bash
# The emulator can access host machine via 10.0.2.2
# Connect to: 10.0.2.2:55557
```

**For Physical Device:**
```bash
# Set up reverse port forwarding
adb reverse tcp:55557 tcp:55557
# Connect to: localhost:55557
```

See the [Troubleshooting Guide](../docs/TROUBLESHOOTING.md) for more details.

## Manual Databroker Operations

```bash
# View logs
docker logs kuksa-android-sdk-databroker-dev

# Follow logs
docker logs -f kuksa-android-sdk-databroker-dev

# Check container status
docker ps | grep kuksa-android-sdk-databroker-dev

# Stop container (without removing)
docker stop kuksa-android-sdk-databroker-dev

# Start existing container
docker start kuksa-android-sdk-databroker-dev

# Remove container
docker rm kuksa-android-sdk-databroker-dev
```
