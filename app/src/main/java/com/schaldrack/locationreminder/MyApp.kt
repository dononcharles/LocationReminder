package com.schaldrack.locationreminder

import android.app.Application
import com.schaldrack.locationreminder.di.myModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyApp)
            modules(listOf(myModule))
        }
    }
}
