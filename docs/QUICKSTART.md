## Introduction

Get instantly bootstrapped into the world of the KUKSA SDK with the following code snippets!

## Integration

*app/build.gradle.kts*
```
implementation("org.eclipse.kuksa:kuksa-sdk:<VERSION>")
```

## Connecting to the Databroker

You can use the following snippet for a simple (unsecure) connection to the Databroker. This highly depends on your 
setup so see the [samples package](https://github.com/eclipse-kuksa/kuksa-android-sdk/blob/main/samples/src/main/kotlin/com/example/sample/KotlinActivity.kt)
for a detailed implementation or how to connect in a secure way with a certificate.

*Kotlin*
```kotlin
private var dataBrokerConnection: DataBrokerConnection? = null

fun connectInsecure(host: String, port: Int) {
    lifecycleScope.launch {
        val managedChannel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()

        // or jsonWebToken = null when authentication is disabled
        val jsonWebToken = JsonWebToken("someValidJwt") 
        val connector = DataBrokerConnector(managedChannel, jsonWebToken)
        try {
            dataBrokerConnection = connector.connect()
            // Connection to the Databroker successfully established
        } catch (e: DataBrokerException) {
            // Connection to the Databroker failed
        }
    }
}
```
*Java*
```java
void connectInsecure(String host, int port) {
    ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(host, port)
        .usePlaintext()
        .build();

    // or jsonWebToken = null when authentication is disabled
    JsonWebToken jsonWebToken = new JsonWebToken("someValidJwt");

    DataBrokerConnector connector = new DataBrokerConnector(managedChannel, jsonWebToken);
    connector.connect(new CoroutineCallback<DataBrokerConnection>() {
        @Override
        public void onSuccess(DataBrokerConnection result) {
            dataBrokerConnection = result;
        }
        
        @Override
        public void onError(@NonNull Throwable error) {
            // Connection to the Databroker failed
        }
    });
}
```

## Interacting with the Databroker

*Kotlin*
```kotlin
fun fetch() {
    lifecycleScope.launch {
        val request = FetchRequest("Vehicle.Speed", setOf(Field.FIELD_VALUE))
        val response = dataBrokerConnection?.fetch(request) ?: return@launch
        val entry = response.entriesList.first() // Don't forget to handle empty responses
        val value = entry.value
        val speed = value.float
    }
}

fun update() {
    lifecycleScope.launch {
        val request = UpdateRequest("Vehicle.Speed", setOf(Field.FIELD_VALUE))
        val datapoint = Datapoint.newBuilder().setFloat(100f).build()
        dataBrokerConnection?.update(request, datapoint)
    }
}

fun subscribe() {
    val request = SubscribeRequest("Vehicle.Speed", setOf(Field.FIELD_VALUE))
    val listener = object : VssPathListener {
        override fun onEntryChanged(entryUpdates: List<KuksaValV1.EntryUpdate>) {
            entryUpdates.forEach { entryUpdate ->
                val updatedValue = entryUpdate.entry

                // handle entry change
                when (updatedValue.path) {
                    "Vehicle.Speed" -> {
                        val speed = updatedValue.value.float
                    }
            }
        }
    }

    dataBrokerConnection?.subscribe(request, listener)
}
```
*Java*
```java
void fetch() {
    FetchRequest request = new FetchRequest("Vehicle.Speed", Collections.singleton(Types.Field.FIELD_VALUE));
    dataBrokerConnection.fetch(request, new CoroutineCallback<GetResponse>() {
        @Override
        public void onSuccess(GetResponse result) {
            result.entriesList.first() // Don't forget to handle empty responses
            Types.DataEntry dataEntry = result.getEntriesList().get(0);
            Datapoint datapoint = dataEntry.getValue();
            float speed = datapoint.getFloat();
        }
    });
}

void update() {
    Datapoint datapoint = Datapoint.newBuilder().setFloat(100f).build();
    UpdateRequest request = new UpdateRequest("Vehicle.Speed", datapoint, Collections.singleton(Types.Field.FIELD_VALUE));
    dataBrokerConnection.update(request, new CoroutineCallback<KuksaValV1.SetResponse>() {
        @Override
        public void onSuccess(KuksaValV1.SetResponse result) {
        // handle result
        }
    });
}

void subscribe() {
    SubscribeRequest request = new SubscribeRequest("Vehicle.Speed", Collections.singleton(Types.Field.FIELD_VALUE));
    dataBrokerConnection.subscribe(request, new VssPathListener() {
        @Override
        public void onEntryChanged(@NonNull List<EntryUpdate> entryUpdates) {
            for (KuksaValV1.EntryUpdate entryUpdate : entryUpdates) {
            Types.DataEntry updatedValue = entryUpdate.getEntry();

            // handle entry change
            switch (updatedValue.getPath()) {
                case "Vehicle.Speed":
                float speed = updatedValue.getValue().getFloat();
            }
        }
        
        @Override
        public void onError(@NonNull Throwable throwable) {
            // handle error
        }
    });
}
```

## VSS Model Generation

The generic nature of using the `VSS Path` API will result into an information loss of the type which can be seen in 
the `subscribe` example. You may choose to reuse the same listener for multiple VSS paths. Then it requires an additional check 
of the `vssPath` after receiving an updated value to correctly cast it back. This is feasible for simple use cases but can get tedious 
when working with a lot of vehicle signals.

For a more convenient usage you can opt in to auto generate Kotlin models via [Symbol Processing](https://kotlinlang.org/docs/ksp-quickstart.html) 
of the same specification the Databroker uses. For starters you can retrieve an extensive default specification from the
release page of the [COVESA Vehicle Signal Specification GitHub repository](https://github.com/COVESA/vehicle_signal_specification/releases).

Currently VSS specification files in .yaml and .json format are supported by the vss-processor.

*app/build.gradle.kts*
```kotlin
plugins {
    id("org.eclipse.kuksa.vss-processor-plugin")
}

dependencies {
    ksp("org.eclipse.kuksa:vss-processor:<VERSION>")
}

// Optional - See plugin documentation. Files inside the "$rootDir/vss" folder are used automatically.
vssProcessor {
    searchPath = "$rootDir/vss"
}
```

Use the [`VssModelGenerator`](https://github.com/eclipse-kuksa/kuksa-android-sdk/blob/main/vss-core/src/main/java/org/eclipse/kuksa/vsscore/annotation/VssModelGenerator.kt) annotation and provide the path to the VSS file (Inside the assets folder).
Doing so will generate a complete tree of Kotlin models which can be used in combination with the SDK API. This way you can
work with safe types and the SDK takes care of all the model parsing for you. There is also a whole set of 
convenience operators and extension methods to work with to manipulate the tree data. See the `VssNode` class documentation for this.

```kotlin / Java
@VssModelGenerator 
class Activity
```
> [!IMPORTANT]
> Keep in mind to always synchronize a compatible (e.g. subset) VSS file between the client and the Databroker.


*Example .yaml VSS file*
```yaml
Vehicle.Speed:
  datatype: float
  description: Vehicle speed.
  type: sensor
  unit: km/h
  uuid: efe50798638d55fab18ab7d43cc490e9
```

*Example model*

```kotlin
data class VssSpeed @JvmOverloads constructor(
    override val `value`: Float = 0f,
) : VssNode<Float> {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Vehicle speed."

    override val type: String
        get() = "sensor"

    override val uuid: String
        get() = "efe50798638d55fab18ab7d43cc490e9"

    override val vssPath: String
        get() = "Vehicle.Speed"

    override val children: Set<VssNode>
        get() = setOf()

    override val parentClass: KClass<*>
        get() = VssVehicle::class
}
```

### API for generated VSS models

*Kotlin*
```kotlin
fun fetch() {
    lifecycleScope.launch {
        val vssSpeed = VssVehicle.VssSpeed()
        val request = VssNodeFetchRequest(vssSpeed)
        val updatedSpeed = dataBrokerConnection?.fetch(request)
        val speed = updatedSpeed?.value
    }
}

fun update() {
    lifecycleScope.launch {
        val vssSpeed = VssVehicle.VssSpeed(value = 100f)
        val request = VssNodeUpdateRequest(vssSpeed)
        dataBrokerConnection?.update(request)
    }
}

fun subscribe() {
    val vssSpeed = VssVehicle.VssSpeed(value = 100f)
    val request = VssNodeSubscribeRequest(vssSpeed)
    dataBrokerConnection?.subscribe(request, listener = object : VssNodeListener<VssVehicle.VssSpeed> {
        override fun onNodeChanged(vssNode: VssVehicle.VssSpeed) {
            val speed = vssSpeed.value
        }

        override fun onError(throwable: Throwable) {
            // handle error
        }
    })
}
```
*Java*
```java
void fetch() {
    VssVehicle.VssSpeed vssSpeed = new VssVehicle.VssSpeed();
    VssNodeFetchRequest request = new VssNodeFetchRequest(vssSpeed)
    dataBrokerConnection.fetch(
        request,
        new CoroutineCallback<VssVehicle.VssSpeed>() {
            @Override
            public void onSuccess(@Nullable VssVehicle.VssSpeed result) {
                Float speed = result.getValue();
            }
    
            @Override
            public void onError(@NonNull Throwable error) {
               // handle error
            }
        }
    );
}

void update() {
    VssVehicle.VssSpeed vssSpeed = new VssVehicle.VssSpeed(100f);
    VssNodeUpdateRequest request = new VssNodeUpdateRequest(vssSpeed)
    dataBrokerConnection.update(
        request,
        new CoroutineCallback<Collection<? extends KuksaValV1.SetResponse>>() {
            @Override
            public void onSuccess(@Nullable Collection<? extends KuksaValV1.SetResponse> result) {
                // handle result
            }
            
            @Override
            public void onError(@NonNull Throwable error) {
                // handle error
            }
        }
    );
}

void subscribe() {
    VssVehicle.VssSpeed vssSpeed = new VssVehicle.VssSpeed();
    VssNodeSubscribeRequest request = new VssNodeSubscribeRequest(vssSpeed)
    dataBrokerConnection.subscribe(
        request,
        new VssNodeListener<VssVehicle.VssSpeed>() {
            @Override
            public void onNodeChanged(@NonNull VssVehicle.VssSpeed vssNode) {
                Float speed = vssNode.getValue();
            }
    
            @Override
            public void onError(@NonNull Throwable throwable) {
                // handle error
            }
        }
    );
}
```
