package com.studybuddy.app.data.model

/** Flat POJO returned by the GROUP BY document-summary query in KnowledgeChunkDao. */
data class DocumentSummaryTuple(
    val sourceImageUri: String,
    val subjectId: Long?,
    val subjectName: String,
    val chapterId: Long?,
    val chapterName: String,
    val textPreview: String,
    val chunkCount: Int,
    val createdAt: Long
)
