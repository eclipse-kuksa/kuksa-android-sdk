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

package org.eclipse.kuksa.connectivity.databroker.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.InternetProtocol
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import org.eclipse.kuksa.connectivity.databroker.DATABROKER_TIMEOUT_SECONDS
import java.io.File
import java.io.FileNotFoundException
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.TimeUnit

private const val KEY_PROPERTY_DATABROKER_TAG = "databroker.tag"
private const val KEY_ENV_DATABROKER_TAG = "DATABROKER_TAG"
private const val DEFAULT_DATABROKER_TAG = "main"

const val KEY_PROPERTY_DATABROKER_TIMEOUT = "databroker.timeout"
const val KEY_ENV_DATABROKER_TIMEOUT = "DATABROKER_TIMEOUT"
private const val DEFAULT_DATABROKER_TIMEOUT = "15"

private const val KEY_PROPERTY_DATABROKER_VSS = "databroker.vss"
private const val KEY_ENV_DATABROKER_VSS = "DATABROKER_VSS"
private const val DEFAULT_DATABROKER_VSS = "vss/vss_release_6.0.json"

/**
 * Starts and stops the Databroker Docker Container. Per default the image with the master tag is pulled and started.
 * The version of the image can be influenced by either a System Property with value "databroker.tag" or an
 * Environment Variable with value "DATABROKER_TAG".
 *```
 * Samples
 *
 * System Property:
 * ./gradlew clean build -Ddatabroker.tag="0.4.1"
 *
 *
 * Environment Variable:
 * export DATABROKER_TAG="0.4.1"
 * ./gradlew clean build
 *
 * or
 *
 * DATABROKER_TAG="0.4.1" ./gradlew clean build
 * ```
 *
 * The VSS spec file used to populate the databroker can be overridden via system property "databroker.vss" or
 * environment variable "DATABROKER_VSS". The value must be a path to a VSS YAML or JSON file.
 *```
 * System Property:
 * ./gradlew clean build -Ddatabroker.vss="/path/to/vss.yaml"
 *
 * Environment Variable:
 * DATABROKER_VSS="/path/to/vss.yaml" ./gradlew clean build
 * ```
 */
abstract class DataBrokerDockerContainer(
    protected val containerName: String,
) {
    val port: Int = findAvailablePort()

    private val vssFilePath = System.getProperty(KEY_PROPERTY_DATABROKER_VSS)
        ?: System.getenv(KEY_ENV_DATABROKER_VSS)
        ?: DEFAULT_DATABROKER_VSS

    private val resolvedVssFile: File = resolveVssFile(vssFilePath)
    private val vssFileName: String = resolvedVssFile.name
    protected val vssDirectory: String = checkNotNull(resolvedVssFile.parent)
    protected val vssMountDirectory: String = "/vss"
    protected val vssMount: String = "$vssMountDirectory/$vssFileName"

    protected open val hostConfig: HostConfig = HostConfig.newHostConfig()
        .withAutoRemove(true)
        .withPortBindings(
            PortBinding(
                Ports.Binding("0.0.0.0", port.toString()),
                ExposedPort(port, InternetProtocol.TCP),
            ),
        )
        .withBinds(
            Bind(vssDirectory, Volume(vssMountDirectory), AccessMode.ro),
        )

    protected val repository = "ghcr.io/eclipse-kuksa/kuksa-databroker"

    private val databrokerTag = System.getProperty(KEY_PROPERTY_DATABROKER_TAG)
        ?: System.getenv(KEY_ENV_DATABROKER_TAG)
        ?: DEFAULT_DATABROKER_TAG

    private val timeout = System.getProperty(KEY_PROPERTY_DATABROKER_TIMEOUT)
        ?: System.getenv(KEY_ENV_DATABROKER_TIMEOUT)
        ?: DEFAULT_DATABROKER_TIMEOUT

    protected val dockerClient: DockerClient

    private var containerId: String? = null

    init {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()

        val timeout = Duration.ofSeconds(DATABROKER_TIMEOUT_SECONDS)
        val dockerHttpClient = ApacheDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .connectionTimeout(timeout)
            .responseTimeout(timeout)
            .build()

        dockerClient = DockerClientImpl.getInstance(config, dockerHttpClient)
    }

    fun start() {
        pullImage(databrokerTag)
        val databrokerContainer = createContainer(databrokerTag)
        containerId = databrokerContainer.id
        startContainer(databrokerContainer.id)
    }

    fun stop() {
        containerId?.let { id ->
            try {
                dockerClient.stopContainerCmd(id).exec()
            } catch (_: Exception) {
                // thrown when a container is already stopped
            }
        }
    }

    private fun pullImage(tag: String) {
        dockerClient.pullImageCmd(repository)
            .withTag(tag)
            .exec(PullImageResultCallback())
            .awaitCompletion(timeout.toLong(), TimeUnit.SECONDS)
    }

    protected abstract fun createContainer(tag: String): CreateContainerResponse

    private fun startContainer(containerId: String) {
        try {
            dockerClient.startContainerCmd(containerId).exec()
        } catch (_: NotModifiedException) {
            // thrown when a container is already started
        }
    }

    companion object {
        internal fun findAvailablePort(): Int {
            return ServerSocket(0).apply {
                reuseAddress = true
            }.use { it.localPort }
        }
    }
}

internal fun resolveVssFile(rawPath: String, maxParentTraversals: Int = 5): File {
    val initialFile = File(rawPath)
    if (initialFile.exists()) {
        return initialFile.canonicalFile
    }

    var currentDir = File(".").canonicalFile
    repeat(maxParentTraversals) {
        val candidate = File(currentDir, rawPath)
        if (candidate.exists()) {
            return candidate.canonicalFile
        }
        currentDir = currentDir.parentFile ?: return@repeat
    }

    throw FileNotFoundException(
        "Could not resolve VSS file '$rawPath' from working directory '${File(".").canonicalPath}'",
    )
}
