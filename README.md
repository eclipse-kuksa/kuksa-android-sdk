# kuksa-sdk-android

This is an Android SDK for the [KUKSA Vehicle Abstraction Layer](https://github.com/eclipse/kuksa.val).


[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)
[![Gitter](https://img.shields.io/gitter/room/kuksa-val/community)](https://gitter.im/kuksa-val/community)

## Overview

The Kuksa Android SDK allows you to interact with [VSS data](https://github.com/COVESA/vehicle_signal_specification) 
from the [KUKSA Databroker](https://github.com/eclipse/kuksa.val/tree/master/kuksa_databroker)
within an Android App. The main functionality consists of fetching, updating and subscribing to VSS data. 

## Integration

*build.gradle*
```
implementation(org.eclipse.kuksa:kuksa-sdk:<VERSION>)
```

The latest release version can be seen [here](https://github.com/eclipse-kuksa/kuksa-android-sdk/releases).

See the [Quickstart guide](https://github.com/eclipse-kuksa/kuksa-android-sdk/tree/main/docs/QUICKSTART.md) for 
additional integration options.

### GitHub packages

The Kuksa SDK is currently uploaded to GitHub packages where an authentication is needed to download the dependency.
It is recommended to not check these information into your version control.

```
maven {
    url = uri("https://maven.pkg.github.com/eclipse-kuksa/kuksa-android-sdk")
    credentials {
        username = <USERNAME>
        password = <GITHUB_TOKEN>
    }
}
```

## Usage

```
private var dataBrokerConnection: DataBrokerConnection? = null

fun connectInsecure(host: String, port: Int) {
    lifecycleScope.launch {
        val managedChannel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()

        val connector = DataBrokerConnector(managedChannel)
            dataBrokerConnection = connector.connect()
            // Connection to the DataBroker successfully established
        } catch (e: DataBrokerException) {
            // Connection to the DataBroker failed
        }
    }
}
```

```
fun fetch() {
    lifecycleScope.launch {
        val property = Property("Vehicle.Speed")
        val response = dataBrokerConnection?.fetch(property) ?: return@launch
        val value = response.firstValue
        val speed = value.float
    }
}
```

Refer to the [Quickstart guide](https://github.com/eclipse-kuksa/kuksa-android-sdk/tree/main/docs/QUICKSTART.md) or
[class diagrams](https://github.com/eclipse-kuksa/kuksa-android-sdk/blob/main/docs/kuksa-sdk_class-diagram.puml) for 
further insight into the Kuksa SDK API. You can also checkout the [sample](https://github.com/eclipse-kuksa/kuksa-android-sdk/tree/main/samples) implementation.

## Requirements

- A working setup requires at least a running Kuksa [Databroker](https://github.com/eclipse/kuksa.val/tree/master/kuksa_databroker) 
- Optional: The [Kuksa Databroker CLI](https://github.com/eclipse/kuksa.val/tree/master/kuksa_databroker) can be used to manually feed data and test your app.
- Optional: The [Kuksa (Mock)service](https://github.com/eclipse/kuksa.val.services/tree/main/mock_service) can be used to simulate a "real" environment.

## Contribution

Please feel free create [GitHub issues](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues) and contribute 
[(Guidelines)](https://github.com/eclipse-kuksa/kuksa-android-sdk/blob/main/docs/CONTRIBUTING.md).

## License

The Kuksa Android SDK is provided under the terms of the [Apache Software License 2.0](https://github.com/eclipse/kuksa.val/blob/master/LICENSE).
