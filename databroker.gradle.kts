/*
 * Copyright (c) 2023 - 2026 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

/**
 * Gradle tasks for managing the KUKSA Databroker Docker container during local development.
 *
 * Available tasks:
 * - startDatabroker: Starts the databroker container and sets up adb port forwarding
 * - stopDatabroker: Stops and removes the databroker container
 *
 * Configuration via environment variables:
 * - DATABROKER_TAG: Docker image tag (default: main)
 * - DATABROKER_PORT: Port to expose (default: 55557)
 * - DATABROKER_VSS: VSS spec file path (default: vss/vss_release_6.0.json)
 */

tasks.register<Exec>("startDatabroker") {
    description = "Starts the KUKSA Databroker Docker container for local development"
    group = "databroker"

    commandLine("$rootDir/buildscripts/start-databroker.sh")

    isIgnoreExitValue = false
}

tasks.register<Exec>("stopDatabroker") {
    description = "Stops and removes the KUKSA Databroker Docker container"
    group = "databroker"

    commandLine("$rootDir/buildscripts/stop-databroker.sh")

    isIgnoreExitValue = true
}
