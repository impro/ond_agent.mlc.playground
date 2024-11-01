package com.lamrnd.docqa.domain

import android.util.Log
import com.lamrnd.docqa.GmailHelper
import com.lamrnd.docqa.data.QueryResult
import com.lamrnd.docqa.data.OnDeviceQueryResult
import com.lamrnd.docqa.data.RetrievedContext
import com.lamrnd.docqa.domain.llm.GeminiRemoteAPI
//import com.lamrnd.docqa.domain.llm.InferenceModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectIndexed
import java.util.concurrent.atomic.AtomicInteger

@Singleton
class QAUseCase
@Inject
constructor(
    val documentsUseCase: DocumentsUseCase,
    private val chunksUseCase: ChunksUseCase,
    //private val ond_chunksUseCase: ChunksUseCase,
    private val geminiRemoteAPI: GeminiRemoteAPI,
    //private val inferenceModel: InferenceModel,
    //private val gmailHelper: GmailHelper
    ) {
    private lateinit var gmailHelper: GmailHelper

    //private suspend fun buildGmailQueryWithLLM(queryText: String, llmService: LLMService): String {
    //suspend fun buildGmailQueryWithLLM(queryText: String, onResponse: (QueryResult) -> Unit ){
    fun buildGmailQueryWithLLM(queryText: String, onResponse: (QueryResult) -> Unit ){
        // LLM을 통해 자연어를 분석하고 Gmail 검색 태그로 변환
        val inputPrompt = """
        Convert the following natural language query into a Gmail search query:
        "$queryText"
        
        Gmail search query should use tags like 'from:', 'to:', 'subject:', 'has:attachment', etc.
    """.trimIndent()

        val retrievedContextList = ArrayList<RetrievedContext>()

        // LLM 서비스 호출하여 변환된 쿼리 받기
        CoroutineScope(Dispatchers.IO).launch {
            geminiRemoteAPI.getResponse(inputPrompt)?.let { llmResponse ->
                onResponse(QueryResult(llmResponse, retrievedContextList))
            }
        }
        //val response = llmService.query(prompt)
        //val generatedQuery = response.trim()

        // 변환된 쿼리 출력 및 반환
        //Log.d("LLMQuery", "Generated Gmail Query: $generatedQuery")
        //return generatedQuery
    }
    //@Inject
    //lateinit var documentsUseCase: DocumentsUseCase // DocumentsUseCase를 주입받음

    // allResponses 리스트를 출력하는 함수
    //fun printAllResponses(allResponses: List<List<String>>) {
        // allResponses 리스트의 각 응답 리스트를 순회하면서 출력
    //    allResponses.forEachIndexed { responseIndex, responseList ->
    //        Log.d("QAUseCase : printAllResponses : ", "Response Set #$responseIndex:")
    //        responseList.forEachIndexed { index, response ->
    //            Log.d("QAUseCase : printAllResponses : ", "  - Partial Result #$index: $response")
    //        }
    //    }
    //}
    // allResponses가 List<String> 타입에 맞도록 함수 수정
    fun printAllResponses(allResponses: List<String>) {
        // allResponses 리스트의 각 응답을 순회하면서 출력
        allResponses.forEachIndexed { index, response ->
            Log.d("QAUseCase : printAllResponses", "Partial Result #$index: $response")
        }
    }
    fun getAnswer(query: String, prompt: String, onResponse: (QueryResult) -> Unit , onOnDeviceResponse: (OnDeviceQueryResult) -> Unit) {
        var jointContext = ""
        val retrievedContextList = ArrayList<RetrievedContext>()
        //chunksUseCase.getSimilarChunks(query, n = 5).forEach {
        chunksUseCase.getSimilarChunks(query, n = 5).forEach {
            jointContext += " " + it.second.chunkData
            retrievedContextList.add(RetrievedContext(it.second.docFileName, it.second.chunkData))
        }
        Log.d("APP", "OnSite Context: $jointContext")

        val inputPrompt = prompt.replace("\$CONTEXT", jointContext).replace("\$QUERY", query)
        Log.d("APP", "inputPrompt: $inputPrompt")

        CoroutineScope(Dispatchers.IO).launch {
            geminiRemoteAPI.getResponse(inputPrompt)?.let { llmResponse ->
                onResponse(QueryResult(llmResponse, retrievedContextList))
            }
        }
        //-----------------------------------------------------------------------------------------
        var ond_jointContext = ""
        //prompt_ond = f"Here is the retrieved context, $CONTEXT, and here is the user\'s query, $QUERY."
        //val prompt_ond = "Here is the retrieved context, ---- CONTEXT :  \$CONTEXT, --- the user's query, \$QUERY. Please provide a detailed and comprehensive answer to the user's query based on the retrieved context."
        //val prompt_ond = "Here is the retrieved context, ---- CONTEXT : Ferret UI is an advanced AI-powered user interface system designed for visual understanding and interaction. Multimodal Interaction: Ferret UI allows users to interact using both text and images simultaneously. Users can input text commands and upload or reference images within the same interface. --- the user's query, \$QUERY. Please provide a detailed and comprehensive answer to the user's query based on the retrieved context."
        //var CONTEXT = " Ferret UI is an advanced AI-powered user interface system designed for visual understanding and interaction. Multimodal Interaction: Ferret UI allows users to interact using both text and images simultaneously. Users can input text commands and upload or reference images within the same interface."
        //val prompt_ond = "Please provide a answer to the user's query based on the retrieved context. Here is the retrieved context, ---- CONTEXT : \$CONTEXT --- the user's query, \$QUERY. Please provide a detailed and comprehensive answer to the user's query based on the retrieved context."

        //var CONTEXT = "UI demonstrates remarkable proficiency in referring, grounding, and reasoning. The advent of these enhanced capabilities promises substantial advancements for a multitude of downstream UI applications, thereby amplifying the potential benefits afforded by Ferret-UI in this domain. References 1. Achiam, J., Adler, S., Agarwal, S., Ahmad, L., Akkaya, I., Aleman, F.L., Almeida, D., Altenschmidt, J., Altman, S., Anadkat, S., et al.:"
        //var CONTEXT = "encompass a diverse range of basic and advanced UI tasks, Ferret-UI demonstrates remarkable proficiency in referring, grounding, and reasoning. The advent of these enhanced capabilities promises substantial advancements for a multitude of downstream UI applications, thereby amplifying the potential benefits afforded by Ferret-UI in this domain. References 1. Achiam, J., Adler, S., Agarwal, S., Ahmad, L., Akkaya, I., Aleman, F.L., Almeida, D., Altenschmidt, J., Altman, S., Anadkat, S., et al.: [8,26,41,53,56,57,59]. 3 Method Ferret-UI is built upon Ferret [53], which is a MLLM that excells in spatial referring and grounding within natural images of diverse shapes and levels of detail. It can interpret and interact with regions or objects, whether they are specified as points, boxes, or any free-form shapes. Ferret contains a pre-trained visual encoder (e.g., CLIP-ViT-L/14) [42] and a decoder-only language model (e.g., Vicuna [61]). Furthermore, Ferret incorporates a unique hybrid screen data. Specifically, Ferret-UI includes a broad range of UI referring tasks (e.g., OCR, icon recognition, widget classification) and grounding tasks (e.g., find text/icon/widget, widget listing) for model training, building up a strong UI understanding foundation for advanced UI interactions. Unlike previous MLLMs that require external detection modules or screen view files, Ferret-UI is self-sufficient, taking raw screen pixels as model input. This approach not only facilitates that Ferret-UI significantly surpasses the base Ferret model, illustrating the importance of domain-specific model training. Compared to GPT-4V, Ferret-UI demonstrates superior performance in elementary UI tasks. Notably, in the context of advanced tasks, Ferret-UI surpasses both Fuyu and CogAgent. Our contributions are summarized as follows. (i) We propose Ferret-UI with “any-resolution” (anyres) integrated to flexibly accommodate various screen aspect ratios. It represents the first LLMs [12,18,25,62] and MLLMs [4,9,15,43,49,54,60] in the space. Ferret-UI: Grounded Mobile UI Understanding with Multimodal LLMs 5 Fig.2: Overview of Ferret-UI-anyres architecture. While Ferret-UI-base closely follows Ferret’s architecture, Ferret-UI-anyres incorporates additional fine-grained image features. Particularly, a pre-trained image encoder and projection layer produce image features for the entire screen. For each sub-image obtained based on the original image aspect ratio,"
        //var CONTEXT = " Ferret-UI demonstrates remarkable proficiency in referring, grounding, and reasoning. The advent of these enhanced capabilities promises substantial advancements for a multitude of downstream UI applications, thereby amplifying the potential benefits afforded by Ferret-UI in this domain. Ferret-UI is built upon Ferret.  3 Method Ferret-UI is built upon Ferret, which is a MLLM that excells in spatial referring and grounding within natural images of diverse shapes and levels of detail. "
        //var CONTEXT = "   Ferret-UI: Grounded Mobile UI Understanding with Multimodal LLMs 5\n" +
        //        "                                                                                                    Fig.2: Overview of Ferret-UI-anyres architecture. While Ferret-UI-base closely follows\n" +
        //        "                                                                                                    Ferret’s architecture, Ferret-UI-anyres incorporates additional fine-grained image fea-\n" +
        //        "                                                                                                    tures. Particularly, a pre-trained image encoder and projection layer produce image\n" +
        //        "                                                                                                    features for the entire screen. For each sub-image obtained based on the original image\n" +
        //        "                                                                                                    aspect ratio"
        //val prompt_ond = "Please provide a answer to the user's query based on the retrieved context " + prompt
        //val prompt_ond = "make sure answer to the user's query based on the retrieved context " + prompt

        //val ond_retrievedContextList = ArrayList<RetrievedContext>()

        // 각 반복에서 수집된 부분 결과의 개수를 저장할 리스트
        //val collectedResultsCounts = MutableList(3) { 0 }
        val collectedResultsCounts = MutableList(1) { 0 }

        // 각 반복이 완료되었는지 나타내는 플래그 리스트
        //val iterationCompleted = MutableList(3) { false }
        val iterationCompleted = MutableList(1) { false }
        // 완료된 반복 횟수를 세는 카운터
        var completedIterations = 0

        // 최종 결과들을 저장할 리스트 선언
        //val allResponses = mutableListOf<List<String>>() // 각 쿼리의 결과를 담는 리스트
        val allResponses = mutableListOf<String>() // 각 쿼리의 W결과를 담는 리스트
        val responseIndex = AtomicInteger(0) // AtomicInteger 사용하여 응답 인덱스 선언

        // Function to update the UI
        fun updateUIWithResult(result: String) {
            // Replace this with your actual UI state handling logic (e.g., setting LiveData or State)
            Log.d("OND_QAUseCase", "On Device Result: $result")
            // Example: _uiState.value = UIState.NoValidResult(result)
        }

        //ond_chunksUseCase.getSimilarChunks(query, n = 3).forEach {
        //ond_chunksUseCase.getSimilarChunks(query, n = 1).forEach {
        chunksUseCase.getSimilarChunks(query, n = 1).forEach {
            ond_jointContext =  it.second.chunkData
            //ond_jointContext += " " + it.second.chunkData
        }
        //it.second.chunkData
        //ond_retrievedContextList.add(RetrievedContext(it.second.docFileName, it.second.chunkData))
        Log.d("OND_APP", "OnD Context1: $ond_jointContext")
        var ond_inputPrompt = prompt.replace("\$CONTEXT", ond_jointContext).replace("\$QUERY", query)
        Log.d("OND_APP", "ond_inputPrompt: $ond_inputPrompt")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Start generating the response asynchronously
                //ond_inputPrompt = "(answer with korean) Here is the user's query: What is purpose of workshop?  The purpose of the workshop is to: Strengthen the network among mentors: This will help build a collaborative system for ongoing support. Identify mentor challenges: The workshop will provide a space for mentors to share their difficulties and identify areas where they need support. Increase the success rate of research projects: By gathering ideas and insights from mentors, the workshop aims to improve the chances of successful project outcomes. "
                //ond_inputPrompt = """ Here is the retrieved context: Fellowship 프로그램에 멘토로 참여해주셔서 감사합니다. 여러분의 과제 전문성과 Fellow에 대한 애정으로 연구과제가 진도를 나가고 있습니다. 6월에 프로그램이 런칭된 이후 7월 인재개발원 Networking Camp, 8월 중간점검 발표에 이어 9월에 AI Fellowship 멘토 대상 워크샵을 준비하고 있습니다. 이번 워크샵은 멘토들 간의 멘토링 노하우와 팁을 공유하고, 상호 간의 네트워크를 강화하여 멘토링 연구과제의 성공 가능성을 높이기 위한 소중한 기회가 될 것입니다. Here is the user's query: What is the purpose of the workshop? """
                //ond_jointContext = " Here is the user's query: What is purpose of workshop?  The purpose of the workshop is to: Strengthen the network among mentors"
                //ond_inputPrompt = " Here is the user's query: What is purpose of workshop?  The purpose of the workshop is to: Strengthen the network among mentors"
                //ond_inputPrompt = """Here is the retrieved context -------------------------------------------------- Fellowship 프로그램에 멘토로 참여해주셔서 감사합니다. 여러분의 과제 전문성과 Fellow에 대한 애정으로 연구과제가 진도를 나가고 있습니다.
                //                                                                                6월에 프로그램이 런칭된 이후 7월 인재개발원 Networking Camp, 8월 중간점검 발표에 이어 9월에 AI Fellowship 멘토 대상 워크샵을 준비하고 있습니다.
                //                                                                                  *  -------------------------------------------------- Here is the user's query: What is purpose of workshop? """
//                inferenceModel.generateResponseAsync(ond_inputPrompt)
                Log.d("OND_QAUseCase", "Generating response for prompt: $ond_inputPrompt")

                // 각 쿼리의 부분 결과를 저장할 리스트
                val collectedResults = mutableListOf<String>()
                //var responseIndex = 0 // 별도의 카운터 추가

                var stopCollection = false

                // Collect partial results and handle them
            /*
                inferenceModel.partialResults.collect { (partialResult, done) ->
                    if (stopCollection) return@collect
                    // Define a condition to detect the repeated <bos> token pattern
                    Log.d("partialResult : ", partialResult)
                    if (partialResult.contains("<bos><bos><bos>")) {
                        Log.d("OND_QAUseCase", "Stopping due to repeated <bos> tokens.")
                        // Set the UI to show "No Valid Result" on the device
                        updateUIWithResult("No Valid Result")
                        //chatViewModel.handleInvalidResponse()  // Add "No Valid Result" to the chat
                        stopCollection = true
                        return@collect // Stop the current collection iteration
                    }

                    collectedResults.add(partialResult)
                    // 각 반복에서 수집된 부분 결과의 개수 증가
                    if (done) {
                        Log.d("---- DONE ----- OND_QAUseCase", "----DONE---- : Partial result #$responseIndex: $partialResult (Done: $done)")
                        val currentIndex = responseIndex.getAndIncrement() // 인덱스를 안전하게 증가시키고 가져옴

                        // Print all collected results
                        val fullResponse = collectedResults.joinToString(separator = "\n")
                        Log.d("QAUseCase", "Full response:\n$fullResponse")

                        // 전체 결과를 최종 배열에 추가
                        allResponses.addAll(collectedResults)

                        // Perform any cleanup or UI updates once the response is fully generated
                        Log.d("QAUseCase", "Response generation completed.")
                        // 응답이 완료된 후 전체 allResponses를 출력
                        printAllResponses(allResponses)

                        Log.d("OND_QAUseCase", "DONE : Response generation completed. currentIndex: $currentIndex")
                        //iterationCompleted.size
                        Log.d("OND_QAUseCase", "DONE : Iteration completed size: ${iterationCompleted.size}")

                                // OnDeviceQueryResult 생성 (재질의 결과를 포함)
                                val onDeviceQueryResult = OnDeviceQueryResult(
                                    responses = allResponses.toString(), // 수집된 모든 결과를 리스트로 전달
                                    context = retrievedContextList // 관련 문맥 리스트 전달
                                )
                                Log.d("OND_QAUseCase", "OnDeviceQueryResult: $onDeviceQueryResult")
                                // 결과를 처리하는 콜백 호출
                                onOnDeviceResponse(onDeviceQueryResult)

                                // allResponses 및 collectedResultsCounts, iterationCompleted 초기화
                                allResponses.clear()
                                collectedResultsCounts.fill(0)
                                iterationCompleted.fill(false)
                                completedIterations = 0
                        }

                    }
            */
                } catch (e: Exception) {
                    Log.e("QAUseCase", "Error generating response: ${e.message}", e)
                    // Handle errors, such as displaying a message to the user or logging
                }
            }
        }

