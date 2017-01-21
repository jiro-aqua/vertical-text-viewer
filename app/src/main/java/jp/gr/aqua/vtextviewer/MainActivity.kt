package jp.gr.aqua.vtextviewer

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import jp.gr.aqua.vjap.VTextLayout

class MainActivity : AppCompatActivity() {

    private val KEY_POSITION = "position"
    private val vTextLayout by lazy { findViewById(R.id.vTextLayout) as VTextLayout }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val position = savedInstanceState?.getInt(KEY_POSITION) ?: 0

        if (intent.action == Intent.ACTION_SEND ) {
            val extras = intent.extras
            extras?.let{
                val text = it.getCharSequence(Intent.EXTRA_TEXT)
                text?.let{
                    vTextLayout.setText(it.toString())
                    vTextLayout.setFont( resources.getDimension(R.dimen.font_size_middle).toInt() ,
                            Typeface.createFromAsset( assets, "ipam.ttf") ,
                            true )
                    vTextLayout.setPadding( resources.getDimension(R.dimen.padding).toInt() )
                    vTextLayout.setInitialPosition( position )
                }?:finish()
            }?:finish()
        }else{
            finish()
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if ( intent != null ) {
            if (intent.action == Intent.ACTION_SEND) {
                setIntent(intent)
                val extras = intent.extras
                extras?.let {
                    val text = it.getCharSequence(Intent.EXTRA_TEXT)
                    text?.let {
                        vTextLayout.setText(it.toString())
                        //vTextLayout.setInitialPosition(position)
                        vTextLayout.reLayoutChildren()
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(KEY_POSITION, vTextLayout.getCurrentPosition())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState?.getInt(KEY_POSITION)?.let{
            vTextLayout.setInitialPosition(it)
        }
    }

}
