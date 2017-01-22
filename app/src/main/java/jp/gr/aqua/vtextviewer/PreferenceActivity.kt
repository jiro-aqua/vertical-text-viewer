package jp.gr.aqua.vtextviewer


import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class PreferenceActivity : AppCompatActivity(), PreferenceFragment.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)
    }
}