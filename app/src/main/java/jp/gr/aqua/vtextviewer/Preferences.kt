package jp.gr.aqua.vtextviewer

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class Preferences(context: Context){

    companion object {
        val KEY_ABOUT ="app_about"
        val KEY_USAGE="app_usage"
        val KEY_FONT_KIND="font_kind"
        val KEY_FONT_SIZE="font_size"
        val KEY_CHAR_MAX_PORT="char_max_port"
        val KEY_CHAR_MAX_LAND="char_max_land"
        val KEY_RUBY_KIND="ruby_kind"
        val KEY_IPA="about_ipa"
    }

    val sp : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getFontKind() : String
    {
        return sp.getString(KEY_FONT_KIND,"mincho")
    }

    fun getFontSize() : Int
    {
        val fontsize = sp.getString(KEY_FONT_SIZE,"16")
        return fontsize.toInt()
    }

    fun getCharMaxPort() : Int
    {
        return sp.getString(KEY_CHAR_MAX_PORT,"0").toIntSafety()
    }

    fun getCharMaxLand() : Int
    {
        return sp.getString(KEY_CHAR_MAX_LAND,"0").toIntSafety()
    }

    fun String.toIntSafety() : Int = if ( this.isEmpty() ) 0 else this.toInt()

    fun getRubyMode() : String
    {
        return sp.getString(KEY_RUBY_KIND,"aozora")
    }

}