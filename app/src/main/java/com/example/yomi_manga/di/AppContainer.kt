package com.example.yomi_manga.di

import com.example.yomi_manga.data.api.ApiClient
import com.example.yomi_manga.data.api.OtruyenApiService
import com.example.yomi_manga.data.repository.AuthRepository
import com.example.yomi_manga.data.repository.MangaRepository
import com.example.yomi_manga.data.repository.UserRepository

object AppContainer {
    val apiService: OtruyenApiService by lazy { ApiClient.apiService }
    
    val userRepository: UserRepository by lazy { UserRepository() }
    val authRepository: AuthRepository by lazy { AuthRepository(userRepository) }
    val mangaRepository: MangaRepository by lazy { MangaRepository() }
}

