package jp.gr.aqua.vjap

data class Ruby( val bodyStart1 : String ,
                 val bodyStart2 : String? ,
                  val rubyStart : String ,
                  val rubyEnd : String ,
                 val pattern : String,
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
                "aozora" to Ruby("|","｜","《", "》", "｜.+《.+》" ,aozora = true),  //青空文庫ルビ
                "bccks" to Ruby("{",null,"}(",")","\\{.+\\}\\(.+\\)"),      //BCCKS
                "denden" to Ruby( "{",null,"|","}", "\\{.+\\|.+\\}"),     //でんでんマークダウン
                "pixiv" to Ruby( "[[rb:",null," > ","]]", "\\[\\[rb\\:.+ > .+\\]\\]"),   //pixiv
                "html" to Ruby( "<ruby>",null,"<rt>","</rt></ruby>", "<ruby>.+<rt>.+</rt></ruby>") //HTML5
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
