package com.pinkauto.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val app = application as PinkAutoAppApplication
                val vm: PinkAutoViewModel = viewModel(
                    factory = PinkAutoViewModelFactory(app.container.repository)
                )
                PinkAutoApp(vm)
            }
        }
    }
}
