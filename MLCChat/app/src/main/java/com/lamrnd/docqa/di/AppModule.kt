package com.lamrnd.docqa.di

import android.app.Activity
import android.app.Application
import android.content.Context
import com.lamrnd.docqa.GmailHelper
import com.lamrnd.docqa.data.ChunksDB
import com.lamrnd.docqa.data.DocumentsDB
import com.lamrnd.docqa.domain.DocumentsUseCase
import com.lamrnd.docqa.domain.embeddings.SentenceEmbeddingProvider
import com.lamrnd.docqa.domain.llm.GeminiRemoteAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//import com.lamrnd.docqa.domain.llm.InferenceModel
//import com.lamrnd.docqa.GmailHelper


// AppModule provides dependencies that are to be injected by Hilt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // SingletonComponent ensures that instances survive
    // across the application's lifespan
    // @Singleton creates a single instance in the app's lifespan

    @Provides
    @Singleton
    fun provideDocumentsDB(): DocumentsDB {
        return DocumentsDB()
    }

    @Provides
    @Singleton
    fun provideChunksDB(): ChunksDB {
        return ChunksDB()
    }

    @Provides
    @Singleton
    //fun provideGeminiRemoteAPI(context: Application): GeminiRemoteAPI {
    //    return GeminiRemoteAPI(context)
    //}
    fun provideGeminiRemoteAPI(): GeminiRemoteAPI {
        return GeminiRemoteAPI()
    }
//    private val gmailHelper: GmailHelper

//    @Provides
//    @Singleton
//    fun provideGmailHelperAPI(): GeminiRemoteAPI {
//        return GmailHelper(
//    }
@Provides
//    fun provideGmailHelper(activity: Activity, documentsUseCase: DocumentsUseCase): GmailHelper {
//        val gmailHelper = GmailHelper(documentsUseCase)
//        gmailHelper.setActivity(activity) // Manually set the activity
//        return gmailHelper
//    }
    fun provideGmailHelper(
    @ActivityContext context: Context,
    documentsUseCase: DocumentsUseCase
    ): GmailHelper {
        return GmailHelper(
            activity = context as Activity,
            documentsUseCase = documentsUseCase
            // We cannot provide accountPickerLauncher here
        )
    }
    @Provides
    @Singleton
    fun provideSentenceEncoder(context: Application): SentenceEmbeddingProvider {
        return SentenceEmbeddingProvider(context)
    }

    //@Provides
    //fun provideInferenceModel(context: Application): InferenceModel {
        // InferenceModel의 의존성이 있다면 여기서 초기화합니다.
    //    return InferenceModel(context)
    //}
}
