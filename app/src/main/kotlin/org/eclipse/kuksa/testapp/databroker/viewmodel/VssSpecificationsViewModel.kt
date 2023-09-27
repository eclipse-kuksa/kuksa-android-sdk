package org.eclipse.kuksa.testapp.databroker.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.eclipse.kuksa.vss.VssVehicle
import org.eclipse.kuksa.vsscore.model.model.VssSpecification
import org.eclipse.kuksa.vsscore.model.model.heritage

class VssSpecificationsViewModel : ViewModel() {
    var onGetSpecification: (specification: VssSpecification) -> Unit = { }
    var onSubscribeSpecification: (specification: VssSpecification) -> Unit = { }

    private val vssVehicle = VssVehicle()
    val specifications = listOf(vssVehicle) + vssVehicle.heritage

    var specification: VssSpecification by mutableStateOf(vssVehicle)
        private set

    fun updateSpecification(specification: VssSpecification) {
        this.specification = specification
    }
}
