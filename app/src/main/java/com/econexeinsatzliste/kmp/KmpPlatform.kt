package com.econexeinsatzliste.kmp

interface KmpPlatform {
    val name: String
    val isIos: Boolean
}

object AndroidKmpPlatform : KmpPlatform {
    override val name: String = "Android"
    override val isIos: Boolean = false
}

object IosKmpPlatform : KmpPlatform {
    override val name: String = "iOS"
    override val isIos: Boolean = true
}
