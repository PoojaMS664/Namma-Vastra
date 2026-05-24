package com.nammavastra.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeaverProfile(
    val id: String? = null,
    val name: String? = null,
    val whatsapp: String? = null,
    val village: String? = null,
    val category: String? = "Ilkal",
    val role: String? = "Weaver"
)

@Serializable
data class Saree(
    val id: String? = null,
    val title: String? = null,
    val origin: String? = null,
    val price: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("weaver_id")
    val weaverId: String? = null,
    @SerialName("weaver_name")
    val weaverName: String? = null,
    @SerialName("weaver_whatsapp")
    val weaverWhatsapp: String? = null,
    val timestamp: Long? = null,
    val palette: List<String>? = null
)

@Serializable
data class Trend(
    val id: String? = null,
    val title: String? = null,
    val city: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val colors: List<String>? = null
)
