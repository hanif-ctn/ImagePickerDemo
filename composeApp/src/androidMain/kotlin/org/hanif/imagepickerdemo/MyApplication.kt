package org.hanif.imagepickerdemo

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContext.init(this)
    }
}