package org.example.project.feature.user

class UserAuthRepositoryImpl : UserAuthRepository {

    override fun login(username: String, password: String): Result<UserInfo> {
        return Result.failure(Exception("假装请求了"))
    }

}