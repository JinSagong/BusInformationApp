package com.jin.businformation

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.item_main.view.*
import org.jetbrains.anko.startActivity
import kotlin.math.round

class MainAdapter(items: ArrayList<MainActivity.DataMainInfo>, act: Activity) :
    RecyclerView.Adapter<MainAdapter.MyViewHolder>() {
    private val realm = Realm.getDefaultInstance()

    private val list = items
    private val activity = act

    private var onDetail = false

    class MyViewHolder(v: View) : RecyclerView.ViewHolder(v)

    fun onStop() {
        realm.close()
    }

    fun setOffDetail() {
        onDetail = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_main, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = list[position]

        holder.itemView.background =
            activity.resources.getDrawable(R.drawable.item_container, activity.theme)
        holder.itemView.mainIconImageView.setImageDrawable(
            VectorDrawableCompat.create(
                activity.resources,
                if (item.isStation) R.drawable.ic_bus_stop_48dp else R.drawable.ic_bus_48dp,
                activity.theme
            )
        )

        holder.itemView.mainTitleTextView.text = item.name
        if (item.isStation) {
            holder.itemView.mainDescriptionTextView.text = item.stationNumber
            holder.itemView.mainTypeTextView.visibility = View.GONE
            if (item.distance != null) {
                holder.itemView.mainDistanceLayout.visibility = View.VISIBLE
                holder.itemView.mainDistanceImageView.setImageDrawable(
                    VectorDrawableCompat.create(
                        activity.resources, R.drawable.ic_my_location_black_48dp, activity.theme
                    )
                )
                val themeColorAttribute =
                    activity.obtainStyledAttributes(intArrayOf(R.attr.themeColor))
                holder.itemView.mainDistanceTextView.setTextColor(
                    themeColorAttribute.getColorStateList(0)
                )
                themeColorAttribute.recycle()
                holder.itemView.mainDistanceTextView.text =
                    if (item.distance < 1000) "${round(item.distance).toInt()}m"
                    else "${round(item.distance / 10) / 100}km"
            } else holder.itemView.mainDistanceLayout.visibility = View.GONE
        } else {
            val descriptionText = "${item.startStation}  ->  ${item.endStation}"
            holder.itemView.mainDescriptionTextView.text = descriptionText
            holder.itemView.mainTypeTextView.visibility = View.VISIBLE
            holder.itemView.mainTypeTextView.text =
                item.routeType!!.substring(0, item.routeType.length - 2)
            holder.itemView.mainTypeTextView.backgroundTintList = when (item.routeType) {
                "좌석버스" -> ActivityCompat.getColorStateList(activity, R.color.type2)
                else -> ActivityCompat.getColorStateList(activity, R.color.type1)
            }
            holder.itemView.mainDistanceLayout.visibility = View.GONE
        }

        setFavoriteButton(holder, item.isStation, item.id)

        holder.itemView.setOnClickListener {
            if (!onDetail) {
                onDetail = true
                it.context.startActivity<DetailActivity>(
                    "isStation" to item.isStation,
                    "id" to item.id,
                    "name" to item.name,
                    "stationNumber" to item.stationNumber,
                    "lat" to item.lat,
                    "lng" to item.lng,
                    "routeType" to item.routeType,
                    "startStation" to item.startStation,
                    "endStation" to item.endStation
                )
            }
        }
    }

    private fun setFavoriteButton(holder: MyViewHolder, isStation: Boolean, id: String) {
        val filter = realm.where<DataFavorite>()
            .equalTo("isStation", isStation).equalTo("id", id)
        holder.itemView.mainFavoriteIconView.setImageDrawable(
            VectorDrawableCompat.create(
                activity.resources,
                if (filter.findFirst() == null) R.drawable.ic_favorite_border_black_48dp
                else R.drawable.ic_favorite_black_48dp, activity.theme
            )
        )
        holder.itemView.mainFavoriteIconView.setOnClickListener {
            if (filter.findFirst() == null) {
                favoriteOn(isStation, id)
                (it as ImageView).setImageDrawable(
                    VectorDrawableCompat.create(
                        activity.resources, R.drawable.ic_favorite_black_48dp, activity.theme
                    )
                )
            } else {
                favoriteOff(isStation, id)
                (it as ImageView).setImageDrawable(
                    VectorDrawableCompat.create(
                        activity.resources, R.drawable.ic_favorite_border_black_48dp, activity.theme
                    )
                )
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

}