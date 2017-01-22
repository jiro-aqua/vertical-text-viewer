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
            .setOnPreferenceClickListener {
                val message = context.assets.open("IPA_Font_License_Agreement_v1.0.txt").reader(charset=Charsets.UTF_8).use{it.readText()}
                AlertDialog.Builder(context)
                        .setTitle(R.string.about_ipa_font)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok,null)
                        .show()

                false
            }

//        // 文字数
//        (ps.findPreference(Preferences.KEY_CHAR_MAX_PORT) as EditTextPreference)
//                .getE

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
}
