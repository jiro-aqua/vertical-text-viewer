package jp.gr.aqua.vtextviewer

import android.content.Context
import android.content.SharedPreferences

class Flags(context: Context){

    companion object {
        const val PREF_FILE="flags"

        const val KEY_IS_NEWS_READ="is_news_read_202206"
        const val KEY_USAGE_COUNTER="usage_counter"
    }

    private val sp : SharedPreferences = context.getSharedPreferences(PREF_FILE,Context.MODE_PRIVATE)

    val isStandalone = context.packageName == "jp.gr.aqua.jota.vtextviewer"

    var isNewsRead : Boolean
        get() = sp.getBoolean(KEY_IS_NEWS_READ,false)
        set(value) = sp.edit().putBoolean(KEY_IS_NEWS_READ,value).apply()

    var usageCounter : Long
        get() = sp.getLong(KEY_USAGE_COUNTER,0L)
        set(value) = sp.edit().putLong(KEY_USAGE_COUNTER,value).apply()

}