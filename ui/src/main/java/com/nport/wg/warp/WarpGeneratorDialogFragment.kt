package com.nport.wg.warp

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wireguard.android.R
import com.wireguard.android.databinding.WarpGeneratorDialogBinding
import com.nport.wg.models.WarpLocation
import kotlinx.coroutines.launch

class WarpGeneratorDialogFragment : DialogFragment() {

    private var _binding: WarpGeneratorDialogBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WarpGeneratorViewModel by viewModels()
    private var selectedLocation: WarpLocation? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = WarpGeneratorDialogBinding.inflate(LayoutInflater.from(requireContext()))
        
        setupLocationSpinner()
        setupGenerateButton()
        observeState()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupLocationSpinner() {
        lifecycleScope.launch {
            viewModel.locations.collect { locations ->
                if (locations.isNotEmpty()) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        listOf("Auto (Random)") + locations.map { "${it.country} - ${it.city}" }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.locationSpinner.adapter = adapter
                }
            }
        }
    }

    private fun setupGenerateButton() {
        binding.generateButton.setOnClickListener {
            val position = binding.locationSpinner.selectedItemPosition
            selectedLocation = if (position == 0) null else {
                viewModel.locations.value.getOrNull(position - 1)
            }
            viewModel.generateConfig(selectedLocation)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is WarpUiState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.generateButton.isEnabled = true
                    }
                    is WarpUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.generateButton.isEnabled = false
                    }
                    is WarpUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.generateButton.isEnabled = true
                        showSuccessDialog(state.config.toWireGuardConfig())
                        dismiss()
                    }
                    is WarpUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.generateButton.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showSuccessDialog(config: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("WARP Config Generated")
            .setMessage("Configuration copied to clipboard. Create a new tunnel and paste the config.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
