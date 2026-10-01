package com.nport.wg.warp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nport.wg.amnezia.AmneziaWarpConfig
import com.nport.wg.amnezia.AmneziaWarpGenerator
import com.nport.wg.amnezia.AmneziaWarpLocation
import com.nport.wg.amnezia.AmneziaObfuscationPresets
import com.nport.wg.amnezia.ObfuscationPreset
import com.nport.wg.amnezia.ObfuscationLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Amnezia WARP configuration generation
 */
class WarpGeneratorViewModel : ViewModel() {

    private val amneziaGenerator = AmneziaWarpGenerator()

    private val _uiState = MutableStateFlow<WarpUiState>(WarpUiState.Idle)
    val uiState: StateFlow<WarpUiState> = _uiState.asStateFlow()

    private val _locations = MutableStateFlow<List<AmneziaWarpLocation>>(emptyList())
    val locations: StateFlow<List<AmneziaWarpLocation>> = _locations.asStateFlow()

    private val _generatedConfig = MutableStateFlow<AmneziaWarpConfig?>(null)
    val generatedConfig: StateFlow<AmneziaWarpConfig?> = _generatedConfig.asStateFlow()

    private val _selectedObfuscationLevel = MutableStateFlow(ObfuscationLevel.MEDIUM)
    val selectedObfuscationLevel: StateFlow<ObfuscationLevel> = _selectedObfuscationLevel.asStateFlow()

    init {
        loadLocations()
    }

    /**
     * Load available Amnezia WARP locations
     */
    fun loadLocations() {
        viewModelScope.launch {
            val result = amneziaGenerator.getAvailableLocations()
            result.onSuccess { locations ->
                _locations.value = locations
            }.onFailure { error ->
                _uiState.value = WarpUiState.Error(error.message ?: "Failed to load locations")
            }
        }
    }

    /**
     * Set obfuscation level
     */
    fun setObfuscationLevel(level: ObfuscationLevel) {
        _selectedObfuscationLevel.value = level
    }

    /**
     * Generate Amnezia WARP configuration
     */
    fun generateConfig(location: AmneziaWarpLocation? = null) {
        viewModelScope.launch {
            _uiState.value = WarpUiState.Loading

            val preset = when (_selectedObfuscationLevel.value) {
                ObfuscationLevel.LOW -> AmneziaObfuscationPresets.LOW
                ObfuscationLevel.MEDIUM -> AmneziaObfuscationPresets.MEDIUM
                ObfuscationLevel.HIGH -> AmneziaObfuscationPresets.HIGH
                ObfuscationLevel.NONE -> AmneziaObfuscationPresets.LOW
            }

            val result = if (location != null) {
                amneziaGenerator.generateForLocation(location, preset)
            } else {
                amneziaGenerator.generate(preset)
            }

            result.onSuccess { config ->
                _generatedConfig.value = config
                val testResult = amneziaGenerator.testObfuscation(config)
                _uiState.value = WarpUiState.Success(config, testResult)
            }.onFailure { error ->
                _uiState.value = WarpUiState.Error(error.message ?: "Failed to generate config")
            }
        }
    }

    /**
     * Reset state
     */
    fun reset() {
        _uiState.value = WarpUiState.Idle
        _generatedConfig.value = null
    }
}

/**
 * UI state for Amnezia WARP generation
 */
sealed class WarpUiState {
    object Idle : WarpUiState()
    object Loading : WarpUiState()
    data class Success(
        val config: AmneziaWarpConfig,
        val obfuscationTest: com.nport.wg.amnezia.ObfuscationTestResult
    ) : WarpUiState()
    data class Error(val message: String) : WarpUiState()
}
