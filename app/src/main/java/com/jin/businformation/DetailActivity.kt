package com.jin.businformation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where

import kotlinx.android.synthetic.main.activity_detail.*
import okhttp3.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.longToast
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

@Suppress("UNCHECKED_CAST")
class DetailActivity : AppCompatActivity() {
    private val realm = Realm.getDefaultInstance()

    private val citycode by lazy { getString(R.string.CITY_CODE).toInt() }

    private val apiKeyArray by lazy { arrayOf(getString(R.string.apiKey1)) }
    private var apiKeyIndex = 0

    private var isOpen = true
    private val aniStart by lazy {
        AnimationUtils.loadAnimation(
            applicationContext, R.anim.enter_up_slide
        )
    }
    private val aniEnd by lazy {
        AnimationUtils.loadAnimation(applicationContext, R.anim.exit_down_slide)
    }

    private val isStation by lazy { intent.getBooleanExtra("isStation", true) }
    private val id by lazy { intent.getStringExtra("id")!! }
    private val name by lazy { intent.getStringExtra("name")!! }
    private val stationNumber by lazy { intent.getStringExtra("stationNumber") }
    private val lat by lazy { intent.getFloatExtra("lat", 0f) }
    private val lng by lazy { intent.getFloatExtra("lng", 0f) }
    private val routeType by lazy { intent.getStringExtra("routeType") }
    private val startStation by lazy { intent.getStringExtra("startStation") }
    private val endStation by lazy { intent.getStringExtra("endStation") }

    private val itemList by lazy {
        if (isStation) arrayListOf<DataStationInfo>() else arrayListOf<DataRouteInfo>()
    }
    private val detailAdapter by lazy {
        if (isStation) StationAdapter(itemList as ArrayList<DataStationInfo>, this)
        else RouteAdapter(itemList as ArrayList<DataRouteInfo>, this)
    }

    private val currentBusPositionList = arrayListOf<Int>()
    private var currentBusIndex = 0

    private var myToast: Toast? = null

    private var onRefresh = false
    private var onMap = false
    private var hasError = false
    private var countButton = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        setContentView(R.layout.activity_detail)

        // Delete animation of activity frame and activate animation of child views
        overridePendingTransition(0, 0)

        // Set recyclerView adapter
        detailRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        detailRecyclerView.adapter = detailAdapter

        detailNameTextView.text = name
        if (isStation) {
            detailTypeTextView.visibility = View.GONE
            detailDescriptionTextView.text = stationNumber
            detailInfoTextView.text = getString(R.string.loadingStationInfo)
        } else {
            detailTypeTextView.text = routeType!!.substring(0, routeType!!.length - 2)
            detailFakeTypeTextView.text = routeType!!.substring(0, routeType!!.length - 2)
            detailTypeTextView.backgroundTintList = when (routeType) {
                "좌석버스" -> ActivityCompat.getColorStateList(this, R.color.type2)
                else -> ActivityCompat.getColorStateList(this, R.color.type1)
            }
            val descriptionText = "$startStation  ->  $endStation"
            detailDescriptionTextView.text = descriptionText
            detailInfoTextView.text = getString(R.string.loadingRouteInfo)
        }

        setButtons()
        setAnimations()

        detailLayout.startAnimation(aniStart)

