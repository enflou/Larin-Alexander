package com.example.tipcalculator

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tipcalculator.databinding.FragmentDishCalculatorBinding
import com.example.tipcalculator.model.Dish

class DishCalculatorFragment : Fragment() {

    private var _binding: FragmentDishCalculatorBinding? = null
    private val binding get() = _binding!!

    private val dishes = mutableListOf<Dish>()
    private lateinit var dishAdapter: DishAdapter
    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDishCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPref = requireActivity().getSharedPreferences("app_prefs", 0)

        setupRecyclerView()
        setupClickListeners()
        updateSubtotal()
    }

    private fun setupRecyclerView() {
        dishAdapter = DishAdapter(dishes)
        binding.rvDishes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dishAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAddDish.setOnClickListener {
            addDish()
        }

        binding.btnIncrease.setOnClickListener {
            increaseQuantity()
        }

        binding.btnDecrease.setOnClickListener {
            decreaseQuantity()
        }

        binding.btnTransferToMain.setOnClickListener {
            transferToMain()
        }

        binding.btnGoToTipCalculator.setOnClickListener {
            findNavController().navigate(R.id.action_dishCalculatorFragment_to_tipCalculatorFragment)
        }
    }

    private fun addDish() {
        val name = binding.etDishName.text.toString()
        val priceText = binding.etDishPrice.text.toString()
        val quantity = binding.tvQuantity.text.toString().toInt()

        if (name.isEmpty() || priceText.isEmpty()) {
            return
        }

        val price = priceText.toDouble()
        val dish = Dish(name, price, quantity)

        dishes.add(dish)
        dishAdapter.updateDishes(dishes)

        binding.etDishName.text?.clear()
        binding.etDishPrice.text?.clear()
        binding.tvQuantity.text = "1"

        updateSubtotal()
        binding.cardDishes.visibility = View.VISIBLE
        binding.btnTransferToMain.isEnabled = true
    }

    private fun increaseQuantity() {
        val currentQuantity = binding.tvQuantity.text.toString().toInt()
        binding.tvQuantity.text = (currentQuantity + 1).toString()
    }

    private fun decreaseQuantity() {
        val currentQuantity = binding.tvQuantity.text.toString().toInt()
        if (currentQuantity > 1) {
            binding.tvQuantity.text = (currentQuantity - 1).toString()
        }
    }

    private fun updateSubtotal() {
        val subtotal = dishes.sumOf { it.price * it.quantity }
        binding.tvSubtotal.text = "%.2f".format(subtotal)
    }

    private fun transferToMain() {
        val subtotal = dishes.sumOf { it.price * it.quantity }

        sharedPref.edit().putFloat("subtotal", subtotal.toFloat()).apply()

        findNavController().navigate(R.id.action_dishCalculatorFragment_to_tipCalculatorFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}