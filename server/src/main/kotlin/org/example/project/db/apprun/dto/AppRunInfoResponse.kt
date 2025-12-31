package org.example.project.db.apprun.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppRunInfoResponse(
    val appName: String,
    val runDate: String,
    val startBalance: String?,
    val endBalance: String?,
    val checkIn: Boolean,
    val mainTaskCount: Int
)