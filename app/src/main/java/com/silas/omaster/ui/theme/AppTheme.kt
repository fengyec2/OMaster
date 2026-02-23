package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

enum class BrandTheme(
    val id: String,
    val brandName: String,
    val colorName: String,
    val primaryColor: Color,
    val hexCode: String
) {
    Hasselblad("hasselblad", "哈苏 (Hasselblad)", "哈苏橙", HasselbladOrange, "#FF6600"),
    Zeiss("zeiss", "蔡司 (ZEISS)", "蔡司蓝", ZeissBlue, "#005A9C"),
    Leica("leica", "徕卡 (Leica)", "徕卡红", LeicaRed, "#CC0000"),
    Ricoh("ricoh", "理光 (Ricoh)", "理光绿", RicohGreen, "#00A95C"),
    Fujifilm("fujifilm", "富士 (Fujifilm)", "富士绿", FujifilmGreen, "#009B3A"),
    Canon("canon", "佳能 (Canon)", "佳能红", CanonRed, "#CC0000"),
    Nikon("nikon", "尼康 (Nikon)", "尼康黄", NikonYellow, "#FFC20E"),
    Sony("sony", "索尼 (Sony)", "索尼橙", SonyOrange, "#F15A24"),
    PhaseOne("phaseone", "飞思 (Phase One)", "飞思灰", PhaseOneGrey, "#5A5A5A");

    companion object {
        fun fromId(id: String): BrandTheme {
            return entries.find { it.id == id } ?: Hasselblad
        }
    }
}
