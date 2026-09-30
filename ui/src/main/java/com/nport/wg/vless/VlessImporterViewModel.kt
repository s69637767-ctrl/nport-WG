package com.nport.wg.vless

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nport.wg.vless.VlessConfig
import com.nport.wg.vless.VlessImporter
import com.nport.wg.vless.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for VLESS import and subscription management
 */
class VlessImporterViewModel : ViewModel() {

    private val importer = VlessImporter()

    private val _uiState = MutableStateFlow<VlessImportState>(VlessImportState.Idle)
    val uiState: StateFlow<VlessImportState> = _uiState.asStateFlow()

    private val _configs = MutableStateFlow<List<VlessConfig>>(emptyList())
    val configs: StateFlow<List<VlessConfig>> = _configs.asStateFlow()

    private val _subscriptionUrl = MutableStateFlow("")
    val subscriptionUrl: StateFlow<String> = _subscriptionUrl.asStateFlow()

    /**
     * Import VLESS from URI (vless://...)
     */
    fun importFromUri(uri: String) {
        viewModelScope.launch {
            _uiState.value = VlessImportState.Loading

            val result = importer.fromUri(uri)
            result.onSuccess { config ->
                val validationResult = importer.validate(config)
                if (validationResult.isValid) {
                    _configs.value = listOf(config)
                    _uiState.value = VlessImportState.Success(listOf(config))
                } else {
                    _uiState.value = VlessImportState.Error(
                        "Invalid configuration: ${validationResult.errors.joinToString(", ")}"
                    )
                }
            }.onFailure { error ->
                _uiState.value = VlessImportState.Error(error.message ?: "Failed to import VLESS")
            }
        }
    }

    /**
     * Import VLESS from QR code
     */
    fun importFromQrCode(qrContent: String) {
        importFromUri(qrContent)
    }

    /**
     * Import from subscription URL
     */
    fun importFromSubscription(url: String) {
        viewModelScope.launch {
            _uiState.value = VlessImportState.Loading
            _subscriptionUrl.value = url

            val result = importer.fromSubscription(url)
            result.onSuccess { configList ->
                if (configList.isNotEmpty()) {
                    _configs.value = configList
                    _uiState.value = VlessImportState.Success(configList)
                } else {
                    _uiState.value = VlessImportState.Error("No valid VLESS configurations found")
                }
            }.onFailure { error ->
                _uiState.value = VlessImportState.Error(error.message ?: "Failed to fetch subscription")
            }
        }
    }

    /**
     * Update subscription (refresh configs)
     */
    fun updateSubscription() {
        val url = _subscriptionUrl.value
        if (url.isNotEmpty()) {
            importFromSubscription(url)
        }
    }

    /**
     * Validate single config
     */
    fun validateConfig(config: VlessConfig): ValidationResult {
        return importer.validate(config)
    }

    /**
     * Clear all configs
     */
    fun clearConfigs() {
        _configs.value = emptyList()
        _uiState.value = VlessImportState.Idle
    }

    /**
     * Remove single config
     */
    fun removeConfig(config: VlessConfig) {
        _configs.value = _configs.value.filter { it != config }
    }
}

/**
 * UI state for VLESS import
 */
sealed class VlessImportState {
    object Idle : VlessImportState()
    object Loading : VlessImportState()
    data class Success(val configs: List<VlessConfig>) : VlessImportState()
    data class Error(val message: String) : VlessImportState()
}
