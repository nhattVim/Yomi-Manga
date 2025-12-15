package com.example.yomi_manga.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "downloaded_manga")
data class DownloadedManga(
    @PrimaryKey
    val mangaId: String,
    val title: String,
    val slug: String,
    val coverUrl: String,
    val author: String?,
    val lastDownloaded: Long
)

@Entity(tableName = "downloaded_chapters")
data class DownloadedChapter(
    @PrimaryKey
    val chapterId: String,
    val mangaId: String,
    val chapterTitle: String,
    val chapterNumber: Float,
    val downloadDate: Long,
    val imagePaths: List<String>
)

data class MangaAndChapters(
    @Embedded val manga: DownloadedManga,
    @Relation(
        parentColumn = "mangaId",
        entityColumn = "mangaId"
    )
    val chapters: List<DownloadedChapter>
)
