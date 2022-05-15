package jp.gr.aqua.vtextviewer

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.preference.*


class PreferenceFragment : PreferenceFragmentCompat() {

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the vtext_preferences from an XML resource
        setPreferencesFromResource(R.xml.vtext_preferences, rootKey)

        val pm = preferenceManager

        val ps = pm.preferenceScreen

        // 原稿用紙モード
        val writingPaperModeEnabler : (value: Boolean)->Unit = { value->
            ps.findPreference<Preference>(Preferences.KEY_FONT_SIZE)?.isEnabled = !value
            ps.findPreference<Preference>(Preferences.KEY_CHAR_MAX_PORT)?.isEnabled = !value
            ps.findPreference<Preference>(Preferences.KEY_CHAR_MAX_LAND)?.isEnabled = !value
        }

        writingPaperModeEnabler(Preferences(requireContext()).isWritingPaperMode())

        ps.findPreference<Preference>(Preferences.KEY_WRITING_PAPER)
                ?.setOnPreferenceChangeListener { _, newValue -> if ( newValue is Boolean ) writingPaperModeEnabler(newValue)
                    true
                }

        // IPAフォントについて
        ps.findPreference<Preference>(Preferences.KEY_IPA)
            ?.setOnPreferenceClickListener { showMessage(R.string.vtext_about_ipa_font, "IPA_Font_License_Agreement_v1.0.txt") }

        // モリサワ BIZ UD明朝フォントについて
        ps.findPreference<Preference>(Preferences.KEY_ABOUT_MORISAWA_MINCHO)
            ?.setOnPreferenceClickListener { showMessage(R.string.vtext_about_morisawa_mincho, "OFL_bizmincho.txt") }

        // モリサワ BIZ UDゴシックフォントについて
        ps.findPreference<Preference>(Preferences.KEY_ABOUT_MORISAWA_GOTHIC)
            ?.setOnPreferenceClickListener { showMessage(R.string.vtext_about_morisawa_gothic, "OFL_bizgothic.txt") }

        // バージョン
        ps.findPreference<Preference>(Preferences.KEY_ABOUT)
                ?.setSummary("version: ${versionName} (c)Aquamarine Networks.")

    }

    private val versionName: String
        get() {
        val pm = requireActivity().packageManager
        var versionName = ""
        try {
            val packageInfo = pm.getPackageInfo(requireActivity().packageName, 0)
            versionName = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionName
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener

    private fun showMessage(titleResId: Int, assetText: String) : Boolean {
        val message = requireContext().assets.open(assetText).reader(charset = Charsets.UTF_8).use{it.readText()}
        AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setMessage(message)
                .setPositiveButton(R.string.vtext_ok, null)
                .show()

        return false
    }

}
