package jp.gr.aqua.vtextviewerapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import jp.gr.aqua.vtextviewer.PreferenceActivity
import jp.gr.aqua.vtextviewerapp.databinding.ActivityLauncherBinding
import kotlin.math.max


class LaunchActivity : AppCompatActivity() {

    companion object{
        const val JOTA_PACKAGE = "jp.sblo.pandora.jota.plus"
        const val BETA_PACKAGE = "jp.gr.aqua.jotaplus.beta"
        const val MAIN_CLASS = "jp.sblo.pandora.jotaplus.Main"
        const val STORE_URL = "market://details?id="
    }

    lateinit var binding : ActivityLauncherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // edge-to-edge を維持
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT,Color.TRANSPARENT,),        // 通知バーの文字を常に黒で描画する
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT,Color.TRANSPARENT,),
        )
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

        binding.openJota.setOnClickListener {
            openJota(JOTA_PACKAGE)
        }
        binding.openBeta.setOnClickListener {
            openJota(BETA_PACKAGE)
        }
        binding.settings.setOnClickListener {
            val intent = Intent(this, PreferenceActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun openJota(pkgname : String){
        val intent = Intent().apply{
            setClassName(pkgname, MAIN_CLASS)
        }
        try{
            startActivity(intent)
        }catch(e:Exception){
            val uri = STORE_URL + pkgname
            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            try {
                startActivity(storeIntent)
            }catch(e:Exception){
                e.printStackTrace()
            }
        }
    }
}

