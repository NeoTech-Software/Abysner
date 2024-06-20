import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.neotech.app.abysner.domain.persistence.PersistenceRepository
import org.neotech.app.abysner.domain.persistence.get
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.domain.settings.SettingsRepository

@Inject
class SettingsRepositoryImpl(
    private val persistenceRepository: PersistenceRepository,
) : SettingsRepository {

    override val settings: MutableStateFlow<SettingsModel> = MutableStateFlow(SettingsModel())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            persistenceRepository.getPreferences().collect { preferences ->
                settings.update {
                    it.updateWith(preferences)
                }
            }
        }
    }

    private fun SettingsModel.updateWith(preferences: Preferences): SettingsModel {
        return copy(
            showBasicDecoTable = preferences.get(PREFERENCE_KEY_BASIC_DECO_TABLE, false),
            termsAndConditionsAccepted = preferences.get(PREFERENCE_KEY_TERMS_AND_CONDITIONS_ACCEPTED, false)
        )
    }

    override fun updateSettings(updateBlock: (SettingsModel) -> SettingsModel) {
        val newSettings = updateBlock(settings.value)
        settings.update {
            newSettings
        }
        scope.launch {
            persistenceRepository.updatePreferences {

                it[PREFERENCE_KEY_BASIC_DECO_TABLE] = newSettings.showBasicDecoTable
            }
        }
    }

    override suspend fun setTermsAndConditionsAccepted(accepted: Boolean) {
        persistenceRepository.updatePreferences {
            it[PREFERENCE_KEY_TERMS_AND_CONDITIONS_ACCEPTED] = accepted
        }
    }

    override suspend fun getSettings(): SettingsModel {
        return SettingsModel().updateWith(persistenceRepository.getPreferences().first())
    }
}

private val PREFERENCE_KEY_BASIC_DECO_TABLE = booleanPreferencesKey("settings.basicDecoTable")
private val PREFERENCE_KEY_TERMS_AND_CONDITIONS_ACCEPTED = booleanPreferencesKey("settings.termsAndConditions.accepted")
