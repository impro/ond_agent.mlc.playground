package com.lamrnd.docqa.domain.llm

import ai.mlc.mlcllm.MLCEngine
import ai.mlc.mlcllm.OpenAIProtocol
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//class LLAMAOnDAPI {
class LLAMAOnDAPI(private val context: Context) {

    //private val context: Context = null!!

    //private val generativeModel: GenerativeModel
    private val engine: MLCEngine = MLCEngine()
/*
    init {
        // Here's a good reference on topK, topP and temperature
        // parameters, which are used to control the output of a LLM
        // See
        // https://ivibudh.medium.com/a-guide-to-controlling-llm-model-output-exploring-top-k-top-p-and-temperature-parameters-ed6a31313910
        //val configBuilder = GenerationConfig.Builder()
        //configBuilder.topP = 0.4f
        //configBuilder.temperature = 0.3f
        generativeModel =
            GenerativeModel(
                modelName = "gemini-1.5-flash",
                //apiKey = apiKey,
                //generationConfig = configBuilder.build(),
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
*/

    suspend fun generateResponse(prompt: String, shouldProcess: Boolean = true): String? = withContext(Dispatchers.IO) {
        val inputPrompt = """
        Convert the following natural language query to a Gmail search query:
        "$prompt"
        
        Gmail search query should use tags like 'subject:'
        make sure only subject keywords are used
    """.trimIndent()

        // 메시지 초기화 및 입력 프롬프트 추가
        val historyMessages = mutableListOf(
            OpenAIProtocol.ChatCompletionMessage(
                //role = OpenAIProtocol.ChatCompletionRole.user,
                role = OpenAIProtocol.ChatCompletionRole.assistant,  // 필요에 따라 지원되는 역할로 변경
                content = inputPrompt
            )
        )

        Log.d("LLAMAOnDAPI", "Prompt given to on-device MLC-LLM: $inputPrompt")
        try {
            val responses = engine.chat.completions.create(
                messages = historyMessages,
                stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
            )
            Log.d("LLAMAOnDAPI", "Received on-device LLM response: $responses")
            var streamingText = ""
            var finishReasonLength = false

            // 응답을 순차적으로 처리
            for (res in responses) {
                for (choice in res.choices) {
                    choice.delta.content?.let { content ->
                        streamingText += content.asText()
                    }
                    choice.finish_reason?.let { finishReason ->
                        if (finishReason == "length") {
                            finishReasonLength = true
                        }
                    }
                }
                Log.d("LLAMAOnDAPI", "Streaming text: $streamingText")
                if (finishReasonLength) {
                    streamingText += " [output truncated due to context length limit...]"
                }
            }

            // shouldProcess 파라미터에 따라 프로세싱 여부 결정
            return@withContext if (shouldProcess) {
                Log.d("LLAMAOnDAPI", "Processing on-device LLM response...")
                processStreamingText(streamingText)

            } else {
                Log.d("LLAMAOnDAPI", "Returning raw on-device LLM response...")
                streamingText
            }

        } catch (e: Exception) {
            Log.e("LLAMAOnDAPI", "Error occurred during on-device LLM generation: ${e.message}")
            return@withContext null
        }
    }


    // 쿼리 추출 및 로깅을 수행하는 함수
    private fun processStreamingText(streamingText: String): String {
        val searchQuery = extractSearchQuery(streamingText)
        Log.d("LLAMAOnDAPI", "Generated search query: $searchQuery")
        return searchQuery
    }


    // 검색 쿼리를 정리하는 함수
    private fun extractSearchQuery(text: String): String {
        val pattern = Regex("subject:[^\\s]+")
        val matchResult = pattern.find(text)
        return matchResult?.groupValues?.get(0)?.trim() ?: ""
    }
}
