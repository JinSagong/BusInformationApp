package com.jin.businformation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.gms.ads.*
import com.google.android.gms.location.*
import io.realm.Case
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_theme.*
import org.jetbrains.anko.*
import kotlin.math.*

@Suppress("UNCHECKED_CAST")
class MainActivity : AppCompatActivity() {
    private val realm = Realm.getDefaultInstance()

    private val itemList = arrayListOf<DataMainInfo>()
    private val mainAdapter = MainAdapter(itemList, this)

    private val favoriteItemList = arrayListOf<DataMainInfo>()
    private val gpsItemList = arrayListOf<DataMainInfo>()
    private val searchItemList = arrayListOf<DataMainInfo>()

    private val adRequest = AdRequest.Builder().build()
    private lateinit var adView2: AdView

    private var myToast: Toast? = null

    private val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 1000
    }
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            p0?.let {
                Log.d(
                    "Location", "lat:${it.lastLocation.latitude}, lng:${it.lastLocation.longitude}"
                )
                getStationNearby(it.lastLocation.latitude, it.lastLocation.longitude)
            }
        }

        override fun onLocationAvailability(p0: LocationAvailability?) {
            if (!(getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)
            ) {
                myToast?.cancel()
                myToast = longToast(R.string.noLocationFunction)
                gpsItemList.clear()
                changeItemList(gpsItemList)
                gpsFab.alpha = 1f
                locationClient.removeLocationUpdates(this)
                isUpdatingLocation = false
            }
        }
    }
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val aniOpen by lazy {
        AnimationUtils.loadAnimation(
            applicationContext, R.anim.fab_open
        )
    }
    private val aniClose by lazy {
        AnimationUtils.loadAnimation(
            applicationContext, R.anim.fab_close
        )
    }

    private var themeView: ImageView? = null

    private val imm by lazy { getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    private var isUpdatingLocation = false
    private var hasTouchedGps = false
    private var onFavoriteScreen = true
    private var isFabOpen = false
    private var isFabMoved = false
    private var countNotFound = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        // Attach adView of AdMob
        MobileAds.initialize(this)
        adView2 = AdView(this).apply {
            adSize = AdSize.MEDIUM_RECTANGLE
            adUnitId = getString(R.string.BannerAd2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        adView2.loadAd(adRequest)

        // Tutorial screen
        if (defaultSharedPreferences.getBoolean("isFirst", true)) {
            tutorialLayout.visibility = View.VISIBLE
            tutorialCloseImageView.setOnClickListener {
                tutorialLayout.visibility = View.GONE
                defaultSharedPreferences.edit().putBoolean("isFirst", false).apply()
                adView1.loadAd(adRequest)
            }
        } else adView1.loadAd(adRequest)

        // If DB is not existed, create DB
        if (defaultSharedPreferences.getBoolean("hasNoData", true)) CreateDB(this)

        // Set recyclerView adapter
        mainRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        mainRecyclerView.adapter = mainAdapter


        // Set search mechanism and events
        searchView.addTextChangedListener {
            mainRecyclerView.smoothScrollToPosition(0)
            val query = it.toString()
            when {
                query != "" -> {
                    hasTouchedGps = false
                    onFavoriteScreen = false
                    search(query)
                }
                hasTouchedGps -> getLocationCoordinate()
                else -> getFavoriteList()
            }
        }

        setSelectThemeFab()
        setCloseButton()
        setMovingAnimation()
    }

    private fun setTheme() {
        setTheme(
            when (defaultSharedPreferences.getString("theme", "RED")) {
                "RED" -> R.style.AppTheme_RED
                "ORANGE" -> R.style.AppTheme_ORANGE
                "YELLOW" -> R.style.AppTheme_YELLOW
                "GREEN" -> R.style.AppTheme_GREEN
                "BLUE" -> R.style.AppTheme_BLUE
                "PURPLE" -> R.style.AppTheme_PURPLE
                "PINK" -> R.style.AppTheme_PINK
                else -> R.style.AppTheme_GRAY
            }
        )
    }

    private fun setSelectThemeFab() {
        val myOnClickListener = View.OnClickListener {
            if (themeView == null || it != themeView) {
                themeView = it as ImageView
                setTheme(
                    when (it) {
                        themeFab1 -> {
                            defaultSharedPreferences.edit().putString("theme", "RED").apply()
                            R.style.AppTheme_RED
                        }
                        themeFab2 -> {
                            defaultSharedPreferences.edit().putString("theme", "ORANGE").apply()
                            R.style.AppTheme_ORANGE
                        }
                        themeFab3 -> {
                            defaultSharedPreferences.edit().putString("theme", "YELLOW").apply()
                            R.style.AppTheme_YELLOW
                        }
                        themeFab4 -> {
                            defaultSharedPreferences.edit().putString("theme", "GREEN").apply()
                            R.style.AppTheme_GREEN
                        }
                        themeFab5 -> {
                            defaultSharedPreferences.edit().putString("theme", "BLUE").apply()
                            R.style.AppTheme_BLUE
                        }
                        themeFab6 -> {
                            defaultSharedPreferences.edit().putString("theme", "PURPLE").apply()
                            R.style.AppTheme_PURPLE
                        }
                        themeFab7 -> {
                            defaultSharedPreferences.edit().putString("theme", "PINK").apply()
                            R.style.AppTheme_PINK
                        }
                        else -> {
                            defaultSharedPreferences.edit().putString("theme", "GRAY").apply()
                            R.style.AppTheme_GRAY
                        }
                    }
                )

                changeItemList(itemList.clone() as ArrayList<DataMainInfo>)
                locationImageView.setImageDrawable(
                    VectorDrawableCompat.create(
                        resources, R.drawable.ic_pin_drop_black_48dp, theme
                    )
                )
                closeImageView.setImageDrawable(
                    VectorDrawableCompat.create(
                        resources, R.drawable.ic_close_black_48dp, theme
                    )
                )
                favoriteFab.background = resources.getDrawable(R.drawable.fab_button, theme)
                gpsFab.background = resources.getDrawable(R.drawable.fab_button, theme)
                themeFab.background = resources.getDrawable(R.drawable.fab_button, theme)
            }
        }

        themeFab1.setOnClickListener(myOnClickListener)
        themeFab2.setOnClickListener(myOnClickListener)
        themeFab3.setOnClickListener(myOnClickListener)
        themeFab4.setOnClickListener(myOnClickListener)
        themeFab5.setOnClickListener(myOnClickListener)
        themeFab6.setOnClickListener(myOnClickListener)
        themeFab7.setOnClickListener(myOnClickListener)
        themeFab8.setOnClickListener(myOnClickListener)
    }

    private fun setCloseButton() {
        closeImageView.setOnClickListener {
            hasTouchedGps = false
            imm.hideSoftInputFromWindow(searchView.windowToken, 0)
            searchView.setText("")
        }
    }

    private fun setFavoriteFab() {
        hasTouchedGps = false
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
        searchView.setText("")
    }

    private fun setGpsFab() {
        hasTouchedGps = true
        onFavoriteScreen = false
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
        searchView.setText("")
    }

    private fun setThemeFabs() {
        if (isFabOpen) {
            themeFab1.startAnimation(aniClose)
            themeFab2.startAnimation(aniClose)
            themeFab3.startAnimation(aniClose)
            themeFab4.startAnimation(aniClose)
            themeFab5.startAnimation(aniClose)
            themeFab6.startAnimation(aniClose)
            themeFab7.startAnimation(aniClose)
            themeFab8.startAnimation(aniClose)
            themeFab1.isClickable = false
            themeFab2.isClickable = false
            themeFab3.isClickable = false
            themeFab4.isClickable = false
            themeFab5.isClickable = false
            themeFab6.isClickable = false
            themeFab7.isClickable = false
            themeFab8.isClickable = false
        } else {
            themeFab1.startAnimation(aniOpen)
            themeFab2.startAnimation(aniOpen)
            themeFab3.startAnimation(aniOpen)
            themeFab4.startAnimation(aniOpen)
            themeFab5.startAnimation(aniOpen)
            themeFab6.startAnimation(aniOpen)
            themeFab7.startAnimation(aniOpen)
            themeFab8.startAnimation(aniOpen)
            themeFab1.isClickable = true
            themeFab2.isClickable = true
            themeFab3.isClickable = true
            themeFab4.isClickable = true
            themeFab5.isClickable = true
            themeFab6.isClickable = true
            themeFab7.isClickable = true
            themeFab8.isClickable = true
        }
        isFabOpen = !isFabOpen
    }

    private fun createSpringAnimation(
        view: View, property: DynamicAnimation.ViewProperty
    ): SpringAnimation {
        val animation = SpringAnimation(view, property)
        val spring = SpringForce()
        spring.stiffness = SpringForce.STIFFNESS_LOW
        spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        animation.spring = spring

        return animation
    }

    private fun setMovingAnimation() {
        val xAnimationFavorite =
            createSpringAnimation(favoriteFab, SpringAnimation.X)
        val yAnimationFavorite =
            createSpringAnimation(favoriteFab, SpringAnimation.Y)
        val xAnimationGps =
            createSpringAnimation(gpsFab, SpringAnimation.X)
        val yAnimationGps =
            createSpringAnimation(gpsFab, SpringAnimation.Y)
        val xAnimationTheme =
            createSpringAnimation(themeFab, SpringAnimation.X)
        val yAnimationTheme =
            createSpringAnimation(themeFab, SpringAnimation.Y)
        val xAnimationThemeLayout =
            createSpringAnimation(themeLayout, SpringAnimation.X)
        val yAnimationThemeLayout =
            createSpringAnimation(themeLayout, SpringAnimation.Y)

        var initX = 0f
        var initY = 0f
        var dX = 0f
        var dY = 0f
        var numTouched = 0
        var hasMovedFavorite = false
        var hasMovedGps = false
        var hasMovedTheme = false
        val myOnTouchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    numTouched++
                    initX = event.rawX
                    initY = event.rawY
                    dX = v.x - initX
                    dY = v.y - initY
                    when (v) {
                        favoriteFab -> {
                            hasMovedFavorite = false
                            xAnimationGps.cancel()
                            yAnimationGps.cancel()
                            xAnimationTheme.cancel()
                            yAnimationTheme.cancel()
                            xAnimationThemeLayout.cancel()
                            yAnimationThemeLayout.cancel()
                        }
                        gpsFab -> {
                            hasMovedGps = false
                            xAnimationFavorite.cancel()
                            yAnimationFavorite.cancel()
                            xAnimationTheme.cancel()
                            yAnimationTheme.cancel()
                            xAnimationThemeLayout.cancel()
                            yAnimationThemeLayout.cancel()
                        }
                        else -> {
                            hasMovedTheme = false
                            xAnimationFavorite.cancel()
                            yAnimationFavorite.cancel()
                            xAnimationGps.cancel()
                            yAnimationGps.cancel()
                            xAnimationThemeLayout.cancel()
                            yAnimationThemeLayout.cancel()
                        }
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    v.animate().x(event.rawX + dX).y(event.rawY + dY)
                        .setDuration(0).start()
                    if ((abs(event.rawX - initX) > v.width / 4) || (abs(event.rawY - initY) > v.height / 4)) {
                        if (isFabOpen) {
                            isFabMoved = true
                            setThemeFabs()
                        }
                        when (v) {
                            favoriteFab -> hasMovedFavorite = true
                            gpsFab -> hasMovedGps = true
                            else -> hasMovedTheme = true
                        }
                    }
                    if (numTouched == 1) {
                        if (v.x > mainLayout.width / 2 - v.width / 2)
                            xAnimationThemeLayout.animateToFinalPosition(v.x - themeLayout.width)
                        else xAnimationThemeLayout.animateToFinalPosition(v.x + v.width)
                        when (v) {
                            favoriteFab -> {
                                xAnimationGps.animateToFinalPosition(v.x)
                                yAnimationGps.animateToFinalPosition(
                                    v.y + v.height + resources.getDimensionPixelSize(R.dimen.marginDouble)
                                )
                                xAnimationTheme.animateToFinalPosition(v.x)
                                yAnimationTheme.animateToFinalPosition(
                                    v.y + 2 * v.height + 2 * resources.getDimensionPixelSize(R.dimen.marginDouble)
                                )
                                yAnimationThemeLayout.animateToFinalPosition(
                                    v.y + 2 * v.height + 2 * resources.getDimensionPixelSize(R.dimen.marginDouble)
                                )
                            }
                            gpsFab -> {
                                xAnimationFavorite.animateToFinalPosition(v.x)
                                yAnimationFavorite.animateToFinalPosition(
                                    v.y - v.height - resources.getDimensionPixelSize(R.dimen.marginDouble)
                                )
                                xAnimationTheme.animateToFinalPosition(v.x)
                                yAnimationTheme.animateToFinalPosition(
                                    v.y + v.height + resources.getDimensionPixelSize(R.dimen.marginDouble)
                                )
                                yAnimationThemeLayout.animateToFinalPosition(
                                    v.y + v.height + resources.getDimensionPixelSize(R.dimen.marginDouble)
                                )
                            }
                            else -> {
                                xAnimationFavorite.animateToFinalPosition(v.x)
                                yAnimationFavorite.animateToFinalPosition(
                                    v.y - 2 * v.height - 2 * resources.getDimensionPixelSize(R.dimen.marginDouble)
                                )
                                xAnimationGps.animateToFinalPosition(v.x)
                                yAnimationGps.animateToFinalPosition(
                                    v.y - v.height - resources.getDimensionPixelSize(R.dimen.marginDouble)
                                )
                                yAnimationThemeLayout.animateToFinalPosition(v.y)
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    numTouched--
                    when {
                        v == favoriteFab && !hasMovedFavorite -> setFavoriteFab()
                        v == gpsFab && !hasMovedGps -> setGpsFab()
                        v == themeFab && !hasMovedTheme -> setThemeFabs()
                        isFabMoved -> {
                            setThemeFabs()
                            isFabMoved = false
                        }
                    }
                }
            }
            true
        }

        favoriteFab.setOnTouchListener(myOnTouchListener)
        gpsFab.setOnTouchListener(myOnTouchListener)
        themeFab.setOnTouchListener(myOnTouchListener)
    }

    private fun changeItemList(list: ArrayList<DataMainInfo>) {
        // Discard null data
        val validList = arrayListOf<DataMainInfo>()
        list.forEach {
            if ((it.isStation && realm.where<DataStopby>().equalTo(
                    "nodeid", it.id
                ).findFirst() != null)
                || (!it.isStation && realm.where<DataStopby>().equalTo(
                    "routeid", it.id
                ).findFirst() != null)
            )
                validList.add(it)
        }
        itemList.clear()
        itemList.addAll(validList)
        mainAdapter.notifyDataSetChanged()
    }

    private fun getLocationCoordinate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000
            )
        } else if (!isUpdatingLocation) {
            gpsFab.alpha = 0.5f
            locationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.myLooper()
            )
            isUpdatingLocation = true
        }
    }

    private fun search(query: String) {
        searchItemList.clear()

        if (isUpdatingLocation) {
            locationClient.removeLocationUpdates(locationCallback)
            isUpdatingLocation = false
            gpsFab.alpha = 1f
        }

        val dataList1s = realm.where<DataStation>()
            .equalTo("nodenm", query, Case.INSENSITIVE).sort("nodenm").findAll()
        addSearchList(dataList1s as RealmResults<Any>, true)
        val dataList1r = realm.where<DataRoute>()
            .equalTo("routeno", query, Case.INSENSITIVE).sort("routeno").findAll()
        addSearchList(dataList1r as RealmResults<Any>, false)
        val dataList1sn = realm.where<DataStation>()
            .equalTo("nodeno", query, Case.INSENSITIVE)
            .notEqualTo("nodenm", query, Case.INSENSITIVE).sort("nodeno").findAll()
        addSearchList(dataList1sn as RealmResults<Any>, true)
        val dataList2s = realm.where<DataStation>()
            .beginsWith("nodenm", query, Case.INSENSITIVE)
            .notEqualTo("nodenm", query, Case.INSENSITIVE)
            .notEqualTo("nodeno", query, Case.INSENSITIVE).sort("nodenm").findAll()
        addSearchList(dataList2s as RealmResults<Any>, true)
        val dataList2r = realm.where<DataRoute>()
            .beginsWith("routeno", query, Case.INSENSITIVE)
            .notEqualTo("routeno", query, Case.INSENSITIVE).sort("routeno").findAll()
        addSearchList(dataList2r as RealmResults<Any>, false)
        val dataList2sn = realm.where<DataStation>()
            .beginsWith("nodeno", query, Case.INSENSITIVE)
            .notEqualTo("nodenm", query, Case.INSENSITIVE)
            .notEqualTo("nodeno", query, Case.INSENSITIVE).not()
            .beginsWith("nodenm", query, Case.INSENSITIVE).sort("nodeno").findAll()
        addSearchList(dataList2sn as RealmResults<Any>, true)
        val dataList3s = realm.where<DataStation>()
            .contains("nodenm", query, Case.INSENSITIVE)
            .notEqualTo("nodenm", query, Case.INSENSITIVE)
            .notEqualTo("nodeno", query, Case.INSENSITIVE).not()
            .beginsWith("nodenm", query, Case.INSENSITIVE).not()
            .beginsWith("nodeno", query, Case.INSENSITIVE).sort("nodenm").findAll()
        addSearchList(dataList3s as RealmResults<Any>, true)
        val dataList3r = realm.where<DataRoute>()
            .contains("routeno", query, Case.INSENSITIVE)
            .notEqualTo("routeno", query, Case.INSENSITIVE).not()
            .beginsWith("routeno", query, Case.INSENSITIVE).sort("routeno").findAll()
        addSearchList(dataList3r as RealmResults<Any>, false)
        val dataList3sn = realm.where<DataStation>()
            .contains("nodeno", query, Case.INSENSITIVE)
            .notEqualTo("nodenm", query, Case.INSENSITIVE)
            .notEqualTo("nodeno", query, Case.INSENSITIVE).not()
            .beginsWith("nodenm", query, Case.INSENSITIVE).not()
            .beginsWith("nodeno", query, Case.INSENSITIVE).not()
            .contains("nodenm", query, Case.INSENSITIVE).sort("nodeno").findAll()
        addSearchList(dataList3sn as RealmResults<Any>, true)

        changeItemList(searchItemList)
    }

    private fun addSearchList(dataList: RealmResults<Any>, isStation: Boolean) {
        dataList.forEach {
            searchItemList.add(
                if (isStation) {
                    DataMainInfo(
                        true, (it as DataStation).nodeid, it.nodenm, it.nodeno, null,
                        it.gpslati, it.gpslong, null, null, null
                    )
                } else {
                    DataMainInfo(
                        false, (it as DataRoute).routeid, it.routeno, null,
                        null, null, null, it.routetp, it.startnodenm, it.endnodenm
                    )
                }
            )
        }
    }

    private fun getFavoriteList() {
        favoriteItemList.clear()

        onFavoriteScreen = true
        if (isUpdatingLocation) {
            locationClient.removeLocationUpdates(locationCallback)
            isUpdatingLocation = false
            gpsFab.alpha = 1f
        }

        val favoriteList = realm.where<DataFavorite>().sort("index", Sort.DESCENDING).findAll()
        favoriteList.forEach {
            favoriteItemList.add(
                if (it.isStation) {
                    val item = realm.where<DataStation>().equalTo("nodeid", it.id).findFirst()!!
                    DataMainInfo(
                        it.isStation, it.id, item.nodenm, item.nodeno, null,
                        item.gpslati, item.gpslong, null, null, null
                    )
                } else {
                    val item = realm.where<DataRoute>().equalTo("routeid", it.id).findFirst()!!
                    DataMainInfo(
                        it.isStation, it.id, item.routeno, null, null,
                        null, null, item.routetp, item.startnodenm, item.endnodenm
                    )
                }
            )
        }

        changeItemList(favoriteItemList)
    }

    private fun getStationNearby(lat: Double, lng: Double) {
        val distance = 2000.0
        val dR = distance / (1.609344 * 1000 * 60 * 1.1515)
        gpsItemList.clear()

        val nList = realm.where<DataStation>()
            .between("gpslati", (lat - dR).toFloat(), (lat + dR).toFloat())
            .between("gpslong", (lng - dR).toFloat(), (lng + dR).toFloat()).findAll()
        nList.forEach {
            gpsItemList.add(
                DataMainInfo(
                    true, it.nodeid, it.nodenm, it.nodeno,
                    getDistance(lat, lng, it.gpslati, it.gpslong), it.gpslati, it.gpslong,
                    null, null, null
                )
            )
        }

        if (gpsItemList.isNotEmpty()) gpsItemList.sortBy { it.distance }
        else {
            countNotFound++
            myToast?.cancel()
            if (countNotFound == 1) myToast = longToast(getString(R.string.loadingGpsInfo))
            else {
                myToast = longToast(getString(R.string.noGpsInfo))
                countNotFound = 0
                locationClient.removeLocationUpdates(locationCallback)
                isUpdatingLocation = false
            }

        }
        changeItemList(gpsItemList)
        if (countNotFound != 1) gpsFab.alpha = 1f
    }

    private fun getDistance(lat1: Double, lng1: Double, lat2: Float, lng2: Float): Double {
        return 1.609344 * 1000 * 60 * 1.1515 * 180 / PI * acos(
            sin(lat1 * PI / 180) * sin(lat2 * PI / 180)
                    + cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * cos((lng2 - lng1) * PI / 180)
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        myToast?.cancel()
        if (requestCode == 1000 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED)
            myToast = longToast(R.string.noPermission)
        else if (requestCode == 1000 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            getLocationCoordinate()

    }

    override fun onResume() {
        super.onResume()
        if (onFavoriteScreen) getFavoriteList()
        else mainAdapter.notifyDataSetChanged()
        mainAdapter.setOffDetail()
        if (isUpdatingLocation) locationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.myLooper()
        )
    }

    override fun onPause() {
        super.onPause()
        if (isUpdatingLocation) {
            locationClient.removeLocationUpdates(locationCallback)
            gpsFab.alpha = 1f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainAdapter.onStop()
        realm.close()
    }

    override fun onBackPressed() {
        alert {
            title = getString(R.string.closeApp)
            customView = adView2
            yesButton {
                it.dismiss()
                finish()
            }
            noButton {
                adView2 = AdView(this.ctx).apply {
                    adSize = AdSize.MEDIUM_RECTANGLE
                    adUnitId = getString(R.string.BannerAd2)
                    loadAd(adRequest)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }
            onKeyPressed { dialog, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss()
                    finish()
                }
                true
            }
            onCancelled {
                adView2 = AdView(this.ctx).apply {
                    adSize = AdSize.MEDIUM_RECTANGLE
                    adUnitId = getString(R.string.BannerAd2)
                    loadAd(adRequest)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }

            show()
        }
    }

    data class DataMainInfo(
        val isStation: Boolean,
        val id: String,
        val name: String,
        val stationNumber: String?,
        val distance: Double?,
        val lat: Float?,
        val lng: Float?,
        val routeType: String?,
        val startStation: String?,
        val endStation: String?
    )

}
