package org.neotech.app.abysner.data.settings

import org.neotech.app.abysner.data.settings.resources.SettingsResourceV1
import org.neotech.app.abysner.domain.settings.model.SettingsModel

fun SettingsModel.toResource() = SettingsResourceV1(
    showBasicDecoTable = showBasicDecoTable,
    termsAndConditionsAccepted = termsAndConditionsAccepted
)

fun SettingsResourceV1.toModel() = SettingsModel(
    showBasicDecoTable = showBasicDecoTable,
    termsAndConditionsAccepted = termsAndConditionsAccepted
)
