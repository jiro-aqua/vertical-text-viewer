package jp.gr.aqua.vtextviewer

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import jp.gr.aqua.vjap.VTextLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        private const val KEY_POSITION = "position"
        private const val intentAction = "jp.gr.aqua.jota.vtextviewer.ACTION_OPEN"
        //private val mimeType = "text/plain"
        private const val EXTRA_START = "EXTRA_START"
        private const val EXTRA_END = "EXTRA_END"
        private const val EXTRA_POINTED = "EXTRA_POINTED"

        private const val WRITING_PAPER_CHARS = 20
    }
    private val vTextLayout by lazy { findViewById(R.id.vTextLayout) as VTextLayout }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vtext_main)

        val position = savedInstanceState?.getInt(KEY_POSITION) ?: 0

        // 設定読込
        val pr = Preferences(this)
        val fontKind = pr.getFontKind()
        val fontSet = when ( fontKind ) {
            "mincho" -> "ipam.ttf" to true
            "morisawa_mincho" -> "BIZUDMincho-Regular.ttf" to true
            "morisawa_gothic" -> "BIZUDGothic-Regular.ttf" to false
            else /*"gothic"*/ -> "ipag.ttf" to false
        }
        val fontSize = pr.getFontSize()
        val charMax = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 縦向きの場合
            pr.getCharMaxPort()
        }else{
            // 横向きの場合
            pr.getCharMaxLand()
        }
        val writingPaperMode = pr.isWritingPaperMode()

        val (fontColor,bgColor) =  if ( pr.isBackgroundBlack() ){
            // 黒背景
            @Suppress("DEPRECATION")
            resources.getColor(R.color.vtext_color_dkgray) to resources.getColor(R.color.vtext_color_black)
        }else{
            // 白背景
            @Suppress("DEPRECATION")
            resources.getColor(R.color.vtext_color_black) to resources.getColor(R.color.vtext_color_white)
        }

        if(intent.action == intentAction ){
            val start = intent.getIntExtra(EXTRA_START,0)
            val end = intent.getIntExtra(EXTRA_END,0)

            val uri = intent.data
            vTextLayout.apply {
                setText("")
                setColor(fontColor,bgColor)
                setInitialPosition(position)
                setWritingPaperMode(writingPaperMode)
                setWritingPaperChars(WRITING_PAPER_CHARS)
                setFont((fontSize * resources.getDimension(R.dimen.vtext_font_size_unit)).toInt(),
                        Typeface.createFromAsset(assets, fontSet.first), fontSet.second)
                setPadding(resources.getDimension(R.dimen.vtext_padding).toInt())
                setWrapPosition(charMax)
            }

            lifecycleScope.launch {
                try{
                    val text = withContext(Dispatchers.IO){
                        loadContent(uri!!)
                    }
                    text.let {
                        vTextLayout.apply{
                            setText(it)
                            if ( position == 0 ){
                                setInitialPosition((start+end)/2)
                            }
                            reLayoutChildren()
                        }
                    }
                }
                catch (e:Exception){
                    e.printStackTrace()
                    finish()
                }
            }
        }else{
            finish()
        }

        // 文字をダブルクリックされたら、その文字のカーソル位置を持って終了
        vTextLayout.setOnDoubleClickListener {
            pointed->
            val intent = intent.apply{
                val pos = vTextLayout.getCurrentStartPosition()
                putExtra(EXTRA_START,pos)
                putExtra(EXTRA_END,pos)
                putExtra(EXTRA_POINTED,pointed)
            }
            setResult(Activity.RESULT_OK,intent)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply{
            putInt(KEY_POSITION, vTextLayout.getCurrentPosition())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.apply{
            vTextLayout.setInitialPosition(getInt(KEY_POSITION))
        }
    }

    @Throws(Exception::class)
    private fun loadContent(uri: Uri): String {
        return contentResolver.openInputStream(uri)?.bufferedReader(charset = Charsets.UTF_8).use { it!!.readText() }
    }

    override fun onBackPressed() {
        val intent = intent.apply{
            val position = vTextLayout.getCurrentStartPosition()
            putExtra(EXTRA_START,position)
            putExtra(EXTRA_END,position)
        }
        setResult(Activity.RESULT_OK,intent)
        super.onBackPressed()
    }


}
