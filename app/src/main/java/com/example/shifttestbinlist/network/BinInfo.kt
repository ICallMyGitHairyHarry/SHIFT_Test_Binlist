package com.example.shifttestbinlist.network

data class BinInfo (
    var id: String?,
    val number: CardNumber?,
    val scheme: String?,
    val type: String?,
    val brand: String?,
    val prepaid: Boolean?,
    val country: CardCountry?,
    val bank: Map<String, String>?
)