        if (isStation) getCurrentInfoForStation() else getCurrentInfoForRoute()
    }

    private fun setTheme() {
        setTheme(
            when (defaultSharedPreferences.getString("theme", "RED")) {
                "RED" -> R.style.AppTheme_Transparent_RED
                "ORANGE" -> R.style.AppTheme_Transparent_ORANGE
                "YELLOW" -> R.style.AppTheme_Transparent_YELLOW
                "GREEN" -> R.style.AppTheme_Transparent_GREEN
                "BLUE" -> R.style.AppTheme_Transparent_BLUE
                "PURPLE" -> R.style.AppTheme_Transparent_PURPLE
                "PINK" -> R.style.AppTheme_Transparent_PINK
                else -> R.style.AppTheme_Transparent_GRAY
            }
        )
    }

    private fun setButtons() {
        // Set close button
        detailCloseImageView.setOnClickListener {
            closeActivity()
        }

        // Set favorite button
        val filter =
            realm.where<DataFavorite>().equalTo("isStation", isStation).equalTo("id", id)
        detailFavoriteImageView.setImageResource(
            if (filter.findFirst() == null) R.drawable.ic_favorite_border_black_48dp
            else R.drawable.ic_favorite_black_48dp
        )
        detailFavoriteImageView.setOnClickListener {
            if (filter.findFirst() == null) {
                favoriteOn(isStation, id)
                (it as ImageView).setImageResource(R.drawable.ic_favorite_black_48dp)
            } else {
                favoriteOff(isStation, id)
                (it as ImageView).setImageResource(R.drawable.ic_favorite_border_black_48dp)
            }
        }

        // Set refresh button
        detailRefreshFab.setOnClickListener {
            if (!onRefresh) {
                onRefresh = true
                it.alpha = 0.5f
                if (isStation) getCurrentInfoForStation() else getCurrentInfoForRoute()
            }
        }

        if (isStation) {
            // Set map button
            detailSecondFab.setImageResource(R.drawable.googlemaps)
            detailSecondFab.setOnClickListener {
                if (!onMap) {
                    onMap = true
                    var gmmIntentUri = Uri.parse("geo:0,0?q=$lat,$lng($name)")
                    var mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        .setPackage("com.google.android.apps.maps")
                    if (mapIntent.resolveActivity(packageManager) != null) {
                        // To google maps
                        startActivity(mapIntent)
                    } else {
                        // To google play
                        gmmIntentUri =
                            Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps")
                        mapIntent =
                            Intent(
                                Intent.ACTION_VIEW,
                                gmmIntentUri
                            ).setPackage("com.android.vending")
                        startActivity(mapIntent)
                    }
                }
            }
        } else {
            // Set bus button
            detailSecondFab.setImageResource(R.drawable.ic_bus_48dp)
            detailSecondFab.setOnClickListener {
                if (currentBusPositionList.size == 0) {
                    myToast?.cancel()
                    myToast = longToast(R.string.noRouteInfo)
                } else {
                    detailRecyclerView.smoothScrollToPosition(currentBusPositionList[currentBusIndex])
                    currentBusIndex =
                        if (currentBusIndex == currentBusPositionList.size - 1) 0 else currentBusIndex + 1
                }
            }
        }
    }

    private fun favoriteOn(isStation: Boolean, id: String) {
        realm.beginTransaction()
        val newItem = realm.createObject<DataFavorite>()
        val maxIndex = realm.where<DataFavorite>().max("index")?.toInt() ?: 0
        newItem.index = maxIndex + 1
        newItem.isStation = isStation
        newItem.id = id
        realm.commitTransaction()
    }

    private fun favoriteOff(isStation: Boolean, id: String) {
        realm.beginTransaction()
        val deletedItem = realm.where<DataFavorite>()
            .equalTo("isStation", isStation).equalTo("id", id).findFirst()
        deletedItem!!.deleteFromRealm()
        realm.commitTransaction()
    }

    private fun getCurrentInfoForStation() {
        val tempItemList = arrayListOf<DataStationArr>()
        val apiBaseURL = "http://openapi.tago.go.kr/openapi/service/"
        val apiResource = "ArvlInfoInqireService/getSttnAcctoArvlPrearngeInfoList"
        val apiKey = "?ServiceKey=${apiKeyArray[apiKeyIndex]}"
        val apiHeader = "&cityCode=$citycode&nodeId=$id"
        val apiPage = "&numOfRows=1000&pageNo=1"
        val request =
            Request.Builder().url(apiBaseURL + apiResource + apiKey + apiHeader + apiPage).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("OkHttpClientOnFailure", e.message.orEmpty())
                hasError = true
                runOnUiThread {
                    myToast?.cancel()
                    myToast = longToast(R.string.networkError)
                }
                getListForStation(tempItemList)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string()
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    InputSource(StringReader(body.orEmpty()))
                )

                if (doc.getElementsByTagName("resultCode").item(0).textContent != "00") {
                    apiKeyIndex++
                    if (apiKeyIndex == apiKeyArray.size) {
                        hasError = true
                        runOnUiThread {
                            myToast?.cancel()
                            myToast = longToast(R.string.serverError)
                        }
                    } else return getCurrentInfoForStation()
                }

                Log.d("apiKeyIndex", "index: $apiKeyIndex, num of api key: ${apiKeyArray.size}")
                val nList = doc.getElementsByTagName("item")
                for (i in 0 until nList.length) {
                    val node = nList.item(i).childNodes
                    var itemStationCount = 0
                    var itemTime = 0
                    var itemId = ""
                    for (j in 0 until node.length) {
                        when (node.item(j).nodeName) {
                            "arrprevstationcnt" -> itemStationCount =
                                (node.item(j).textContent).toInt()
                            "arrtime" -> itemTime = (node.item(j).textContent).toInt()
                            "routeid" -> itemId = node.item(j).textContent
                        }
                    }
                    tempItemList.add(DataStationArr(itemId, itemTime, null, itemStationCount))
                }
                tempItemList.sortBy { it.time }

                getListForStation(tempItemList)
            }
        })
    }

    private fun getListForStation(arrList: ArrayList<DataStationArr>) {
        val tempRealm = Realm.getDefaultInstance()
        val arrIdList = arrayListOf<String>()
        val arrValueList = arrayListOf<ArrayList<DataStationArr>>()
        arrList.forEach {
            val item1 = tempRealm.where<DataStopby>().equalTo("routeid", it.id)
                .equalTo("nodeid", id).sort("nodeord").findAll().last()?.nodeord ?: -9999
            val item2 = tempRealm.where<DataStopby>().equalTo("routeid", it.id)
                .equalTo("nodeord", item1 - it.stationCount).findFirst()?.nodeid ?: "null"
            val item3 =
                tempRealm.where<DataStation>().equalTo("nodeid", item2).findFirst()?.nodenm
                    ?: "null"

            if (it.id in arrIdList) {
                arrValueList[arrIdList.indexOf(it.id)].add(
                    DataStationArr(it.id, it.time, item3, it.stationCount)
                )
            } else {
                arrIdList.add(it.id)
                arrValueList.add(
                    arrayListOf(DataStationArr(it.id, it.time, item3, it.stationCount))
                )
            }
        }

        val stationItemList1 = ArrayList<DataStationInfo>()
        var idx = 0
        arrIdList.forEach {
            val item = tempRealm.where<DataRoute>().equalTo("routeid", it).findFirst()
            if (item != null) stationItemList1.add(
                DataStationInfo(
                    it, item.routeno, item.routetp,
                    item.startnodenm, item.endnodenm, arrValueList[idx]
                )
            )
            idx++
        }

        val stationItemList2 = ArrayList<DataStationInfo>()
        val stopbyList = tempRealm.where<DataStopby>().equalTo("nodeid", id)
            .not().`in`("routeid", arrIdList.toTypedArray()).findAll()
        stopbyList.forEach {
            val item = tempRealm.where<DataRoute>().equalTo("routeid", it.routeid).findFirst()
            if (item != null) stationItemList2.add(
                DataStationInfo(
                    it.routeid, item.routeno, item.routetp,
                    item.startnodenm, item.endnodenm, null
                )
            )
        }
        tempRealm.close()
        stationItemList2.sortBy { it.routeName }

        itemList.clear()
        (itemList as ArrayList<DataStationInfo>).addAll(stationItemList1 + stationItemList2)
        runOnUiThread {
            detailAdapter.notifyDataSetChanged()
            if (hasError) detailInfoTextView.text = getString(R.string.noResultBecauseOfError)
            else detailInfoTextView.visibility = View.GONE
            detailRefreshFab.alpha = 1f
            onRefresh = false
        }
    }

    private fun getCurrentInfoForRoute() {
        val tempItemList = arrayListOf<DataRouteLocation>()
        val apiBaseURL = "http://openapi.tago.go.kr/openapi/service/"
        val apiResource = "BusLcInfoInqireService/getRouteAcctoBusLcList"
        val apiKey = "?ServiceKey=${apiKeyArray[apiKeyIndex]}"
        val apiHeader = "&cityCode=$citycode&routeId=$id"
        val apiPage = "&numOfRows=1000&pageNo=1"
        val request =
            Request.Builder().url(apiBaseURL + apiResource + apiKey + apiHeader + apiPage).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("OkHttpClientOnFailure", e.message.orEmpty())
                hasError = true
                runOnUiThread {
                    myToast?.cancel()
                    myToast = longToast(R.string.networkError)
                }
                getListForRoute(tempItemList)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string()
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    InputSource(StringReader(body.orEmpty()))
                )

                if (doc.getElementsByTagName("resultCode").item(0).textContent != "00") {
                    apiKeyIndex++
                    if (apiKeyIndex == apiKeyArray.size) {
                        hasError = true
                        runOnUiThread {
                            myToast?.cancel()
                            myToast = longToast(R.string.serverError)
                        }
                    } else return getCurrentInfoForRoute()
                }

                Log.d("apiKeyIndex", "index: $apiKeyIndex, num of api key: ${apiKeyArray.size}")
                val nList = doc.getElementsByTagName("item")
                for (i in 0 until nList.length) {
                    val node = nList.item(i).childNodes
                    var itemId = ""
                    var itemVehicleNo = ""
                    for (j in 0 until node.length) {
                        when (node.item(j).nodeName) {
                            "nodeid" -> itemId = node.item(j).textContent
                            "vehicleno" -> itemVehicleNo = node.item(j).textContent
                        }
                    }
                    tempItemList.add(DataRouteLocation(itemId, itemVehicleNo))
                }

                getListForRoute(tempItemList)
            }
        })
    }

    private fun getListForRoute(locationList: ArrayList<DataRouteLocation>) {
        val locationIdList = arrayListOf<String>()
        val locationValueList = arrayListOf<ArrayList<String>>()
        locationList.forEach {
            if (it.id in locationIdList) {
                locationValueList[locationIdList.indexOf(it.id)].add(it.vehicleNo)
            } else {
                locationIdList.add(it.id)
                locationValueList.add(arrayListOf(it.vehicleNo))
            }
        }

        val tempRealm = Realm.getDefaultInstance()
        val routeItemList = arrayListOf<DataRouteInfo>()
        val stopbyList =
            tempRealm.where<DataStopby>().equalTo("routeid", id).sort("nodeord").findAll()
        var idx = 0
        stopbyList.forEach {
            val item = tempRealm.where<DataStation>().equalTo("nodeid", it.nodeid).findFirst()
            if (item != null) routeItemList.add(
                DataRouteInfo(
                    it.nodeid, item.nodenm, item.nodeno, item.gpslati, item.gpslong,
                    if (it.nodeid in locationIdList) locationValueList[locationIdList.indexOf(it.nodeid)] else null
                )
            )
            if (it.nodeid in locationIdList) currentBusPositionList.add(idx)
            idx++
        }
        tempRealm.close()

        itemList.clear()
        (itemList as ArrayList<DataRouteInfo>).addAll(routeItemList)
        runOnUiThread {
            detailAdapter.notifyDataSetChanged()
            detailInfoTextView.text =
                when {
                    locationIdList.isNotEmpty() -> "현재 ${locationIdList.size}대의 버스가 운행중입니다."
                    hasError -> getString(R.string.noResultBecauseOfError)
                    else -> getString(R.string.noRouteInfo)
                }
            detailRefreshFab.alpha = 1f
            onRefresh = false
        }
    }

    private fun closeActivity() {
        isOpen = false
        if (isStation) (detailAdapter as StationAdapter).onChange = true
        else (detailAdapter as RouteAdapter).onChange = true
        detailCloseImageView.isClickable = false
        detailFavoriteImageView.isClickable = false
        detailRefreshFab.isClickable = false
        detailSecondFab.isClickable = false
        detailLayout.startAnimation(aniEnd)
    }

    private fun setAnimations() {
        val aniUpButton = AnimationUtils.loadAnimation(
            applicationContext, R.anim.button_up_slide
        )

        aniStart.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                detailRefreshFab.visibility = View.VISIBLE
                detailSecondFab.visibility = View.VISIBLE
                detailRefreshFab.startAnimation(aniUpButton)
                detailSecondFab.startAnimation(aniUpButton)
            }

            override fun onAnimationStart(animation: Animation?) {}
        })

        aniUpButton.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                countButton++
                if (countButton == 2) {
                    detailCloseImageView.isClickable = true
                    detailFavoriteImageView.isClickable = true
                    detailRefreshFab.isClickable = true
                    detailSecondFab.isClickable = true
                }
            }

            override fun onAnimationStart(animation: Animation?) {}
        })

        val aniDownButton = AnimationUtils.loadAnimation(
            applicationContext, R.anim.button_down_slide
        )

        aniEnd.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                detailRefreshFab.startAnimation(aniDownButton)
                detailSecondFab.startAnimation(aniDownButton)
            }

            override fun onAnimationStart(animation: Animation?) {}
        })

        aniDownButton.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                countButton--
                if (countButton == 0) {
                    detailRefreshFab.visibility = View.INVISIBLE
                    detailSecondFab.visibility = View.INVISIBLE
                    finish()
                }
            }

            override fun onAnimationStart(animation: Animation?) {}
        })
    }

    override fun onBackPressed() {
        if (isOpen) closeActivity()
    }

    override fun onResume() {
        super.onResume()
        onMap = false
    }

    override fun onPause() {
        super.onPause()
        myToast?.cancel()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isStation) (detailAdapter as StationAdapter).onStop()
        else (detailAdapter as RouteAdapter).onStop()
        realm.close()
    }

    data class DataStationInfo(
        val routeId: String,
        val routeName: String,
        val routeType: String,
        val startStation: String,
        val endStation: String,
        val arrList: ArrayList<DataStationArr>?
    )

    data class DataRouteInfo(
        val nodeId: String,
        val nodeName: String,
        val nodeNo: String,
        val lat: Float,
        val lng: Float,
        val locationList: ArrayList<String>?
    )

    data class DataStationArr(
        val id: String,
        val time: Int,
        val stationName: String?,
        val stationCount: Int
    )

    data class DataRouteLocation(val id: String, val vehicleNo: String)

}