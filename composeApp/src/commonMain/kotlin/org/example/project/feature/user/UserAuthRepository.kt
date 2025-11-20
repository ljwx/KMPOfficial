package org.example.project.feature.user

interface UserAuthRepository {

    fun login(username: String, password: String): Result<UserInfo>

}