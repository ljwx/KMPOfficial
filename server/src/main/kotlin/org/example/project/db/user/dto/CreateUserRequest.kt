package org.example.project.db.user.dto

import kotlinx.serialization.Serializable

/**
 * 创建用户请求 DTO
 * 用于接收客户端创建用户的请求数据
 */
@Serializable
data class CreateUserRequest(
    val username: String,
    val email: String? = null,
    val password: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val phone: String? = null
) {
    /**
     * 验证请求数据的有效性
     * @return 如果验证通过返回 null，否则返回错误消息
     */
    fun validate(): String? {
        // 验证用户名
        if (username.isBlank()) {
            return "用户名不能为空"
        }
        if (username.length < 3 || username.length > 50) {
            return "用户名长度必须在 3-50 个字符之间"
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return "用户名只能包含字母、数字和下划线"
        }
        
        // 验证密码
        if (password.isBlank()) {
            return "密码不能为空"
        }
        if (password.length < 6 || password.length > 100) {
            return "密码长度必须在 6-100 个字符之间"
        }
        
        // 验证邮箱（如果提供）
        email?.let {
            if (it.isNotBlank() && !it.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"))) {
                return "邮箱格式不正确"
            }
        }
        
        // 验证手机号（如果提供）
        phone?.let {
            if (it.isNotBlank() && !it.matches(Regex("^1[3-9]\\d{9}\$"))) {
                return "手机号格式不正确（需要11位中国大陆手机号）"
            }
        }
        
        // 验证昵称（如果提供）
        nickname?.let {
            if (it.length > 50) {
                return "昵称长度不能超过 50 个字符"
            }
        }
        
        return null
    }
}

