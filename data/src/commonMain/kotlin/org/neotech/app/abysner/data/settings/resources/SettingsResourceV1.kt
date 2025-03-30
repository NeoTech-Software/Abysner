package org.neotech.app.abysner.data.settings.resources

import kotlinx.serialization.Serializable
import org.neotech.app.abysner.data.SerializableResource

@Serializable
data class SettingsResourceV1(
    val showBasicDecoTable: Boolean,
    val termsAndConditionsAccepted: Boolean,
): SerializableResource
