package jp.gr.aqua.vtextviewer


import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

class PreferenceActivity : AppCompatActivity(), PreferenceFragment.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // edge-to-edge を維持
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val sys = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)   // ← コンテンツだけ避ける
            WindowInsetsCompat.CONSUMED
        }

        setContentView(R.layout.activity_vtext_preference)
        setTitle(R.string.vtext_app_name)
    }
}