package com.studybuddy.app.di

import android.content.Context
import androidx.room.Room
import com.studybuddy.app.data.local.AppDatabase
import com.studybuddy.app.data.local.dao.ChapterDao
import com.studybuddy.app.data.local.dao.ExamDao
import com.studybuddy.app.data.local.dao.MessageDao
import com.studybuddy.app.data.local.dao.StudySessionDao
import com.studybuddy.app.data.local.dao.SubjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "studybuddy.db").build()

    @Provides fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()
    @Provides fun provideSubjectDao(db: AppDatabase): SubjectDao = db.subjectDao()
    @Provides fun provideChapterDao(db: AppDatabase): ChapterDao = db.chapterDao()
    @Provides fun provideExamDao(db: AppDatabase): ExamDao = db.examDao()
    @Provides fun provideStudySessionDao(db: AppDatabase): StudySessionDao = db.studySessionDao()
}
