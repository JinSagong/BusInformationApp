package com.jin.businformation

import android.app.Activity
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.realm.Realm
import kotlinx.android.synthetic.main.item_detail.view.*
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.startActivity

class RouteAdapter(items: ArrayList<DetailActivity.DataRouteInfo>, act: Activity) :
    RecyclerView.Adapter<RouteAdapter.MyViewHolder>() {
    private val realm = Realm.getDefaultInstance()

    private val list = items
    private val activity = act

    var onChange = false

    class MyViewHolder(v: View) : RecyclerView.ViewHolder(v)

    fun onStop() {
        realm.close()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = list[position]

        holder.itemView.detailItemTitleTextView.text = item.nodeName
        holder.itemView.detailItemTypeTextView.visibility = View.GONE
        holder.itemView.detailItemDescriptionTextView.text = item.nodeNo

        holder.itemView.detailItemInfoLayout.removeAllViews()
        if (item.locationList != null) {
            holder.itemView.detailItemInfoLayout.visibility = View.VISIBLE
            item.locationList.forEach { createCurrentInfo(holder, it) }
        } else {
            holder.itemView.detailItemInfoLayout.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (!onChange) {
                onChange = true
                it.context.startActivity<DetailActivity>(
                    "isStation" to true,
                    "id" to item.nodeId,
                    "name" to item.nodeName,
                    "stationNumber" to item.nodeNo,
                    "lat" to item.lat,
                    "lng" to item.lng,
                    "routeType" to null,
                    "startStation" to null,
                    "endStation" to null
                )
                activity.finish()
            }
        }
    }

    private fun createCurrentInfo(holder: MyViewHolder, number: String) {
        val containerLayout = LinearLayout(activity)
        containerLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        containerLayout.gravity = Gravity.CENTER

        val iconImageView = ImageView(activity)
        iconImageView.setImageResource(R.drawable.ic_bus_48dp)
        iconImageView.layoutParams = LinearLayout.LayoutParams(
            activity.resources.getDimensionPixelSize(R.dimen.miniIconSize),
            activity.resources.getDimensionPixelSize(R.dimen.miniIconSize)
        )
        val contentTextView = TextView(activity)
        val contentText = " $number"
        contentTextView.text = contentText
        contentTextView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX, activity.resources.getDimension(R.dimen.textSize1)
        )
        contentTextView.setTextColor(activity.colorAttr(R.attr.themeColor))
        contentTextView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )

        containerLayout.addView(iconImageView)
        containerLayout.addView(contentTextView)
        holder.itemView.detailItemInfoLayout.addView(containerLayout)
    }

}