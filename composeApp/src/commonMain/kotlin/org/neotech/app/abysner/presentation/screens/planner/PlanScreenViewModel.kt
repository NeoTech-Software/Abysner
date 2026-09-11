/*
 * Abysner - Dive planner
 * Copyright (C) 2024-2026 Neotech
 *
 * Abysner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3,
 * as published by the Free Software Foundation.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.neotech.app.abysner.presentation.screens.planner

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.DiveMode
import org.neotech.app.abysner.domain.diveplanning.MultiDivePlanner
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.MultiDivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.diveplanning.model.toggleAvailableForBailout
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import org.neotech.app.abysner.presentation.utilities.combine
import kotlin.time.Duration
import kotlin.time.measureTimedValue

@OptIn(FlowPreview::class)
@Inject
class PlanScreenViewModel(
    private val planningRepository: PlanningRepository,
    private val settingsRepository: SettingsRepository,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    calculationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private data class PlanInput(
        val model: MultiDivePlanInputModel = MultiDivePlanInputModel.Default,
        val selectedDiveIndex: Int = 0,
    )

    private val planInput = MutableStateFlow(PlanInput())
    private val isCalculatingDivePlan = MutableStateFlow(false)
    private val isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch(ioDispatcher) {
            val loaded = planningRepository.getMultiDivePlanInput() ?: MultiDivePlanInputModel.Default
            // Recompute cylinder lock state for every dive after loading persisted data.
            val recomputed = loaded.copy(dives = loaded.dives.map { it.recomputeCylinderState() })
            planInput.update { it.copy(model = recomputed) }
            isLoading.value = false

            planInput.map { it.model }
                .distinctUntilChanged()
                .debounce(1000)
                .collectLatest { model ->
                    runCatching {
                        planningRepository.setMultiDivePlanInput(model)
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
        }
    }

    private fun mutateDive(mutation: DivePlanInputModel.() -> DivePlanInputModel) {
        planInput.update { state ->
            state.copy(model = state.model.updateDive(state.selectedDiveIndex, mutation))
        }
    }

    fun addSegment(section: DiveProfileSection) = mutateDive { addSegment(section) }
    fun updateSegment(index: Int, section: DiveProfileSection) = mutateDive { updateSegment(index, section) }
    fun removeSegment(index: Int) = mutateDive { removeSegment(index) }
    fun addCylinder(cylinder: Cylinder) = mutateDive { addCylinder(cylinder) }
    fun updateCylinder(cylinder: Cylinder) = mutateDive { updateCylinder(cylinder) }
    fun removeCylinder(cylinder: Cylinder) = mutateDive { removeCylinder(cylinder) }
    fun toggleCylinder(cylinder: Cylinder, enabled: Boolean) = mutateDive { toggleCylinder(cylinder, enabled) }
    fun setContingency(deeper: Boolean, longer: Boolean, bailout: Boolean) = mutateDive { setContingency(deeper, longer, bailout) }
    fun setDiveMode(mode: DiveMode) = mutateDive { setDiveMode(mode) }
    fun toggleAvailableForBailout(cylinder: Cylinder, availableForBailout: Boolean) = mutateDive { toggleAvailableForBailout(cylinder, availableForBailout) }

    fun onEditDive() {
        if (settingsRepository.settings.value.showDiveEditTooltip) {
            settingsRepository.updateSettings { it.copy(showDiveEditTooltip = false) }
        }
    }

    fun selectDive(index: Int) {
        planInput.update { it.copy(selectedDiveIndex = index) }
    }

    fun addDive(surfaceInterval: Duration) {
        planInput.update { state ->
            val newDive = DivePlanInputModel.Default.copy(surfaceIntervalBefore = surfaceInterval)
            state.copy(
                model = state.model.copy(dives = state.model.dives + newDive),
                // Switch to the newly added dive as the selected dive
                selectedDiveIndex = state.model.dives.size,
            )
        }
    }

    fun removeDive(index: Int) {
        if (planInput.value.model.dives.size <= 1) {
            return
        }
        planInput.update { state ->
            val newDives = state.model.dives.toMutableList().apply { removeAt(index) }
            if (index == 0) {
                // If the first dive got removed, set the surface interval of the new first dive to null
                newDives[0] = newDives[0].copy(surfaceIntervalBefore = null)
            }
            state.copy(
                model = state.model.copy(dives = newDives),
                // Keep the selected dive index the same (usually the dive that is selected will be
                // removed), if that index is no longer valid, we set it to the last dive.
                selectedDiveIndex = state.selectedDiveIndex.coerceAtMost(newDives.lastIndex),
            )
        }
    }

    fun updateSurfaceInterval(index: Int, duration: Duration) {
        require(index >= 1) { "The first dive cannot have a surface interval before it." }
        planInput.update { state ->
            state.copy(model = state.model.updateDive(index) { copy(surfaceIntervalBefore = duration) })
        }
    }

    /**
     * Reacts only when the model changes, not when selectedDiveIndex changes, so switching dives
     * does not retrigger a potentially expensive recalculation.
     */
    private val divePlanSet: StateFlow<Result<MultiDivePlanSet?>> = combine(
        planInput.map { it.model }.distinctUntilChanged(),
        planningRepository.configuration,
        settingsRepository.settings.map { it.unitSystem }.distinctUntilChanged(),
    ) { model, config, unitSystem ->
        isCalculatingDivePlan.value = true
        val result = measureTimedValue {
            runCatching { MultiDivePlanner(config, unitSystem).plan(model) }
                .onFailure { it.printStackTrace() }
        }.also { isCalculatingDivePlan.value = false }
        println("Duration: Calculating dive plan took ${result.duration}")
        result.value
    }.flowOn(calculationDispatcher).stateIn(
        viewModelScope,
        // This calculation is quite heavy, we don't want to re-run it when in a short time period
        // after the last subscription ends a new subscription appears.
        // However, we also do not want to keep subscribed, since that could trigger calculations in
        // the background (while the user is not on this screen), which is also wasteful.
        // SUBSCRIPTION_TIME_OUT is a carefully chosen middle ground.
        SharingStarted.WhileSubscribed(SUBSCRIPTION_TIME_OUT),
        Result.success(null)
    )

    val uiState: StateFlow<UiState> = combine(
        planInput,
        divePlanSet,
        isLoading,
        isCalculatingDivePlan,
        settingsRepository.settings,
        // In theory, we can read this directly from the repository, since divePlanSet which this combine depends on already observes it.
        planningRepository.configuration,
    ) { input, plan, loading, isCalc, settings, configuration ->
        val selectedDive = input.model.dives[input.selectedDiveIndex]
        UiState(
            selectedDiveIndex = input.selectedDiveIndex,
            dives = input.model.dives,
            segments = selectedDive.plannedProfile,
            availableGas = selectedDive.cylinders,
            diveMode = selectedDive.diveMode,
            isCalculatingDivePlan = isCalc,
            multiDivePlanSet = plan,
            selectedDivePlanSet = plan.map { it?.divePlanSets?.getOrNull(input.selectedDiveIndex) },
            isLoading = loading,
            settingsModel = settings,
            configuration = configuration,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = UiState()
    )

    @Immutable
    data class UiState(
        val selectedDiveIndex: Int = 0,
        val dives: List<DivePlanInputModel> = listOf(DivePlanInputModel.Default),
        val segments: List<DiveProfileSection> = DivePlanInputModel.Default.plannedProfile,
        val availableGas: List<PlannedCylinderModel> = DivePlanInputModel.Default.cylinders,
        val diveMode: DiveMode = DiveMode.OPEN_CIRCUIT,
        val multiDivePlanSet: Result<MultiDivePlanSet?> = Result.success(null),
        val selectedDivePlanSet: Result<DivePlanSet?> = Result.success(null),
        val isCalculatingDivePlan: Boolean = false,
        val isLoading: Boolean = true,
        val settingsModel: SettingsModel = SettingsModel(),
        val configuration: Configuration = Configuration(),
    )
}

private const val SUBSCRIPTION_TIME_OUT: Long = 5 * 60 * 1000
