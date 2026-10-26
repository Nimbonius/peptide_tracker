package com.peptidetracker

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.peptidetracker.data.local.PeptideDatabaseProvider
import com.peptidetracker.ui.screens.PeptideTrackerScreen

class PeptideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PeptideDatabaseProvider.get(this)
    }
}

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    PeptideTrackerScreen()
                }
            }
        }
    }
}
