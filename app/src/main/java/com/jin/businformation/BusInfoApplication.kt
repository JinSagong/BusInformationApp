package com.jin.businformation

import android.app.Application
import io.realm.Realm

class BusInfoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        // Realm.setDefaultConfiguration(RealmConfiguration.Builder().schemaVersion(1).build())
        // Realm.deleteRealm(Realm.getDefaultConfiguration())
    }
}