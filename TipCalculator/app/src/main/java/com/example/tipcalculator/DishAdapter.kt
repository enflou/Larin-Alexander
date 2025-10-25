package com.example.tipcalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tipcalculator.model.Dish

class DishAdapter(private var dishes: List<Dish>) : RecyclerView.Adapter<DishAdapter.DishViewHolder>() {

    class DishViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDishName: TextView = itemView.findViewById(R.id.tvDishName)
        val tvDishPrice: TextView = itemView.findViewById(R.id.tvDishPrice)
        val tvDishQuantity: TextView = itemView.findViewById(R.id.tvDishQuantity)
        val tvDishTotal: TextView = itemView.findViewById(R.id.tvDishTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DishViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dish, parent, false)
        return DishViewHolder(view)
    }

    override fun onBindViewHolder(holder: DishViewHolder, position: Int) {
        val dish = dishes[position]
        holder.tvDishName.text = dish.name
        holder.tvDishPrice.text = "%.2f".format(dish.price)
        holder.tvDishQuantity.text = dish.quantity.toString()
        holder.tvDishTotal.text = "%.2f".format(dish.price * dish.quantity)
    }

    override fun getItemCount(): Int = dishes.size

    fun updateDishes(newDishes: List<Dish>) {
        dishes = newDishes
        notifyDataSetChanged()
    }
}