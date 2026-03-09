package com.example.project.roomDb

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    val name: String,
    val image: String,
    @PrimaryKey
    val id: Int = 0
)
