package jp.gr.aqua.vjap

data class Ruby( val bodyStart1 : String ,
                 val bodyStart2 : String? ,
                  val rubyStart : String ,
                  val rubyEnd : String ,
                 val pattern : String,
                 val isRuby : (Char)->Boolean,
                 val dotStart :String? = null,
                 val dotEnd :String? = null,
                 val aozora : Boolean = false
                 ) {

    fun isRubyMarkup(str:String) : Boolean {
        return when(str) {
            bodyStart1 -> true
            bodyStart2 -> true
            rubyEnd -> true
            rubyStart -> true
            else -> false
        }
    }
}

class Rubys( val mode : String ) {

    fun getRuby() = RUBYS[mode] ?: RUBYS["aozora"]!!

    companion object {

        private val RUBYS = mapOf(
                "aozora" to Ruby("|","｜","《", "》", "｜.+《.+》" ,isRuby = {ch -> ch=='｜' || ch=='|'||ch=='《'||ch=='》'} , aozora = true, dotStart = "《《", dotEnd = "》》"),  //青空文庫ルビ
                "bccks" to Ruby("{",null,"}(",")","\\{.+\\}\\(.+\\)",isRuby = {ch -> ch=='{'||ch=='}'||ch==')'}),      //BCCKS
                "denden" to Ruby( "{",null,"|","}", "\\{.+\\|.+\\}",isRuby = {ch -> ch=='{'||ch=='}'||ch=='|'}),     //でんでんマークダウン
                "pixiv" to Ruby( "[[rb:",null," > ","]]", "\\[\\[rb\\:.+ > .+\\]\\]",isRuby = {ch -> ch=='['||ch==' '||ch==']'}),   //pixiv
                "html" to Ruby( "<ruby>",null,"<rt>","</rt></ruby>", "<ruby>.+<rt>.+</rt></ruby>",isRuby = {ch -> ch=='<'}) //HTML5
        )

        fun detectRubyMode( text:String ) : String {
            RUBYS.forEach {
                val regex = Regex(it.value.pattern)
                if ( text.contains(regex) ){
                    return it.key
                }
            }
            return "aozora"
        }

    }

}
