package jp.gr.aqua.vjap

import java.util.*

data class Line(
        val line : ArrayList<VChar>,
        val index : Int,
        val broken : Boolean,
        val next : Int
)