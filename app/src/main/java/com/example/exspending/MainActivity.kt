package com.example.exspending

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.exspending.ui.ExspendingNav
import com.example.exspending.ui.theme.ExspendingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ExspendingApp
        setContent {
            ExspendingTheme {
                ExspendingNav(repository = app.repository)
            }
        }
    }
}
