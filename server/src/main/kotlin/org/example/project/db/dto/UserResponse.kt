package org.example.project.db.dto

import kotlinx.serialization.Serializable

/**
 * 用户响应 DTO
 * 用于返回用户信息，不包含敏感信息（如密码）
 */
@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val phone: String? = null,
    val status: String,
    val role: String,
    val createdAt: String,
    val updatedAt: String,
    val lastLoginAt: String? = null
)

