package com.example.tipcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tipcalculator.databinding.FragmentTipCalculatorBinding

class TipCalculatorFragment : Fragment() {

    private var _binding: FragmentTipCalculatorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTipCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCalculate.setOnClickListener {
            calculateTip()
        }

        binding.btnGoToDishCalculator.setOnClickListener {
            findNavController().navigate(R.id.action_tipCalculatorFragment_to_dishCalculatorFragment)
        }

        val sharedPref = requireActivity().getSharedPreferences("app_prefs", 0)
        val savedAmount = sharedPref.getFloat("subtotal", 0f)
        if (savedAmount > 0) {
            binding.etTotalAmount.setText(savedAmount.toString())
        }
    }

    private fun calculateTip() {
        val totalAmountText = binding.etTotalAmount.text.toString()
        val tipPercentageText = binding.etTipPercentage.text.toString()

        if (totalAmountText.isEmpty() || tipPercentageText.isEmpty()) {
            return
        }

        val totalAmount = totalAmountText.toDouble()
        val tipPercentage = tipPercentageText.toDouble()

        val tipAmount = totalAmount * (tipPercentage / 100)
        val totalWithTip = totalAmount + tipAmount

        binding.tvTotalAmount.text = "%.2f".format(totalAmount)
        binding.tvTipAmount.text = "%.2f".format(tipAmount)
        binding.tvTotalWithTip.text = "%.2f".format(totalWithTip)

        binding.cardResult.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}