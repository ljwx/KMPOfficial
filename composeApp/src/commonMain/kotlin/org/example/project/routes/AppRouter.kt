package org.example.project.routes

import kotlinx.serialization.Serializable

@Serializable
object RouterSplash

@Serializable
object RouterMainHome

@Serializable
object RouterPullRefresh

@Serializable
data class Detail(val id: String, val fromSource: String)