/*
        //ond_jointContext = CONTEXT
        //Log.d("OND_APP", "OnD Context2: $ond_jointContext")
        //val ond_inputPrompt = prompt_ond.replace("\$CONTEXT", ond_jointContext).replace("\$QUERY", query)
        val ond_inputPrompt = prompt.replace("\$CONTEXT", ond_jointContext).replace("\$QUERY", query)
        Log.d("OND_APP", "ond_inputPrompt: $ond_inputPrompt")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Start generating the response asynchronously
                inferenceModel.generateResponseAsync(ond_inputPrompt)
                Log.d("OND_QAUseCase", "Generating response for prompt: $ond_inputPrompt")

                val collectedResults = mutableListOf<String>()

                // Collect partial results and handle them
                inferenceModel.partialResults.collectIndexed { index, (partialResult, done) ->
                    // Simulated handling of UI state updates; adjust as needed based on your actual implementation
                    // Here, `_uiState` refers to your UI state management object (e.g., ViewModel or LiveData)
                    Log.d("OND_QAUseCase", "Partial result #$index: $partialResult (Done: $done)")


                    // Collect each partial result
                    collectedResults.add(partialResult)

                    // Handle when done is true (response generation is complete)
                    if (done) {
                        // Print all collected results
                        val fullResponse = collectedResults.joinToString(separator = "\n")
                        Log.d("QAUseCase", "Full response:\n$fullResponse")

                        // Perform any cleanup or UI updates once the response is fully generated
                        Log.d("QAUseCase", "Response generation completed.")
                    }


                    // Example handling, adjust based on how you want to handle the result in your UI
                    if (index == 0) {
                        // Handle the first partial result
                        // Update your UI state with the first message
                    } else {
                        // Append the partial result to the existing message
                        // Update your UI state accordingly
                    }

                    // If the response generation is complete, reset states or re-enable inputs
                    if (done) {
                        // Perform any cleanup or UI updates once the response is fully generated
                        Log.d("QAUseCase", "Response generation completed.")
                    }
                }
            } catch (e: Exception) {
                Log.e("QAUseCase", "Error generating response: ${e.message}", e)
                // Handle errors, such as displaying a message to the user or logging
            }
 */
 //       }
        /*
            //val fullPrompt = _uiState.value.fullPrompt
            inferenceModel.generateResponseAsync(inputPrompt)
            inferenceModel.partialResults
                .collectIndexed { index, (partialResult, done) ->
                    currentMessageId?.let {
                        if (index == 0) {
                            _uiState.value.appendFirstMessage(it, partialResult)
                        } else {
                            _uiState.value.appendMessage(it, partialResult, done)
                        }
                        if (done) {
                            currentMessageId = null
                            // Re-enable text input
                            setInputEnabled(true)
                        }
                    }
                }
            '''

        }

         */


