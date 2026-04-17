package com.pinkauto.app

import android.app.Application
import com.pinkauto.app.di.AppContainer

class PinkAutoAppApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
