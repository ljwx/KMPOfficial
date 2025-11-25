package org.example.project.routes

import kotlinx.serialization.Serializable

interface BaseRouter

@Serializable
object RouterSplash : BaseRouter

@Serializable
object RouterMainHome : BaseRouter

@Serializable
object RouterPullRefresh : BaseRouter

@Serializable
data class RouterProductDetail(val productJson: String) : BaseRouter

@Serializable
data class Detail(val id: String, val fromSource: String) : BaseRouter