package com.nport.wg.warp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nport.wg.models.WarpConfig
import com.nport.wg.models.WarpLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for WARP configuration generation
 */
class WarpGeneratorViewModel : ViewModel() {

    private val warpGenerator = WarpGenerator()

    private val _uiState = MutableStateFlow<WarpUiState>(WarpUiState.Idle)
    val uiState: StateFlow<WarpUiState> = _uiState.asStateFlow()

    private val _locations = MutableStateFlow<List<WarpLocation>>(emptyList())
    val locations: StateFlow<List<WarpLocation>> = _locations.asStateFlow()

    private val _generatedConfig = MutableStateFlow<WarpConfig?>(null)
    val generatedConfig: StateFlow<WarpConfig?> = _generatedConfig.asStateFlow()

    init {
        loadLocations()
    }

    /**
     * Load available WARP locations
     */
    fun loadLocations() {
        viewModelScope.launch {
            val result = warpGenerator.getAvailableLocations()
            result.onSuccess { locations ->
                _locations.value = locations
            }.onFailure { error ->
                _uiState.value = WarpUiState.Error(error.message ?: "Failed to load locations")
            }
        }
    }

    /**
     * Generate WARP configuration
     */
    fun generateConfig(location: WarpLocation? = null) {
        viewModelScope.launch {
            _uiState.value = WarpUiState.Loading

            val result = if (location != null) {
                warpGenerator.generateForLocation(location)
            } else {
                warpGenerator.generate()
            }

            result.onSuccess { config ->
                _generatedConfig.value = config
                _uiState.value = WarpUiState.Success(config)
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
 * UI state for WARP generation
 */
sealed class WarpUiState {
    object Idle : WarpUiState()
    object Loading : WarpUiState()
    data class Success(val config: WarpConfig) : WarpUiState()
    data class Error(val message: String) : WarpUiState()
}
