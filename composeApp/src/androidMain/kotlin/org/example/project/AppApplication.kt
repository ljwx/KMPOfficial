package org.example.project

import android.app.Application

class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }

}