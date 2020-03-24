package com.jin.businformation

import android.app.Activity
import io.realm.Realm
import io.realm.RealmObject
import org.jetbrains.anko.defaultSharedPreferences

class CreateDB(act: Activity) {
    private val realm = Realm.getDefaultInstance()

    private val activity = act
    private val citycode = activity.getString(R.string.CITY_CODE).toInt()

    init {
        getStationList()
        getRouteList()
        getStopbyList()
        realm.close()

        activity.defaultSharedPreferences.edit().putBoolean("hasNoData", false).apply()
    }

    private fun getStationList() {
        realm.beginTransaction()
        realm.createAllFromJson(
            DataStation::class.java, activity.assets.open("station${citycode}.json")
        )
        realm.commitTransaction()
    }

    private fun getRouteList() {
        realm.beginTransaction()
        realm.createAllFromJson(
            DataRoute::class.java, activity.assets.open("route${citycode}.json")
        )
        realm.commitTransaction()
    }

    private fun getStopbyList() {
        realm.beginTransaction()
        realm.createAllFromJson(
            DataStopby::class.java, activity.assets.open("stopby${citycode}.json")
        )
        realm.commitTransaction()
    }
}

open class DataStation(
    var nodeid: String = "",
    var nodenm: String = "",
    var nodeno: String = "",
    var gpslati: Float = 0f,
    var gpslong: Float = 0f
) : RealmObject()

open class DataRoute(
    var routeid: String = "",
    var routeno: String = "",
    var routetp: String = "일반버스",
    var startnodenm: String = "",
    var endnodenm: String = ""
) : RealmObject()

open class DataStopby(
    var routeid: String = "",
    var nodeord: Int = 0,
    var nodeid: String = ""
) : RealmObject()

open class DataFavorite(
    var index: Int = 0,
    var isStation: Boolean = true,
    var id: String = ""
) : RealmObject()