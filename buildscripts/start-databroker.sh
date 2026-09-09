#!/bin/bash
# Copyright (c) 2023 - 2026 Contributors to the Eclipse Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0

set -e

CONTAINER_NAME="kuksa-android-sdk-databroker-dev"
DATABROKER_IMAGE="ghcr.io/eclipse-kuksa/kuksa-databroker"
DATABROKER_TAG="${DATABROKER_TAG:-main}"
DATABROKER_PORT="${DATABROKER_PORT:-55557}"
VSS_FILE="${DATABROKER_VSS:-vss/vss_release_6.0.json}"

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Check if container is already running
if docker ps --filter "name=${CONTAINER_NAME}" --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "✓ Databroker container '${CONTAINER_NAME}' is already running on port ${DATABROKER_PORT}"

    # Set up adb reverse port forwarding if device is connected
    if command -v adb &> /dev/null; then
        DEVICE_COUNT=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l | tr -d ' ')
        if [ "$DEVICE_COUNT" -gt 0 ]; then
            echo "→ Setting up adb reverse port forwarding..."
            adb reverse tcp:${DATABROKER_PORT} tcp:${DATABROKER_PORT}
            echo "✓ Port forwarding configured: device can connect to localhost:${DATABROKER_PORT}"
        fi
    fi

    exit 0
fi

# Check if container exists but is stopped
if docker ps -a --filter "name=${CONTAINER_NAME}" --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "→ Starting stopped databroker container '${CONTAINER_NAME}'..."
    docker start "${CONTAINER_NAME}"
    echo "✓ Databroker container started on port ${DATABROKER_PORT}"

    # Set up adb reverse port forwarding if device is connected
    if command -v adb &> /dev/null; then
        DEVICE_COUNT=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l | tr -d ' ')
        if [ "$DEVICE_COUNT" -gt 0 ]; then
            echo "→ Setting up adb reverse port forwarding..."
            adb reverse tcp:${DATABROKER_PORT} tcp:${DATABROKER_PORT}
            echo "✓ Port forwarding configured: device can connect to localhost:${DATABROKER_PORT}"
        fi
    fi

    exit 0
fi

# Container doesn't exist, create and start it
echo "→ Pulling databroker image ${DATABROKER_IMAGE}:${DATABROKER_TAG}..."
docker pull "${DATABROKER_IMAGE}:${DATABROKER_TAG}"

echo "→ Starting new databroker container '${CONTAINER_NAME}'..."

# Resolve VSS file path
if [[ ! -f "${PROJECT_ROOT}/${VSS_FILE}" ]]; then
    echo "Error: VSS file not found at ${PROJECT_ROOT}/${VSS_FILE}"
    exit 1
fi

# Start the container
docker run -d \
    --name "${CONTAINER_NAME}" \
    -p "${DATABROKER_PORT}:55555" \
    -v "${PROJECT_ROOT}/vss:/vss:ro" \
    "${DATABROKER_IMAGE}:${DATABROKER_TAG}" \
    --insecure \
    --metadata "/vss/vss_release_6.0.json"

echo "✓ Databroker container started successfully on port ${DATABROKER_PORT}"

# Set up adb reverse port forwarding if device is connected
if command -v adb &> /dev/null; then
    DEVICE_COUNT=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l | tr -d ' ')
    if [ "$DEVICE_COUNT" -gt 0 ]; then
        echo "→ Setting up adb reverse port forwarding..."
        adb reverse tcp:${DATABROKER_PORT} tcp:${DATABROKER_PORT}
        echo "✓ Port forwarding configured: device can connect to localhost:${DATABROKER_PORT}"
    fi
fi

echo ""
echo "Connection info:"
echo "  Physical device: localhost:${DATABROKER_PORT} (adb reverse configured)"
echo "  Emulator: 10.0.2.2:${DATABROKER_PORT}"
echo "  Host: localhost:${DATABROKER_PORT}"
echo ""
echo "To stop the databroker, run:"
echo "  docker stop ${CONTAINER_NAME}"
echo ""
echo "To remove the container, run:"
echo "  docker rm ${CONTAINER_NAME}"