//    }

    fun canGenerateAnswers(): Boolean {
        return documentsUseCase.getDocsCount() > 0
    }

    fun getAnswer_with_embedding(
        query: String,
        prompt: String,
        onResponse: (QueryResult) -> Unit,
        onOnDeviceResponse: (OnDeviceQueryResult) -> Unit
    ) {
        var jointContext = ""
        val retrievedContextList = ArrayList<RetrievedContext>()
        //gmailHelper = GmailHelper(this, documentsUseCase)

        // Coroutine to query Gmail, load emails, and proceed to embeddings and response generation
        CoroutineScope(Dispatchers.IO).launch {
            // Step 1: Query Gmail to get message IDs based on the query
            val messageIds = gmailHelper.query_to_idlist(query)

            if (messageIds.isNotEmpty()) {
                Log.d("QAUseCase", "Message IDs found: $messageIds")

                // Step 2: Load and process the emails using the message IDs
                gmailHelper.loadMails_idlist(messageIds)

                // Optionally, you can concatenate all email contents to include in the context
                // For now, let's assume each email content has been processed by loadMails_idlist

                // Step 3: Retrieve embeddings and build joint context based on loaded emails
                chunksUseCase.getSimilarChunks(query, n = 5).forEach {
                    jointContext += " " + it.second.chunkData
                    retrievedContextList.add(RetrievedContext(it.second.docFileName, it.second.chunkData))
                }
                Log.d("QAUseCase", "Embedding Context: $jointContext")

                val inputPrompt = prompt.replace("\$CONTEXT", jointContext).replace("\$QUERY", query)
                Log.d("QAUseCase", "Input Prompt with Embedding: $inputPrompt")

                // Step 4: Use the embedding context to get a response from the model
                geminiRemoteAPI.getResponse(inputPrompt)?.let { llmResponse ->
                    // Final result based on retrieved emails and embedding context
                    onResponse(QueryResult(llmResponse, retrievedContextList))
                }

                // Step 5: On-device processing based on the retrieved chunks
                var ond_jointContext = ""
                chunksUseCase.getSimilarChunks(query, n = 1).forEach {
                    ond_jointContext = it.second.chunkData
                }

                val ond_inputPrompt = prompt.replace("\$CONTEXT", ond_jointContext).replace("\$QUERY", query)
                Log.d("QAUseCase", "On-device Input Prompt with Embedding: $ond_inputPrompt")
/*
                inferenceModel.generateResponseAsync(ond_inputPrompt)
                Log.d("QAUseCase", "Generating on-device response for prompt: $ond_inputPrompt")

                val collectedResults = mutableListOf<String>()
                inferenceModel.partialResults.collect { (partialResult, done) ->
                    if (partialResult.contains("<bos><bos><bos>")) {
                        Log.d("QAUseCase", "Stopping due to repeated <bos> tokens.")
                        return@collect
                    }

                    collectedResults.add(partialResult)

                    if (done) {
                        val fullResponse = collectedResults.joinToString(separator = "\n")
                        Log.d("QAUseCase", "Full on-device response: $fullResponse")

                        // Callback with the final on-device query result
                        onOnDeviceResponse(
                            OnDeviceQueryResult(responses = fullResponse, context = retrievedContextList)
                        )
                    }
                }

 */
            } else {
                Log.d("QAUseCase", "No messages found for query: $query")
                //withContext(Dispatchers.Main) {
                //    Toast.makeText(activity, "No emails found for this query", Toast.LENGTH_SHORT).show()
                //}
            }
        }
    }


}
