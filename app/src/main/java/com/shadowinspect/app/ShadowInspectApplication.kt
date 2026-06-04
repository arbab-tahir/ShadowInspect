package com.shadowinspect.app

import android.app.Application
import android.util.Log
import com.shadowinspect.app.domain.ml.MLModelManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ShadowInspectApplication : Application() {

    @Inject
    lateinit var mlModelManager: MLModelManager

    override fun onCreate() {
        super.onCreate()

        // Initialize ML models in background at app startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ShadowInspect", "Starting ML model initialization...")
                val initialized = mlModelManager.initializeModels()
                Log.d("ShadowInspect", "ML Models initialized: $initialized")
                mlModelManager.debugMLStatus()
            } catch (e: Exception) {
                Log.e("ShadowInspect", "ML initialization failed at startup", e)
            }
        }
    }
}
