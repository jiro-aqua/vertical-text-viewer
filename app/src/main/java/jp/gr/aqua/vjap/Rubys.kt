package jp.gr.aqua.vjap

data class Ruby( val bodyStart1 : String ,
                 val bodyStart2 : String? ,
                  val rubyStart : String ,
                  val rubyEnd : String ,
                 val aozora : Boolean = false) {

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
    private val RUBYS = mapOf<String,Ruby>(
            "aozora" to Ruby("|","｜","《", "》",true),  //青空文庫ルビ
            "bccks" to Ruby("{",null,"}(",")"),      //BCCKS
            "denden" to Ruby( "{",null,"|","}"),     //でんでんマークダウン
            "pixiv" to Ruby( "[[rb:",null," > ","]]"),   //pixiv
            "html" to Ruby( "<ruby>",null,"<rt>","</rt></ruby>") //HTML5
    )

    fun getRuby() = RUBYS[mode] ?: RUBYS["aozora"]!!
}
