package com.studionails.beauty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studionails.beauty.ui.Paper
import com.studionails.beauty.ui.StudioRoot
import com.studionails.beauty.ui.StudioTheme
import com.studionails.beauty.ui.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
                    val model: StudioViewModel = viewModel()
                    StudioRoot(model)
                }
            }
        }
    }
}
