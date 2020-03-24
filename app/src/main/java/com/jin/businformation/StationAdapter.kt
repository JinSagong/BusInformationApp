package com.jin.businformation

import android.app.Activity
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import io.realm.Realm
import kotlinx.android.synthetic.main.item_detail.view.*
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.startActivity

class StationAdapter(items: ArrayList<DetailActivity.DataStationInfo>, act: Activity) :
    RecyclerView.Adapter<StationAdapter.MyViewHolder>() {
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

        holder.itemView.detailItemTitleTextView.text = item.routeName
        holder.itemView.detailItemTypeTextView.text =
            item.routeType.substring(0, item.routeType.length - 2)
        holder.itemView.detailItemTypeTextView.backgroundTintList = when (item.routeType) {
            "좌석버스" -> ActivityCompat.getColorStateList(activity, R.color.type2)
            else -> ActivityCompat.getColorStateList(activity, R.color.type1)
        }
        val descriptionText = "${item.startStation}  ->  ${item.endStation}"
        holder.itemView.detailItemDescriptionTextView.text = descriptionText

        holder.itemView.detailItemInfoLayout.removeAllViews()
        if (item.arrList != null) {
            item.arrList.forEach {
                createCurrentInfo(holder, it.time, it.stationName!!, it.stationCount)
            }
        } else {
            val contentTextView = TextView(activity)
            contentTextView.text = activity.getString(R.string.noStationInfo)
            contentTextView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX, activity.resources.getDimension(R.dimen.textSize1)
            )
            contentTextView.setTextColor(ActivityCompat.getColor(activity, R.color.textColor1))
            contentTextView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            holder.itemView.detailItemInfoLayout.addView(contentTextView)
        }

        holder.itemView.setOnClickListener {
            if (!onChange) {
                onChange = true
                it.context.startActivity<DetailActivity>(
                    "isStation" to false,
                    "id" to item.routeId,
                    "name" to item.routeName,
                    "stationNumber" to null,
                    "lat" to null,
                    "lng" to null,
                    "routeType" to item.routeType,
                    "startStation" to item.startStation,
                    "endStation" to item.endStation
                )
                activity.finish()
            }
        }
    }

    private fun createCurrentInfo(holder: MyViewHolder, time: Int, name: String, count: Int) {
        val content1 = if (time / 60 <= 1 || count == 1) " 곧 도착" else " 약 ${time / 60}분 후"
        val content2 = if (name != "null") " (${count}정거장 전: $name)" else " (${count}정거장 전)"

        val containerLayout = LinearLayout(activity)
        containerLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        containerLayout.gravity = Gravity.CENTER

        val iconImageView = ImageView(activity)
        iconImageView.setImageResource(R.drawable.ic_watch_later_black_48dp)
        iconImageView.layoutParams = LinearLayout.LayoutParams(
            activity.resources.getDimensionPixelSize(R.dimen.miniIconSize),
            activity.resources.getDimensionPixelSize(R.dimen.miniIconSize)
        )
        val contentTextView1 = TextView(activity)
        contentTextView1.text = content1
        contentTextView1.setTextSize(
            TypedValue.COMPLEX_UNIT_PX, activity.resources.getDimension(R.dimen.textSize1)
        )
        contentTextView1.setTextColor(activity.colorAttr(R.attr.themeColor))
        contentTextView1.typeface = Typeface.DEFAULT_BOLD
        contentTextView1.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val contentTextView2 = TextView(activity)
        contentTextView2.text = content2
        contentTextView2.setTextSize(
            TypedValue.COMPLEX_UNIT_PX, activity.resources.getDimension(R.dimen.textSize1)
        )
        contentTextView2.setTextColor(activity.colorAttr(R.attr.themeColor))
        contentTextView2.maxLines = 1
        contentTextView2.ellipsize = TextUtils.TruncateAt.END
        contentTextView2.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )

        containerLayout.addView(iconImageView)
        containerLayout.addView(contentTextView1)
        containerLayout.addView(contentTextView2)
        holder.itemView.detailItemInfoLayout.addView(containerLayout)
    }

}