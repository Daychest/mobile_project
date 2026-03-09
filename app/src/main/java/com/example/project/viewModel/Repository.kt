package com.example.project.viewModel

import com.example.project.roomDb.User
import com.example.project.roomDb.UserDatabase

class Repository(private val db: UserDatabase) {
    suspend fun upsertUser(user: User) {
        db.dao.upsertUser(user)
    }

    fun getAllUsers() = db.dao.getAllUsers()
}