package com.ss.video.rtc.demo.quickstart

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication

open class DemoApplication: MultiDexApplication() {

    @Override
    override fun  attachBaseContext(context : Context){
        super.attachBaseContext(context)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}