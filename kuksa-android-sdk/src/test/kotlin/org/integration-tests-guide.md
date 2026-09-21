While DataBrokerConnection#fetch and DataBrokerConnection#update are running synchronously the observers of the
corresponding SubscriptionChannels run asynchronously. Between the update executed using #updateRandomXXXValue
here and the propagation to the subscribed observers a small timeframe may pass. Therefore things like this most
likely won't work because of the behavior described above:
```
    val request = SubscribeRequest("Vehicle.Speed")
    subscribe(request, observer)
    updateRandomFloatValue("Vehicle.Speed")
    verify { observer.onEntryChanged("Vehicle.Speed", any())}
```

Keep in mind, that when calling DataBrokerConnection#subscribe an initial update with the current value is send,
to the subscribing observer, meaning that checking for a successful update requires at least two calls to the
#onEntryChanged method. One for the initial update and one for the consecutive, real update of the value.

Using verify(timeout = 100\[, exactly = 1\]) alone won't fix this issue, because "exactly = 1" will always be 
fulfilled by the initial update mentioned above. It therefore needs to be something like 
verify(timeout = 100, exactly = 2) if an update is expected.

A better example therefore would be:
```
    val request = SubscribeRequest("Vehicle.Speed")
    subscribe(request, observer) // triggers first #onEntryChanged with initial value
    val value = updateRandomFloatValue() // triggers second #onEntryChanged with updated value

    val dataEntries = mutableListOf<DataEntry>()

    verify (timeout = 100, exactly = 2) { // initial update +  "updateRandomFloatValue")
        observer.onEntryChanged("Vehicle.Speed", capture(dataEntries))
    }

    val count = dataEntries.count { it.value.float == randomFloatValue }
    count shouldBe 1
```
