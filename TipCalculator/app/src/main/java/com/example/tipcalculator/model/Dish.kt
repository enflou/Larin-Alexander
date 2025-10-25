package com.example.tipcalculator.model

data class Dish(
    val name: String,
    val price: Double,
    var quantity: Int = 1
)