package org.eclipse.kuksa.vssprocessor

import org.eclipse.kuksa.vssprocessor.VssDefinitionProcessor.VssSpecificationElement
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

internal interface VssDefinitionParser {
    /**
     * @param definitionFile to parse [VssSpecificationElement] with
     */
    fun parseSpecifications(definitionFile: File): List<VssSpecificationElement>
}

/**
 * @param fields to set via reflection. Pair<PropertyName, anyValue>.
 * @param remapNames which can be used if the propertyName does not match with the input name
 */
fun VssSpecificationElement.setFields(
    fields: List<Pair<String, Any?>>,
    remapNames: Map<String, String> = emptyMap(),
) {
    val nameToProperty = this::class.memberProperties.associateBy(KProperty<*>::name)

    val remappedFields = fields.toMutableList()
    remapNames.forEach { (propertyName, newName) ->
        val find = fields.find { it.first == propertyName } ?: return@forEach
        remappedFields.remove(find)
        remappedFields.add(Pair(find.first, newName))
    }

    remappedFields.forEach { (propertyName, propertyValue) ->
        nameToProperty[propertyName]
            .takeIf { it is KMutableProperty<*> }
            ?.let { it as KMutableProperty<*> }
            ?.setter?.call(this, propertyValue)
    }
}
