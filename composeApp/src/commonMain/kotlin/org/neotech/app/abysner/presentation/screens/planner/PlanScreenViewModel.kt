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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import org.neotech.app.abysner.presentation.utilities.combine
import org.neotech.app.abysner.domain.core.model.Configuration
import org.neotech.app.abysner.domain.core.model.Cylinder
import org.neotech.app.abysner.domain.core.model.Gas
import org.neotech.app.abysner.domain.diveplanning.DivePlanner
import org.neotech.app.abysner.domain.diveplanning.PlanningRepository
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanInputModel
import org.neotech.app.abysner.domain.diveplanning.model.DivePlanSet
import org.neotech.app.abysner.domain.diveplanning.model.DiveProfileSection
import org.neotech.app.abysner.domain.diveplanning.model.PlannedCylinderModel
import org.neotech.app.abysner.domain.gasplanning.GasPlanner
import org.neotech.app.abysner.domain.settings.SettingsRepository
import org.neotech.app.abysner.domain.settings.model.SettingsModel
import kotlin.time.measureTimedValue

@OptIn(FlowPreview::class)
@Inject
class PlanScreenViewModel(
    planningRepository: PlanningRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val inputState = MutableStateFlow(defaultDivePlanInputModel)
    private val isCalculatingDivePlan = MutableStateFlow(false)
    private val isLoading = MutableStateFlow(true)

    /**
     * The downside of using combine on a StateFlow is that it returns a cold flow, meaning that
     * everytime it gains a subscriber the `transform` block will run again. If combine would return
     * a StateFlow as well... then this `transform` block would not need execution again, because the
     * result is cached.
     */
    private val divePlanSet: StateFlow<Result<DivePlanSet?>> = combine(
        flow = inputState,
        flow2 = planningRepository.configuration,
    ) { inputState, configuration, ->
        isCalculatingDivePlan.value = true
        val result = measureTimedValue {
            calculateDivePlan(inputState, configuration)
        }.also {
            isCalculatingDivePlan.value = false
        }
        println("Duration: Calculating dive plan took ${result.duration}")
        result.value
    }.flowOn(Dispatchers.IO)
        // This calculation is quite heavy, we don't want to re-run it when in a short time period the
        // last subscription ends after which a new subscription appears (in a short time period).
        // However we also do not want to keep subscribed, since that could trigger calculations in
        // the background (while the user is not on this screen), which is also wasteful.
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(SUBSCRIPTION_TIME_OUT),
            Result.success(null)
        )

    val uiState: StateFlow<UiState> =
        combine(inputState, divePlanSet, isLoading, isCalculatingDivePlan, settingsRepository.settings, planningRepository.configuration) {
            input, divePlan, isLoading, isCalculatingDivePlan, settings, configuration ->
            UiState(
                segments = input.plannedProfile,
                availableGas = input.cylinders,
                isCalculatingDivePlan = isCalculatingDivePlan,
                divePlanSet = divePlan,
                isLoading = isLoading,
                settingsModel = settings,
                configuration = configuration,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = UiState()
        )

    init {
        viewModelScope.launch(context = Dispatchers.IO) {

            val loaded = planningRepository.getDivePlanInput() ?: defaultDivePlanInputModel
            inputState.value = loaded.copy(
                // Cylinder locked state is not stored, so recalculate it.
                cylinders = recomputeCylinderState(loaded.plannedProfile, loaded.cylinders)
            )
            //delay(1000)
            isLoading.value = false
            inputState.debounce(1000).collectLatest {
                try {
                    planningRepository.setDivePlanInput(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun recomputeCylinderState(segments: List<DiveProfileSection>, cylinders: List<PlannedCylinderModel>): List<PlannedCylinderModel> {
        val gasesInUse = segments.gasesInUse()
        val autoCheckedGases = mutableSetOf<Gas>()

        val updated = cylinders.map { planned ->
            val gas = planned.cylinder.gas
            val shouldAutoCheck = gas in gasesInUse
                && gas !in autoCheckedGases
                && cylinders.checkedCylinderCountFor(gas) == 0

            if (shouldAutoCheck) {
                autoCheckedGases.add(gas)
                planned.copy(isChecked = true)
            } else {
                planned
            }
        }

        return updated.map { planned ->
            planned.copy(
                isLocked = planned.isChecked
                    && planned.cylinder.gas in gasesInUse
                    && updated.checkedCylinderCountFor(planned.cylinder.gas) == 1
            )
        }
    }

    fun updateSegment(index: Int, diveProfileSection: DiveProfileSection) {
        inputState.update {
            val newSegments = it.plannedProfile.toMutableList().apply {
                set(index, diveProfileSection)
            }
            it.copy(
                plannedProfile = newSegments,
                cylinders = recomputeCylinderState(newSegments, it.cylinders)
            )
        }
    }

    fun addSegment(diveProfileSection: DiveProfileSection) {
        inputState.update {
            val newSegments = it.plannedProfile.plus(diveProfileSection)
            it.copy(
                plannedProfile = newSegments,
                cylinders = recomputeCylinderState(newSegments, it.cylinders)
            )
        }
    }

    fun removeSegment(index: Int) {
        inputState.update {
            val newSegments = it.plannedProfile.toMutableList().apply {
                removeAt(index)
            }
            it.copy(
                plannedProfile = newSegments,
                cylinders = recomputeCylinderState(newSegments, it.cylinders)
            )
        }
    }

    fun addCylinder(cylinder: Cylinder) {
        inputState.update {
            val newCylinders = it.cylinders.plus(
                PlannedCylinderModel(
                    cylinder = cylinder,
                    isChecked = false,
                    isLocked = false
                )
            ).sortedBy { gas -> gas.cylinder.gas.oxygenFraction }
            it.copy(
                cylinders = recomputeCylinderState(it.plannedProfile, newCylinders)
            )
        }
    }

    fun updateCylinder(cylinder: Cylinder) {
        inputState.update { inputState ->
            val index = inputState.cylinders.indexOfFirst { it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier }
            val item = inputState.cylinders[index]
            val newCylinders = inputState.cylinders.toMutableList().apply {
                set(index, item.copy(cylinder = cylinder))
            }
            val newProfile = inputState.plannedProfile.map {
                if (it.cylinder.uniqueIdentifier == cylinder.uniqueIdentifier) {
                    it.copy(cylinder = cylinder)
                } else {
                    it
                }
            }
            inputState.copy(
                cylinders = recomputeCylinderState(newProfile, newCylinders),
                plannedProfile = newProfile
            )
        }
    }

    fun toggleCylinder(cylinder: Cylinder, enabled: Boolean) {
        inputState.update { inputState ->
            val currentModel = inputState.cylinders.find { it.cylinder == cylinder }
            if (currentModel?.isLocked == true) {
                // Last checked cylinder of a gas used in a segment — do not allow toggle.
                inputState
            } else {
                val newCylinders = inputState.cylinders.map { pair ->
                    if (pair.cylinder == cylinder) {
                        pair.copy(isChecked = enabled)
                    } else {
                        pair
                    }
                }
                inputState.copy(
                    cylinders = recomputeCylinderState(inputState.plannedProfile, newCylinders)
                )
            }
        }
    }

    fun removeCylinder(gas: Cylinder) {
        inputState.update { inputState ->
            val currentModel = inputState.cylinders.find { it.cylinder == gas }
            if (currentModel?.isLocked == true) {
                // Last checked cylinder of a gas used in a segment — do not allow removal.
                inputState
            } else {
                val newCylinders = inputState.cylinders.toMutableList().apply {
                    removeAll { pair -> pair.cylinder == gas }
                }
                inputState.copy(
                    cylinders = recomputeCylinderState(inputState.plannedProfile, newCylinders)
                )
            }
        }
    }

    private fun calculateDivePlan(
        inputState: DivePlanInputModel,
        configuration: Configuration,
    ): Result<DivePlanSet> {
        return try {
            val timedResult = measureTimedValue {
                val planner = DivePlanner()
                planner.configuration = configuration

                val segmentsAdjusted = inputState.plannedProfile.toMutableList()
                val index = segmentsAdjusted.indices.maxByOrNull { segmentsAdjusted[it].depth }
                val deepestSegment = index?.let { inputState.plannedProfile[it] }

                val deeper = configuration.contingencyDeeper.takeIf { inputState.deeper }
                val longer = configuration.contingencyLonger.takeIf { inputState.longer }

                if (deepestSegment != null) {
                    segmentsAdjusted[index] = deepestSegment.copy(
                        depth = deepestSegment.depth + (deeper ?: 0),
                        duration = deepestSegment.duration + (longer ?: 0)
                    )
                }

                val cylinders = inputState.cylinders.filter { it.isChecked }.map { it.cylinder }

                val adjustedPlan = planner.addDive(
                    plan = segmentsAdjusted,
                    cylinders = cylinders,
                )

                val gasPlan = GasPlanner().calculateGasPlan(adjustedPlan)

                Result.success(
                    DivePlanSet(
                        base = adjustedPlan,
                        deeper = deeper,
                        longer = longer,
                        gasPlan = gasPlan,
                    )
                )
            }
            println("Dive plan calculation took: ${timedResult.duration.inWholeMilliseconds}ms")
            timedResult.value
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun setContingency(deeper: Boolean, longer: Boolean) {
        inputState.update { inputState ->
            inputState.copy(
                deeper = deeper,
                longer = longer,
            )
        }
    }

    data class UiState(
        val segments: List<DiveProfileSection> = defaultProfile,
        val availableGas: List<PlannedCylinderModel> = defaultCylinders,
        val divePlanSet: Result<DivePlanSet?> = Result.success(null),
        val isCalculatingDivePlan: Boolean = false,
        val isLoading: Boolean = true,
        val settingsModel: SettingsModel = SettingsModel(),
        val configuration: Configuration = Configuration()
    )
}

// TODO consider removing defaults from the production version of the app.
private val defaultCylinderAir = Cylinder.steel12Liter(gas = Gas.Air, pressure = 232.0)

// TODO consider removing defaults from the production version of the app.
private val defaultCylinders: List<PlannedCylinderModel> = listOf(
    PlannedCylinderModel(
        cylinder = defaultCylinderAir,
        isLocked = true,
        isChecked = true
    ),
    PlannedCylinderModel(
        cylinder = Cylinder.aluminium80Cuft(gas = Gas.Nitrox50, pressure = 207.0),
        isLocked = false,
        isChecked = true
    ),
    PlannedCylinderModel(
        cylinder = Cylinder.aluminium63Cuft(gas = Gas.Nitrox80, pressure = 207.0),
        isLocked = false,
        isChecked = false
    )
)

// TODO consider removing defaults from the production version of the app.
private val defaultProfile = listOf(
    DiveProfileSection(
        30,
        25,
        defaultCylinderAir
    )
)

private val defaultDivePlanInputModel = DivePlanInputModel(
    deeper = false,
    longer = false,
    plannedProfile = defaultProfile,
    cylinders = defaultCylinders,
)


private const val SUBSCRIPTION_TIME_OUT: Long = 5 * 60 * 1000

private fun List<DiveProfileSection>.gasesInUse(): Set<Gas> =
    mapTo(mutableSetOf()) { it.cylinder.gas }

private fun List<PlannedCylinderModel>.checkedCylinderCountFor(gas: Gas): Int =
    count { it.cylinder.gas == gas && it.isChecked }

