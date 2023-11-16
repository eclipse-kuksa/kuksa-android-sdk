## Introduction

## Integration

*build.gradle*
```
implementation(org.eclipse.kuksa:kuksa-sdk:<VERSION>)
```

## Connecting to the DataBroker

You can use the following snippet for a simple (unsecure) connection to the DataBroker. See the samples package for
a detailed implementation or how to connect in a secure way with a certificate.

```
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

## Interacting with the DataBroker

```
fun fetch() {
    lifecycleScope.launch {
        val property = Property("Vehicle.Speed")
        val response = dataBrokerConnection?.fetch(property) ?: return@launch
        val value = response.firstValue
        val speed = value.float
    }
}

fun update(property: Property, datapoint: Datapoint) {
    lifecycleScope.launch {
        val property = Property("Vehicle.Speed")
        val datapoint = Datapoint.newBuilder().float = 50f
        dataBrokerConnection?.update(property, datapoint)
    }
}

 fun subscribe(property: Property) {
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

## Specifications Symbol Processing

The generic nature of using the `Property` API will result into an information loss of the type which can be seen in 
the `subscribe` example. It requires an additional check of the `vssPath` when receiving an updated value to correctly 
cast it back. This is feasible for simple use cases but can get tedious when working with a lot of vehicle signals.

For a more convenient usage you can opt in to auto generate Kotlin models of the same specification the DataBroker uses.

*build.gradle*
```
ksp(org.eclipse.kuksa:vss-processor:<VERSION>)
```

Use the new `VssDefinition` annotation and provide the path to the specification file (Inside the assets folder).
This will generate a complete tree of Kotlin models which can be used in combination with the SDK API. This way you can
work safe types which are also returned again when interacting with the Kuksa SDK. There is also a whole set of 
convenience operators and methods to work with the tree data. See the `VssSpecification` class documentation for this.

```
@VssDefinition("vss_rel_4.0.yaml") 
class MainActivity
```

*Example .yaml specification file*
```
Vehicle.Speed:
  datatype: float
  description: Vehicle speed.
  type: sensor
  unit: km/h
  uuid: efe50798638d55fab18ab7d43cc490e9
```

*Example model*
```
public data class VssSpeed @JvmOverloads constructor(
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
}
```

*Specification API*
```
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
        val updatedSpeed = dataBrokerConnection?.update(vssSpeed)
    }
}

fun subscribe() {
    val vssSpeed = VssVehicle.VssSpeed(value = 100f)
    dataBrokerConnection?.subscribe(vssSpeed, listener = object : VssSpecificationListener<VssVehicle.VssSpeed> {
        override fun onSpecificationChanged(vssSpecification: VssVehicle.VssSpeed) {
            val speed = vssSpeed.value
        }
    })
}
```
