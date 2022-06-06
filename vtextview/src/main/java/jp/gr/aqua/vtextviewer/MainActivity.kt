package jp.gr.aqua.vtextviewer

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import com.google.android.play.core.review.ReviewManagerFactory
import jp.gr.aqua.vtextviewer.databinding.ActivityHtextBinding
import jp.gr.aqua.vtextviewer.databinding.ActivityVtextMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val KEY_POSITION = "position"
        private const val intentAction = "jp.gr.aqua.jota.vtextviewer.ACTION_OPEN"
        //private val mimeType = "text/plain"
        private const val EXTRA_START = "EXTRA_START"
        private const val EXTRA_END = "EXTRA_END"
        private const val EXTRA_POINTED = "EXTRA_POINTED"

        private const val WRITING_PAPER_CHARS = 20

        private const val HTML_NAME = "preview.html"
        private const val HTML_PATH = "preview_html"

        private const val FONT_IPAM =  "ipam.ttf"
        private const val FONT_IPAG =  "ipag.ttf"
        private const val FONT_BIZM =  "BIZUDMincho-Regular.ttf"
        private const val FONT_BIZG =  "BIZUDGothic-Regular.ttf"

        private val FONTS = listOf(FONT_IPAG,FONT_IPAM, FONT_BIZG, FONT_BIZM)
    }

    private val htmlDir by lazy { File(filesDir, HTML_PATH) }
    private val previewFile by lazy { File(htmlDir, HTML_NAME) }

    private lateinit var vBinding : ActivityVtextMainBinding
    private lateinit var hBinding : ActivityHtextBinding

    private var tategakiMode : Boolean = true

    private val pr by lazy { Preferences(this) }
    private val flags by lazy { Flags(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        copyFonts()

        tategakiMode = !pr.isYokogaki()

        if (tategakiMode) {
            vBinding = ActivityVtextMainBinding.inflate(layoutInflater)
            setContentView(vBinding.root)
        } else {
            hBinding = ActivityHtextBinding.inflate(layoutInflater)
            setContentView(hBinding.root)
        }

        val position = savedInstanceState?.getInt(KEY_POSITION) ?: 0

        // 設定読込
        val fontKind = pr.getFontKind()
        val fontSet = when (fontKind) {
            "mincho" -> FONT_IPAM to true
            "morisawa_mincho" -> FONT_BIZM to true
            "morisawa_gothic" -> FONT_BIZG to false
            else /*"gothic"*/ -> FONT_IPAG to false
        }
        val fontSize = pr.getFontSize()
        val charMax =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                // 縦向きの場合
                pr.getCharMaxPort()
            } else {
                // 横向きの場合
                pr.getCharMaxLand()
            }
        val writingPaperMode = pr.isWritingPaperMode()

        val useDarkMode = pr.isUseDarkMode()
        var useBlack = false

        if (useDarkMode) {
            val currentNightMode =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            when (currentNightMode) {
                Configuration.UI_MODE_NIGHT_NO -> { useBlack = false }
                Configuration.UI_MODE_NIGHT_YES -> {  useBlack = true }
            }
        }else{
            useBlack = pr.isBackgroundBlack()
        }

        val (fontColor,bgColor) =  if ( useBlack ){
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

            if ( tategakiMode ){
                vBinding.vTextLayout.apply {
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
            }else{
                val assetLoader = WebViewAssetLoader.Builder()
                    .setDomain("www.aqua.gr.jp")
//                    .addPathHandler("/asset/", WebViewAssetLoader.AssetsPathHandler(this))
                    .addPathHandler("/", WebViewAssetLoader.InternalStoragePathHandler(this, htmlDir))
                    .build()

                val client = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if ( url?.contains('#') != true ) {
                            hBinding.webview.visibility = View.VISIBLE
                            hBinding.progressbar.visibility = View.GONE
                            hBinding.webview.loadUrl("https://www.aqua.gr.jp/$HTML_NAME#pos")
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, _url: String?): Boolean {
                        handleUrl(_url)
                        return true
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    if ( useBlack ) {
                        WebSettingsCompat.setForceDark(
                            hBinding.webview.settings,
                            WebSettingsCompat.FORCE_DARK_ON
                        )
                    }else{
                        WebSettingsCompat.setForceDark(
                            hBinding.webview.settings,
                            WebSettingsCompat.FORCE_DARK_OFF
                        )
                    }
                }
                hBinding.webview.webViewClient = client
            }

            lifecycleScope.launch {
                try{
                    val text = withContext(Dispatchers.IO){
                        loadContent(uri!!)
                    }
                    text.let {
                        if (tategakiMode) {
                            vBinding.vTextLayout.apply {
                                setText(it)
                                if (position == 0) {
                                    setInitialPosition((start + end) / 2)
                                }
                                reLayoutChildren()
                            }
                        } else {
                            val htmlFile = File(htmlDir, HTML_NAME)
                            withContext(Dispatchers.IO) {
                                val html = RubyToHtmlConverter(this@MainActivity)
                                    .toHtml(
                                        text = it,
                                        fontface = fontSet.first,
                                        fontsize = fontSize,
                                        start = start,
                                    )
                                htmlFile.writeText(html, charset = Charsets.UTF_8)
                            }
                            val url = "https://www.aqua.gr.jp/$HTML_NAME"
                            hBinding.webview.loadUrl(url)
                        }
                        if ( text.length > 3000 ){
                            flags.usageCounter = flags.usageCounter + 1     // 3000文字以上あれば正規ユーザーなので、カウンタをアップ
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

        if ( tategakiMode ){
            // 文字をダブルクリックされたら、その文字のカーソル位置を持って終了
            vBinding.vTextLayout.setOnDoubleClickListener {
                    pointed->
                val intent = intent.apply{
                    val pos = vBinding.vTextLayout.getCurrentStartPosition()
                    putExtra(EXTRA_START,pos)
                    putExtra(EXTRA_END,pos)
                    putExtra(EXTRA_POINTED,pointed)
                }
                setResult(Activity.RESULT_OK,intent)
                finish()
            }
        }

        val settings = findViewById<ImageView>(R.id.settings)
        settings?.setOnClickListener {
            val intent = Intent(this,PreferenceActivity::class.java)
            startActivity(intent)
        }

        pr.addChangedListener(preferenceChangedListner)

        if ( !flags.isNewsRead ){
            lifecycleScope.launch {
                val message = withContext(Dispatchers.IO){
                    assets.open("news_202206.txt").reader(charset = Charsets.UTF_8).readText()
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.vtext_app_name)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.vtext_ok){ _,_ ->
                        flags.isNewsRead = true
                    }
                    .show()
            }
        }

        if ( flags.usageCounter > 19 && (flags.usageCounter.mod(10L)) == 0L){        // よく使うユーザーであればレビューリクエスト
            val manager = ReviewManagerFactory.create(this)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // We got the ReviewInfo object
                    val reviewInfo = task.result
                    if ( reviewInfo != null) {
                        val flow = manager.launchReviewFlow(this, reviewInfo)
                        flow.addOnCompleteListener { _ ->
                            // The flow has finished. The API does not indicate whether the user
                            // reviewed or not, or even whether the review dialog was shown. Thus, no
                            // matter the result, we continue our app flow.
                        }
                    }
                } else {
                    // There was some problem, log or handle the error code.
                    task.exception?.printStackTrace()
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        pr.removeChangedListener(preferenceChangedListner)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply{
            if ( tategakiMode ) {
                putInt(KEY_POSITION, vBinding.vTextLayout.getCurrentPosition())
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.apply{
            if ( tategakiMode ) {
                vBinding.vTextLayout.setInitialPosition(getInt(KEY_POSITION))
            }
        }
    }

    @Throws(Exception::class)
    private fun loadContent(uri: Uri): String {
        return contentResolver.openInputStream(uri)?.reader(charset = Charsets.UTF_8)?.readText() ?: ""
    }

    override fun onBackPressed() {
        if ( tategakiMode ){
            val intent = intent.apply{
                val position = vBinding.vTextLayout.getCurrentStartPosition()
                putExtra(EXTRA_START,position)
                putExtra(EXTRA_END,position)
            }
            setResult(Activity.RESULT_OK,intent)
        }
        super.onBackPressed()
    }

    private fun handleUrl(url:String?) {
        RubyToHtmlConverter.handleUrl(url)?.let{
            // 編集ボタンを押されたら、その行の番号を持って終了
            val intent = intent.apply {
                putExtra(EXTRA_START,it)
                putExtra(EXTRA_END,it)
                putExtra(EXTRA_POINTED,it)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun copyFonts(){
        htmlDir.mkdirs()
        FONTS.forEach {
            if ( !File(htmlDir,it).exists() ){
                assets.open(it).copyTo(File(htmlDir,it).outputStream())
            }
        }
    }

    private val preferenceChangedListner = { // Preferenceが変更されたらfinish()
        finish()
    }
}
