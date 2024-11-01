package com.lamrnd.docqa.domain.llm

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
//import com.lamrnd.docqa.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties


//val geminiRemoteAPI = GeminiRemoteAPI(File(context.filesDir.parentFile, "../"))
//val geminiRemoteAPI = GeminiRemoteAPI(requireContext()) // Fragment의 경우
// 또는
//val geminiRemoteAPI = GeminiRemoteAPI(this) // Activity의 경우
// val geminiRemoteAPI = GeminiRemoteAPI(applicationContext) // Application context 사용

fun loadPropertiesFromAssets(context: Context): Properties {
    val properties = Properties()

    try {
        context.assets.open("local.properties").use { inputStream ->
            properties.load(inputStream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return properties
}

//class GeminiRemoteAPI {
class GeminiRemoteAPI(private val context: Context) {
    //private val context: Context = null!!
    //private val apiKey = BuildConfig.geminiKey
    /*
    private val apiKey: String by lazy {
        val properties = Properties()
        //val localPropertiesFile = File(rootDir, "local.properties")
        val localPropertiesFile = File(context.filesDir.parentFile, "../local.properties")

        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        properties.getProperty("geminiKey") ?: error("geminiKey가 local.properties 파일에 정의되지 않았습니다.")
    }
    */
    private val apiKey: String by lazy {
        val properties = loadPropertiesFromAssets(context)
        properties.getProperty("geminiKey") ?: error("geminiKey가 local.properties 파일에 정의되지 않았습니다.")
    }

    private val generativeModel: GenerativeModel

    init {
        // Here's a good reference on topK, topP and temperature
        // parameters, which are used to control the output of a LLM
        // See
        // https://ivibudh.medium.com/a-guide-to-controlling-llm-model-output-exploring-top-k-top-p-and-temperature-parameters-ed6a31313910
        val configBuilder = GenerationConfig.Builder()
        configBuilder.topP = 0.4f
        configBuilder.temperature = 0.3f
        Log.d("GeminiRemoteAPI", "apiKey: $apiKey")
        generativeModel =
            GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                generationConfig = configBuilder.build(),
                systemInstruction = content {
                    text("You are an intelligent search engine. You will be provided with some retrieved context, as well as the users query. Your job is to understand the request, and answer based on the retrieved context.")
                }
            )
    }

    suspend fun getResponse(prompt: String): String? =
        withContext(Dispatchers.IO) {
            Log.e("APP", "Prompt given: $prompt")
            val response = generativeModel.generateContent(prompt)
            return@withContext response.text
        }
}
