package com.example.exspending.ui.util

import java.text.NumberFormat
import java.util.Locale

object MoneyFormat {
    private val euro: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-BE"))

    fun format(amount: Double): String = euro.format(amount)
}
