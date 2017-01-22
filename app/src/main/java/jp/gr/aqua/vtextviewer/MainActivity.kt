package jp.gr.aqua.vtextviewer

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import jp.gr.aqua.vjap.VTextLayout

class MainActivity : AppCompatActivity() {

    private val KEY_POSITION = "position"
    private val vTextLayout by lazy { findViewById(R.id.vTextLayout) as VTextLayout }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val position = savedInstanceState?.getInt(KEY_POSITION) ?: 0

        // 設定読込
        val pr = Preferences(this)
        val fontKind = pr.getFontKind()
        val fontSet = if ( fontKind=="mincho") "ipam.ttf" to true else "ipag.ttf" to false
        val fontSize = pr.getFontSize()
        val charMax = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 縦向きの場合
            pr.getCharMaxPort()
        }else{
            // 横向きの場合
            pr.getCharMaxLand()
        }
        val rubyMode = pr.getRubyMode()

        if (intent.action == Intent.ACTION_SEND ) {
            val extras = intent.extras
            extras?.let{
                val text = it.getCharSequence(Intent.EXTRA_TEXT)
                text?.let{
                    vTextLayout.apply{
                        setText(it.toString())
                        setFont( (fontSize * resources.getDimension(R.dimen.font_size_unit)).toInt() ,
                                Typeface.createFromAsset( assets, fontSet.first) , fontSet.second )
                        setPadding( resources.getDimension(R.dimen.padding).toInt() )
                        setInitialPosition( position )
                        setWrapPosition( charMax )
                        setRubyMode(rubyMode)
                    }
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
                        vTextLayout.apply{
                            setText(it.toString())
                            //setInitialPosition(position)
                            reLayoutChildren()
                        }
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
