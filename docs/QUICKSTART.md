## Introduction

Get instantly bootstrapped into the world of the Kuksa SDK with the following code snippets!

## Integration

*build.gradle*
```
implementation("org.eclipse.kuksa:kuksa-sdk:<VERSION>")
```

## Connecting to the DataBroker

You can use the following snippet for a simple (unsecure) connection to the DataBroker. This highly depends on your 
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

        val connector = DataBrokerConnector(managedChannel)
        try {
            dataBrokerConnection = connector.connect()
            // Connection to the DataBroker successfully established
        } catch (e: DataBrokerException) {
            // Connection to the DataBroker failed
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

    DataBrokerConnector connector = new DataBrokerConnector(managedChannel);
    connector.connect(new CoroutineCallback<DataBrokerConnection>() {
        @Override
        public void onSuccess(DataBrokerConnection result) {
            dataBrokerConnection = result;
        }
        
        @Override
        public void onError(@NonNull Throwable error) {
            // Connection to the DataBroker failed
        }
    });
}
```

## Interacting with the DataBroker

*Kotlin*
```kotlin
fun fetch() {
    lifecycleScope.launch {
        val property = Property("Vehicle.Speed", listOf(Field.FIELD_VALUE))
        val response = dataBrokerConnection?.fetch(property) ?: return@launch
        val entry = response.entriesList.first() // Don't forget to handle empty responses
        val value = entry.value
        val speed = value.float
    }
}

fun update() {
    lifecycleScope.launch {
        val property = Property("Vehicle.Speed", listOf(Field.FIELD_VALUE))
        val datapoint = Datapoint.newBuilder().setFloat(100f).build()
        dataBrokerConnection?.update(property, datapoint)
    }
}

fun subscribe() {
    val property = Property("Vehicle.Speed", listOf(Field.FIELD_VALUE))
    val propertyListener = object : PropertyListener {
        override fun onPropertyChanged(vssPath: String, field: Types.Field, updatedValue: Types.DataEntry) {
             when (vssPath) {
                "Vehicle.Speed" -> {
                    val speed = updatedValue.value.float
                }
            }
        }
    }

    dataBrokerConnection?.subscribe(property, propertyListener)
}
```
*Java*
```java
void fetch() {
    Property property = new Property("Vehicle.Speed", Collections.singleton(Types.Field.FIELD_VALUE));
    dataBrokerConnection.fetch(property, new CoroutineCallback<GetResponse>() {
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
    Property property = new Property("Vehicle.Speed", Collections.singleton(Types.Field.FIELD_VALUE));
    Datapoint datapoint = Datapoint.newBuilder().setFloat(100f).build();
    dataBrokerConnection.update(property, datapoint, new CoroutineCallback<KuksaValV1.SetResponse>() {
        @Override
        public void onSuccess(KuksaValV1.SetResponse result) {
          // handle result
        }
    });
}

void subscribe() {
    Property property = new Property("Vehicle.Speed", Collections.singleton(Types.Field.FIELD_VALUE));
    dataBrokerConnection.subscribe(property, new PropertyListener() {
        @Override
        public void onPropertyChanged(
            @NonNull String vssPath,
            @NonNull Types.Field field,
            @NonNull Types.DataEntry updatedValue) {
            
            switch (vssPath) {
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

## Specifications Symbol Processing

The generic nature of using the `Property` API will result into an information loss of the type which can be seen in 
the `subscribe` example. You may choose to reuse the same listener for multiple properties. Then it requires an additional check 
of the `vssPath` after receiving an updated value to correctly cast it back. This is feasible for simple use cases but can get tedious when working with a lot of vehicle signals.

For a more convenient usage you can opt in to auto generate Kotlin models via [Symbol Processing](https://kotlinlang.org/docs/ksp-quickstart.html) 
of the same specification the DataBroker uses. For starters you can retrieve an extensive default specification from the
release page of the [COVESA Vehicle Signal Specification GitHub repository](https://github.com/COVESA/vehicle_signal_specification/releases).

*build.gradle*
```
ksp("org.eclipse.kuksa:vss-processor:<VERSION>")
```

Use the new [`VssDefinition`](https://github.com/eclipse-kuksa/kuksa-android-sdk/blob/main/vss-core/src/main/java/org/eclipse/kuksa/vsscore/annotation/VssDefinition.kt) annotation and provide the path to the specification file (Inside the assets folder).
Doing so will generate a complete tree of Kotlin models which can be used in combination with the SDK API. This way you can
work with safe types and the SDK takes care of all the model parsing for you. There is also a whole set of 
convenience operators and extension methods to work with to manipulate the tree data. See the `VssSpecification` class documentation for this.

*Kotlin*
```kotlin
@VssDefinition("vss_rel_4.0.yaml") 
class KotlinActivity
```
*Java*
```java
@VssDefinition(vssDefinitionPath = "vss_rel_4.0.yaml")
public class JavaActivity
```
> [!IMPORTANT]
> Keep in mind to always synchronize the specification file between the client and the DataBroker.


*Example .yaml specification file*
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
  ) : VssProperty<Float> {
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

    override val children: Set<VssSpecification>
      get() = setOf()

    override val parentClass: KClass<*>?
      get() = VssVehicle::class
}
```

### Specification API

*Kotlin*
```kotlin
fun fetch() {
    lifecycleScope.launch {
        val vssSpeed = VssVehicle.VssSpeed()
        val updatedSpeed = dataBrokerConnection?.fetch(vssSpeed)
        val speed = updatedSpeed?.value
    }
}

fun update() {
    lifecycleScope.launch {
        val vssSpeed = VssVehicle.VssSpeed(value = 100f)
        dataBrokerConnection?.update(vssSpeed)
    }
}

fun subscribe() {
    val vssSpeed = VssVehicle.VssSpeed(value = 100f)
    dataBrokerConnection?.subscribe(vssSpeed, listener = object : VssSpecificationListener<VssVehicle.VssSpeed> {
        override fun onSpecificationChanged(vssSpecification: VssVehicle.VssSpeed) {
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
    dataBrokerConnection.fetch(
        vssSpeed,
        Collections.singleton(Types.Field.FIELD_VALUE),
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
    dataBrokerConnection.update(
        vssSpeed,
        Collections.singleton(Types.Field.FIELD_VALUE),
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
    dataBrokerConnection.subscribe(
        vssSpeed,
        Collections.singleton(Types.Field.FIELD_VALUE),
        new VssSpecificationListener<VssVehicle.VssSpeed>() {
            @Override
            public void onSpecificationChanged(@NonNull VssVehicle.VssSpeed vssSpecification) {
                Float speed = vssSpecification.getValue();
            }
    
            @Override
            public void onError(@NonNull Throwable throwable) {
                // handle error
            }
        }
    );
}
```
