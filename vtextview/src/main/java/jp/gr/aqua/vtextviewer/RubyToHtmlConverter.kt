package jp.gr.aqua.vtextviewer

import android.content.Context
import android.net.Uri
import java.util.regex.Pattern

class RubyToHtmlConverter(val context: Context) {
    companion object {
        private const val PLACEHOLDER_H1 = "!!!TEMPLATE-H1!!!"
        private const val PLACEHOLDER_BODY = "!!!BODY!!!"
        private const val PLACEHOLDER_FONTFACE = "!!!FONTFACE!!!"
        private const val PLACEHOLDER_FONTSIZE = "!!!FONTSIZE!!!"
        private const val PLACEHOLDER_COLORSCHEME = "!!!COLORSCHEME!!!"


        private const val PREVIEW = "preview"
        private const val JUMP = "jump"
        private const val POS = "pos"

        fun handleUrl(url:String?) : Int? {
            return url?.let {
                val uri = Uri.parse(url)
                return if (uri.scheme == PREVIEW && uri.host == JUMP) {
                    val jump = uri.getQueryParameter(POS)
                    jump?.toIntOrNull()
                }else null
            }
        }

    }


    fun toHtml(text: String, fontface: String, fontsize: Int, start: Int) : String {
        val data = text.lines()
        var template = context.assets.open("template.html").reader(charset = Charsets.UTF_8).readText()

        val lineNum = data.count()

        template = template.replace(PLACEHOLDER_FONTFACE, fontface)
        template = template.replace(PLACEHOLDER_FONTSIZE, fontsize.toString())

        template = if ( lineNum > 0 ){
            template.replace(PLACEHOLDER_H1, """${replaceRubys(data[0])}<a href="${PREVIEW}://${JUMP}?${POS}=0">✎</a>""")
        }else{
            template.replace(PLACEHOLDER_H1, "")
        }
        template = if ( lineNum > 1 ){
            var pos = 0
            val body = data.foldIndexed(StringBuilder()) { index, acc, s ->
                if (pos <= start && start < pos + s.length + 1) {
                    acc.append("""<div id="pos"/>""")
                }
                if (index != 0) {
                    acc.append(
                        if (s.isEmpty()) {
                            """<br/>""" + "\n"
                        } else {
                            """${replaceRubys(s)}<a href="${PREVIEW}://${JUMP}?${POS}=$pos">✎</a><br/>""" + "\n"
                        }
                    )
                }
                pos += s.length + 1
                acc
            }.toString()
            template.replace(PLACEHOLDER_BODY, body)
        }else {
            template.replace(PLACEHOLDER_BODY, "")
        }
        return template
    }

    private fun replaceRubys(text : String ) : String {
        return text
            .replace("""<""", "&lt;")
            .replace(""">""", "&gt;")
            .replaceDot()
            /* BCCKS 記法*/
            .replaceRuby("""\{(.+?)\}\((.+?)\)""")
            /* 青空文庫・なろう・カクヨム記法 */
            .replaceRuby("""[\|｜](.+?)《(.+?)》""")
            .replaceRuby("""[\|｜](.+?)（(.+?)）""")
            .replaceRuby("""[\|｜](.+?)\((.+?)\)""")
            /* 漢字の連続の後に括弧が存在した場合、一連の漢字をベーステキスト、括弧内の文字をルビテキストとします。 */
            .replaceRuby("""([一-龠々ヶ]+)《(.+?)》""")
//            .replaceRuby("""([ぁ-んァ-ヶ]+)《(.+?)》""")
//            .replaceRuby("""([A-Za-z]+)《(.+?)》""")
            /* ただし丸括弧内の文字はひらがなかカタカナのみを指定できます。 */
            .replaceRuby("""([一-龠々ヶ]+)（([ぁ-んァ-ヶ]+?)）""")
            .replaceRuby("""([一-龠々ヶ]+)\(([ぁ-んァ-ヶ]+?)\)""")
            /* Pixiv 記法*/
            .replaceRuby("""\[\[rb:(.+?) > (.+?)\]\]""")
            /* でんでんマークダウン 記法*/
            .replaceRuby("""\{(.+?)\|(.+?)\}""")
            /* 括弧を括弧のまま表示したい場合は、括弧の直前に縦棒を入力します。 */
            .replaceParensys("""[\|｜]《(.*?)》""", "《》")
            .replaceParensys("""[\|｜]（(.*?)）""", "（）")
            .replaceParensys("""[\|｜]\((.*?)\)""", "()")
    }

    private fun String.replaceRuby(pattern: String) : String {
        val p = Pattern.compile(pattern)
        var out = this

        while(true){
            val m = p.matcher(out)
            if ( m.find() ){
                val body = m.group(1)
                val ruby = m.group(2)
                out = out.replaceRange(m.start(), m.end(), "<ruby>$body<rt>$ruby</rt></ruby>")
            }else{
                break
            }
        }
        return out
    }

    private fun String.replaceParensys(pattern: String, parensys : String ) : String {
        val p = Pattern.compile(pattern)
        var out = this

        while(true){
            val m = p.matcher(out)
            if ( m.find() ){
                val body = m.group(1)
                out = out.replaceRange(m.start(), m.end(), "${parensys[0]}$body${parensys[1]}")
            }else{
                break
            }
        }
        return out
    }

    private fun String.replaceDot() : String {
        val p = Pattern.compile("""《《(.*?)》》""")
        var out = this

        while(true){
            val m = p.matcher(out)
            if ( m.find() ){
                val body = m.group(1)?.fold(StringBuilder()) { s, char-> s.append("<ruby>$char<rt>・</rt></ruby>") }?.toString()
                out = out.replaceRange(m.start(), m.end(), "$body")
            }else{
                break
            }
        }
        return out
    }


}