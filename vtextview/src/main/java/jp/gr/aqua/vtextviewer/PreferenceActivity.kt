package jp.gr.aqua.vtextviewer


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PreferenceActivity : AppCompatActivity(), PreferenceFragment.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vtext_preference)
        setTitle(R.string.vtext_app_name)
    }
}