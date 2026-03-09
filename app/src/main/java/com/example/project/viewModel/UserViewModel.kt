package com.example.project.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.project.roomDb.User
import kotlinx.coroutines.launch

class UserViewModel(private val repository: Repository): ViewModel() {
    fun getUsers() = repository.getAllUsers().asLiveData(viewModelScope.coroutineContext)

    fun upsertUser(user: User){
        viewModelScope.launch {
            repository.upsertUser(user)
        }
    }
}