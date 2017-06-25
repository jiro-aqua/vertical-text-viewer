package jp.gr.aqua.vtextviewer

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.support.v7.preference.*

class PreferenceFragment : PreferenceFragmentCompat() {

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val pm = preferenceManager

        val ps = pm.preferenceScreen

        // IPAフォントについて
        ps.findPreference(Preferences.KEY_IPA)
            .setOnPreferenceClickListener { showMessage(R.string.about_ipa_font , "IPA_Font_License_Agreement_v1.0.txt" ) }

        // バージョン
        ps.findPreference(Preferences.KEY_ABOUT)
                .setSummary("version: ${BuildConfig.VERSION_NAME} (c)Aquamarine Networks.")

    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context as OnFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener

    private fun showMessage(titleResId : Int , assetText:String) : Boolean {
        val message = context.assets.open(assetText).reader(charset=Charsets.UTF_8).use{it.readText()}
        AlertDialog.Builder(context)
                .setTitle(titleResId)
                .setMessage(message)
                .setPositiveButton(R.string.ok,null)
                .show()

        return false
    }

}
