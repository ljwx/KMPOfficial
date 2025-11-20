package org.example.project.feature.user

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val id: Long?,
    val username: String,
    val email: String?,
    val password: String?,
    val nickname: String?,
    val avatar: String?,
    val phone: String?,
    val status: String?,
    val role: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val lastLoginAt: String?
)