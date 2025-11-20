package org.example.project.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class UserAuthViewModel(private val repository: UserAuthRepositoryImpl) : ViewModel() {

    val state = MutableStateFlow<Result<UserInfo>?>(null)

    fun login(username: String, password: String) {
        viewModelScope.launch {
            state.value = repository.login(username, password)
        }
    }

}