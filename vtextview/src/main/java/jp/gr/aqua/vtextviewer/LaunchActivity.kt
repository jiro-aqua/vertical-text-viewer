package jp.gr.aqua.vtextviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.gr.aqua.vtextviewer.databinding.ActivityLauncherBinding


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

