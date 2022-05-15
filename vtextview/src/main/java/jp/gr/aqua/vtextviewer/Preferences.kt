package jp.gr.aqua.vtextviewer

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class Preferences(context: Context){

    companion object {
        const val KEY_ABOUT ="app_about"
        const val KEY_USAGE="app_usage"
        const val KEY_FONT_KIND="font_kind"
        const val KEY_FONT_SIZE="font_size"
        const val KEY_CHAR_MAX_PORT="char_max_port"
        const val KEY_CHAR_MAX_LAND="char_max_land"
        const val KEY_IPA="about_ipa"
        const val KEY_ABOUT_MORISAWA_MINCHO="about_morisawa_mincho"
        const val KEY_ABOUT_MORISAWA_GOTHIC="about_morisawa_gothic"
        const val KEY_WRITING_PAPER="writing_paper"
        const val KEY_BACKGROUND_BLACK="background_black"
    }

    private val sp : SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getFontKind() : String
    {
        return sp.getString(KEY_FONT_KIND,null) ?: "mincho"
    }

    fun getFontSize() : Int
    {
        val fontsize = sp.getString(KEY_FONT_SIZE,null) ?: "16"
        return fontsize.toInt()
    }

    fun getCharMaxPort() : Int
    {
        return (sp.getString(KEY_CHAR_MAX_PORT,null) ?: "0").toIntSafety()
    }

    fun getCharMaxLand() : Int
    {
        return ( sp.getString(KEY_CHAR_MAX_LAND,null) ?: "0").toIntSafety()
    }

    fun isWritingPaperMode() : Boolean
    {
        return sp.getBoolean(KEY_WRITING_PAPER,false)
    }

    fun isBackgroundBlack() : Boolean
    {
        return sp.getBoolean(KEY_BACKGROUND_BLACK,false)
    }

    private fun String.toIntSafety() : Int = if ( this.isEmpty() ) 0 else this.toInt()

}