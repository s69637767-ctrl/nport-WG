package com.nport.wg.vless

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.wireguard.android.R
import com.wireguard.android.databinding.VlessImportDialogBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class VlessImportDialogFragment : DialogFragment() {

    private var _binding: VlessImportDialogBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: VlessImporterViewModel by viewModels()

    private val qrCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            binding.uriInput.setText(result.contents)
            viewModel.importFromQrCode(result.contents)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = VlessImportDialogBinding.inflate(LayoutInflater.from(requireContext()))
        
        setupTabLayout()
        setupButtons()
        observeState()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.uriImportLayout.visibility = View.VISIBLE
                        binding.subscriptionLayout.visibility = View.GONE
                    }
                    1 -> {
                        binding.uriImportLayout.visibility = View.GONE
                        binding.subscriptionLayout.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupButtons() {
        // Scan QR Code
        binding.scanQrButton.setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Scan VLESS QR Code")
            options.setCameraId(0)
            options.setBeepEnabled(false)
            options.setBarcodeImageEnabled(true)
            qrCodeLauncher.launch(options)
        }

        // Import from URI
        binding.importUriButton.setOnClickListener {
            val uri = binding.uriInput.text.toString()
            if (uri.isNotEmpty()) {
                viewModel.importFromUri(uri)
            } else {
                Toast.makeText(requireContext(), "Please enter VLESS URI", Toast.LENGTH_SHORT).show()
            }
        }

        // Import from Subscription
        binding.importSubscriptionButton.setOnClickListener {
            val url = binding.subscriptionUrlInput.text.toString()
            if (url.isNotEmpty()) {
                viewModel.importFromSubscription(url)
            } else {
                Toast.makeText(requireContext(), "Please enter subscription URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is VlessImportState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.statusText.visibility = View.GONE
                    }
                    is VlessImportState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.statusText.visibility = View.VISIBLE
                        binding.statusText.text = "Importing VLESS configurations..."
                    }
                    is VlessImportState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.statusText.visibility = View.GONE
                        
                        val configCount = state.configs.size
                        showSuccessDialog(configCount)
                    }
                    is VlessImportState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.statusText.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showSuccessDialog(configCount: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import Successful")
            .setMessage("Successfully imported $configCount VLESS configuration(s).\n\nWould you like to create tunnels now?")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
