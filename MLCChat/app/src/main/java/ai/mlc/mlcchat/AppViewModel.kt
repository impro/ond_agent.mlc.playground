package ai.mlc.mlcchat

import ai.mlc.mlcllm.Chat
import ai.mlc.mlcllm.MLCEngine
import ai.mlc.mlcllm.OpenAIProtocol
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage
import android.util.Log
import com.lamrnd.docqa.GmailHelper
import com.lamrnd.ond_agent.connect.call_onsite.AgentCallback
import com.lamrnd.ond_agent.eventplay.actions.WebActions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val modelList = emptyList<ModelState>().toMutableStateList()
    val chatState = ChatState()
    val modelSampleList = emptyList<ModelRecord>().toMutableStateList()
    private var showAlert = mutableStateOf(false)
    private var alertMessage = mutableStateOf("")
    private var appConfig = AppConfig(
        emptyList<String>().toMutableList(),
        emptyList<ModelRecord>().toMutableList()
    )

//    private val _uiState: MutableStateFlow<GemmaUiState> = MutableStateFlow(GemmaUiState())
//    val uiState: StateFlow<UiState> =
//        _uiState.asStateFlow()

    private val application = getApplication<Application>()
    private val appDirFile = application.getExternalFilesDir("")
    private val gson = Gson()
    private val modelIdSet = emptySet<String>().toMutableSet()

    companion object {
        const val AppConfigFilename = "mlc-app-config.json"
        const val ModelConfigFilename = "mlc-chat-config.json"
        const val ParamsConfigFilename = "ndarray-cache.json"
        const val ModelUrlSuffix = "resolve/main/"
    }

    init {
        loadAppConfig()
    }

    fun isShowingAlert(): Boolean {
        return showAlert.value
    }

    fun errorMessage(): String {
        return alertMessage.value
    }

    fun dismissAlert() {
        require(showAlert.value)
        showAlert.value = false
    }

    fun copyError() {
        require(showAlert.value)
        val clipboard =
            application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MLCChat", errorMessage()))
    }

    private fun issueAlert(error: String) {
        showAlert.value = true
        alertMessage.value = error
    }

    fun requestDeleteModel(modelId: String) {
        deleteModel(modelId)
        issueAlert("Model: $modelId has been deleted")
    }


    private fun loadAppConfig() {
        val appConfigFile = File(appDirFile, AppConfigFilename)
        val jsonString: String = if (!appConfigFile.exists()) {
            application.assets.open(AppConfigFilename).bufferedReader().use { it.readText() }
        } else {
            appConfigFile.readText()
        }
        appConfig = gson.fromJson(jsonString, AppConfig::class.java)
        appConfig.modelLibs = emptyList<String>().toMutableList()
        modelList.clear()
        modelIdSet.clear()
        modelSampleList.clear()
        for (modelRecord in appConfig.modelList) {
            appConfig.modelLibs.add(modelRecord.modelLib)
            val modelDirFile = File(appDirFile, modelRecord.modelId)
            val modelConfigFile = File(modelDirFile, ModelConfigFilename)
            if (modelConfigFile.exists()) {
                val modelConfigString = modelConfigFile.readText()
                val modelConfig = gson.fromJson(modelConfigString, ModelConfig::class.java)
                modelConfig.modelId = modelRecord.modelId
                modelConfig.modelLib = modelRecord.modelLib
                modelConfig.estimatedVramBytes = modelRecord.estimatedVramBytes
                addModelConfig(modelConfig, modelRecord.modelUrl, true)
            } else {
                downloadModelConfig(
                    if (modelRecord.modelUrl.endsWith("/")) modelRecord.modelUrl else "${modelRecord.modelUrl}/",
                    modelRecord,
                    true
                )
            }
        }
    }

    private fun updateAppConfig(action: () -> Unit) {
        action()
        val jsonString = gson.toJson(appConfig)
        val appConfigFile = File(appDirFile, AppConfigFilename)
        appConfigFile.writeText(jsonString)
    }

    private fun addModelConfig(modelConfig: ModelConfig, modelUrl: String, isBuiltin: Boolean) {
        require(!modelIdSet.contains(modelConfig.modelId))
        modelIdSet.add(modelConfig.modelId)
        modelList.add(
            ModelState(
                modelConfig,
                modelUrl + if (modelUrl.endsWith("/")) "" else "/",
                File(appDirFile, modelConfig.modelId)
            )
        )
        if (!isBuiltin) {
            updateAppConfig {
                appConfig.modelList.add(
                    ModelRecord(
                        modelUrl,
                        modelConfig.modelId,
                        modelConfig.estimatedVramBytes,
                        modelConfig.modelLib
                    )
                )
            }
        }
    }

    private fun deleteModel(modelId: String) {
        val modelDirFile = File(appDirFile, modelId)
        modelDirFile.deleteRecursively()
        require(!modelDirFile.exists())
        modelIdSet.remove(modelId)
        modelList.removeIf { modelState -> modelState.modelConfig.modelId == modelId }
        updateAppConfig {
            appConfig.modelList.removeIf { modelRecord -> modelRecord.modelId == modelId }
        }
    }

    private fun isModelConfigAllowed(modelConfig: ModelConfig): Boolean {
        if (appConfig.modelLibs.contains(modelConfig.modelLib)) return true
        viewModelScope.launch {
            issueAlert("Model lib ${modelConfig.modelLib} is not supported.")
        }
        return false
    }


    private fun downloadModelConfig(
        modelUrl: String,
        modelRecord: ModelRecord,
        isBuiltin: Boolean
    ) {
        thread(start = true) {
            try {
                val url = URL("${modelUrl}${ModelUrlSuffix}${ModelConfigFilename}")
                val tempId = UUID.randomUUID().toString()
                val tempFile = File(
                    application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    tempId
                )
                url.openStream().use {
                    Channels.newChannel(it).use { src ->
                        FileOutputStream(tempFile).use { fileOutputStream ->
                            fileOutputStream.channel.transferFrom(src, 0, Long.MAX_VALUE)
                        }
                    }
                }
                require(tempFile.exists())
                viewModelScope.launch {
                    try {
                        val modelConfigString = tempFile.readText()
                        val modelConfig = gson.fromJson(modelConfigString, ModelConfig::class.java)
                        modelConfig.modelId = modelRecord.modelId
                        modelConfig.modelLib = modelRecord.modelLib
                        modelConfig.estimatedVramBytes = modelRecord.estimatedVramBytes
                        if (modelIdSet.contains(modelConfig.modelId)) {
                            tempFile.delete()
                            issueAlert("${modelConfig.modelId} has been used, please consider another local ID")
                            return@launch
                        }
                        if (!isModelConfigAllowed(modelConfig)) {
                            tempFile.delete()
                            return@launch
                        }
                        val modelDirFile = File(appDirFile, modelConfig.modelId)
                        val modelConfigFile = File(modelDirFile, ModelConfigFilename)
                        tempFile.copyTo(modelConfigFile, overwrite = true)
                        tempFile.delete()
                        require(modelConfigFile.exists())
                        addModelConfig(modelConfig, modelUrl, isBuiltin)
                    } catch (e: Exception) {
                        viewModelScope.launch {
                            issueAlert("Add model failed: ${e.localizedMessage}")
                        }
                    }
                }
            } catch (e: Exception) {
                viewModelScope.launch {
                    issueAlert("Download model config failed: ${e.localizedMessage}")
                }
            }

        }
    }

    inner class ModelState(
        val modelConfig: ModelConfig,
        private val modelUrl: String,
        private val modelDirFile: File
    ) {
        var modelInitState = mutableStateOf(ModelInitState.Initializing)
        private var paramsConfig = ParamsConfig(emptyList())
        val progress = mutableStateOf(0)
        val total = mutableStateOf(1)
        val id: UUID = UUID.randomUUID()
        private val remainingTasks = emptySet<DownloadTask>().toMutableSet()
        private val downloadingTasks = emptySet<DownloadTask>().toMutableSet()
        private val maxDownloadTasks = 3
        private val gson = Gson()


        init {
            switchToInitializing()
        }

        private fun switchToInitializing() {
            val paramsConfigFile = File(modelDirFile, ParamsConfigFilename)
            if (paramsConfigFile.exists()) {
                loadParamsConfig()
                switchToIndexing()
            } else {
                downloadParamsConfig()
            }
        }

        private fun loadParamsConfig() {
            val paramsConfigFile = File(modelDirFile, ParamsConfigFilename)
            require(paramsConfigFile.exists())
            val jsonString = paramsConfigFile.readText()
            paramsConfig = gson.fromJson(jsonString, ParamsConfig::class.java)
        }

        private fun downloadParamsConfig() {
            thread(start = true) {
                val url = URL("${modelUrl}${ModelUrlSuffix}${ParamsConfigFilename}")
                val tempId = UUID.randomUUID().toString()
                val tempFile = File(modelDirFile, tempId)
                url.openStream().use {
                    Channels.newChannel(it).use { src ->
                        FileOutputStream(tempFile).use { fileOutputStream ->
                            fileOutputStream.channel.transferFrom(src, 0, Long.MAX_VALUE)
                        }
                    }
                }
                require(tempFile.exists())
                val paramsConfigFile = File(modelDirFile, ParamsConfigFilename)
                tempFile.renameTo(paramsConfigFile)
                require(paramsConfigFile.exists())
                viewModelScope.launch {
                    loadParamsConfig()
                    switchToIndexing()
                }
            }
        }

        fun handleStart() {
            switchToDownloading()
        }

        fun handlePause() {
            switchToPausing()
        }

        fun handleClear() {
            require(
                modelInitState.value == ModelInitState.Downloading ||
                        modelInitState.value == ModelInitState.Paused ||
                        modelInitState.value == ModelInitState.Finished
            )
            switchToClearing()
        }

        private fun switchToClearing() {
            if (modelInitState.value == ModelInitState.Paused) {
                modelInitState.value = ModelInitState.Clearing
                clear()
            } else if (modelInitState.value == ModelInitState.Finished) {
                modelInitState.value = ModelInitState.Clearing
                if (chatState.modelName.value == modelConfig.modelId) {
                    chatState.requestTerminateChat { clear() }
                } else {
                    clear()
                }
            } else {
                modelInitState.value = ModelInitState.Clearing
            }
        }

        fun handleDelete() {
            require(
                modelInitState.value == ModelInitState.Downloading ||
                        modelInitState.value == ModelInitState.Paused ||
                        modelInitState.value == ModelInitState.Finished
            )
            switchToDeleting()
        }

        private fun switchToDeleting() {
            if (modelInitState.value == ModelInitState.Paused) {
                modelInitState.value = ModelInitState.Deleting
                delete()
            } else if (modelInitState.value == ModelInitState.Finished) {
                modelInitState.value = ModelInitState.Deleting
                if (chatState.modelName.value == modelConfig.modelId) {
                    chatState.requestTerminateChat { delete() }
                } else {
                    delete()
                }
            } else {
                modelInitState.value = ModelInitState.Deleting
            }
        }

        private fun switchToIndexing() {
            modelInitState.value = ModelInitState.Indexing
            progress.value = 0
            total.value = modelConfig.tokenizerFiles.size + paramsConfig.paramsRecords.size
            for (tokenizerFilename in modelConfig.tokenizerFiles) {
                val file = File(modelDirFile, tokenizerFilename)
                if (file.exists()) {
                    ++progress.value
                } else {
                    remainingTasks.add(
                        DownloadTask(
                            URL("${modelUrl}${ModelUrlSuffix}${tokenizerFilename}"),
                            file
                        )
                    )
                }
            }
            for (paramsRecord in paramsConfig.paramsRecords) {
                val file = File(modelDirFile, paramsRecord.dataPath)
                if (file.exists()) {
                    ++progress.value
                } else {
                    remainingTasks.add(
                        DownloadTask(
                            URL("${modelUrl}${ModelUrlSuffix}${paramsRecord.dataPath}"),
                            file
                        )
                    )
                }
            }
            if (progress.value < total.value) {
                switchToPaused()
            } else {
                switchToFinished()
            }
        }

        private fun switchToDownloading() {
            modelInitState.value = ModelInitState.Downloading
            for (downloadTask in remainingTasks) {
                if (downloadingTasks.size < maxDownloadTasks) {
                    handleNewDownload(downloadTask)
                } else {
                    return
                }
            }
        }

        private fun handleNewDownload(downloadTask: DownloadTask) {
            require(modelInitState.value == ModelInitState.Downloading)
            require(!downloadingTasks.contains(downloadTask))
            downloadingTasks.add(downloadTask)
            thread(start = true) {
                val tempId = UUID.randomUUID().toString()
                val tempFile = File(modelDirFile, tempId)
                downloadTask.url.openStream().use {
                    Channels.newChannel(it).use { src ->
                        FileOutputStream(tempFile).use { fileOutputStream ->
                            fileOutputStream.channel.transferFrom(src, 0, Long.MAX_VALUE)
                        }
                    }
                }
                require(tempFile.exists())
                tempFile.renameTo(downloadTask.file)
                require(downloadTask.file.exists())
                viewModelScope.launch {
                    handleFinishDownload(downloadTask)
                }
            }
        }

        private fun handleNextDownload() {
            require(modelInitState.value == ModelInitState.Downloading)
            for (downloadTask in remainingTasks) {
                if (!downloadingTasks.contains(downloadTask)) {
                    handleNewDownload(downloadTask)
                    break
                }
            }
        }

        private fun handleFinishDownload(downloadTask: DownloadTask) {
            remainingTasks.remove(downloadTask)
            downloadingTasks.remove(downloadTask)
            ++progress.value
            require(
                modelInitState.value == ModelInitState.Downloading ||
                        modelInitState.value == ModelInitState.Pausing ||
                        modelInitState.value == ModelInitState.Clearing ||
                        modelInitState.value == ModelInitState.Deleting
            )
            if (modelInitState.value == ModelInitState.Downloading) {
                if (remainingTasks.isEmpty()) {
                    if (downloadingTasks.isEmpty()) {
                        switchToFinished()
                    }
                } else {
                    handleNextDownload()
                }
            } else if (modelInitState.value == ModelInitState.Pausing) {
                if (downloadingTasks.isEmpty()) {
                    switchToPaused()
                }
            } else if (modelInitState.value == ModelInitState.Clearing) {
                if (downloadingTasks.isEmpty()) {
                    clear()
                }
            } else if (modelInitState.value == ModelInitState.Deleting) {
                if (downloadingTasks.isEmpty()) {
                    delete()
                }
            }
        }

        private fun clear() {
            val files = modelDirFile.listFiles { dir, name ->
                !(dir == modelDirFile && name == ModelConfigFilename)
            }
            require(files != null)
            for (file in files) {
                file.deleteRecursively()
                require(!file.exists())
            }
            val modelConfigFile = File(modelDirFile, ModelConfigFilename)
            require(modelConfigFile.exists())
            switchToIndexing()
        }

        private fun delete() {
            modelDirFile.deleteRecursively()
            require(!modelDirFile.exists())
            requestDeleteModel(modelConfig.modelId)
        }

        private fun switchToPausing() {
            modelInitState.value = ModelInitState.Pausing
        }

        private fun switchToPaused() {
            modelInitState.value = ModelInitState.Paused
        }


        private fun switchToFinished() {
            modelInitState.value = ModelInitState.Finished
        }

        fun startChat() {
            chatState.requestReloadChat(
                modelConfig,
                modelDirFile.absolutePath,
            )
        }

    }

    inner class ChatState {
        private val context = getApplication<Application>().applicationContext

        val messages = emptyList<MessageData>().toMutableStateList()
        val report = mutableStateOf("")
        val modelName = mutableStateOf("")
        private var modelChatState = mutableStateOf(ModelChatState.Ready)
            @Synchronized get
            @Synchronized set
        private val engine = MLCEngine()
        private var historyMessages = mutableListOf<ChatCompletionMessage>()
        private var modelLib = ""
        private var modelPath = ""
        private val executorService = Executors.newSingleThreadExecutor()
        private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())
        private fun mainResetChat() {
            executorService.submit {
                callBackend { engine.reset() }
                historyMessages = mutableListOf<ChatCompletionMessage>()
                viewModelScope.launch {
                    clearHistory()
                    switchToReady()
                }
            }
        }

        private fun clearHistory() {
            messages.clear()
            report.value = ""
            historyMessages.clear()
        }


        private fun switchToResetting() {
            modelChatState.value = ModelChatState.Resetting
        }

        private fun switchToGenerating() {
            modelChatState.value = ModelChatState.Generating
        }

        private fun switchToReloading() {
            modelChatState.value = ModelChatState.Reloading
        }

        private fun switchToReady() {
            modelChatState.value = ModelChatState.Ready
        }

        private fun switchToFailed() {
            modelChatState.value = ModelChatState.Falied
        }

        private fun callBackend(callback: () -> Unit): Boolean {
            try {
                callback()
            } catch (e: Exception) {
                viewModelScope.launch {
                    val stackTrace = e.stackTraceToString()
                    val errorMessage = e.localizedMessage
                    appendMessage(
                        MessageRole.Assistant,
                        "MLCChat failed\n\nStack trace:\n$stackTrace\n\nError message:\n$errorMessage"
                    )
                    switchToFailed()
                }
                return false
            }
            return true
        }

        fun requestResetChat() {
            require(interruptable())
            interruptChat(
                prologue = {
                    switchToResetting()
                },
                epilogue = {
                    mainResetChat()
                }
            )
        }

        private fun interruptChat(prologue: () -> Unit, epilogue: () -> Unit) {
            // prologue runs before interruption
            // epilogue runs after interruption
            require(interruptable())
            if (modelChatState.value == ModelChatState.Ready) {
                prologue()
                epilogue()
            } else if (modelChatState.value == ModelChatState.Generating) {
                prologue()
                executorService.submit {
                    viewModelScope.launch { epilogue() }
                }
            } else {
                require(false)
            }
        }

        fun requestTerminateChat(callback: () -> Unit) {
            require(interruptable())
            interruptChat(
                prologue = {
                    switchToTerminating()
                },
                epilogue = {
                    mainTerminateChat(callback)
                }
            )
        }

        private fun mainTerminateChat(callback: () -> Unit) {
            executorService.submit {
                callBackend { engine.unload() }
                viewModelScope.launch {
                    clearHistory()
                    switchToReady()
                    callback()
                }
            }
        }

        private fun switchToTerminating() {
            modelChatState.value = ModelChatState.Terminating
        }


        fun requestReloadChat(modelConfig: ModelConfig, modelPath: String) {

            if (this.modelName.value == modelConfig.modelId && this.modelLib == modelConfig.modelLib && this.modelPath == modelPath) {
                return
            }
            require(interruptable())
            interruptChat(
                prologue = {
                    switchToReloading()
                },
                epilogue = {
                    mainReloadChat(modelConfig, modelPath)
                }
            )
        }

        private fun mainReloadChat(modelConfig: ModelConfig, modelPath: String) {
            clearHistory()
            this.modelName.value = modelConfig.modelId
            this.modelLib = modelConfig.modelLib
            this.modelPath = modelPath
            executorService.submit {
                viewModelScope.launch {
                    Toast.makeText(application, "Initialize...", Toast.LENGTH_SHORT).show()
                }
                if (!callBackend {
                        engine.unload()
                        engine.reload(modelPath, modelConfig.modelLib)
                    }) return@submit
                viewModelScope.launch {
                    Toast.makeText(application, "Ready to chat", Toast.LENGTH_SHORT).show()
                    switchToReady()
                }
            }
        }
/*
        fun requestGenerate(prompt: String) {
            require(chatable())
            switchToGenerating()
            appendMessage(MessageRole.User, prompt)
            appendMessage(MessageRole.Assistant, "")

            executorService.submit {
                historyMessages.add(ChatCompletionMessage(
                    role = OpenAIProtocol.ChatCompletionRole.user,
                    content = prompt
                ))

                viewModelScope.launch {
                    val responses = engine.chat.completions.create(
                        messages = historyMessages,
                        stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
                    )

                    var finishReasonLength = false
                    var streamingText = ""

                    for (res in responses) {
                        if (!callBackend {
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
                            updateMessage(MessageRole.Assistant, streamingText)
                            res.usage?.let { finalUsage ->
                                report.value = finalUsage.extra?.asTextLabel() ?: ""
                            }
                            if (finishReasonLength) {
                                streamingText += " [output truncated due to context length limit...]"
                                updateMessage(MessageRole.Assistant, streamingText)
                            }
                        });
                    }
                    if (streamingText.isNotEmpty()) {
                        historyMessages.add(ChatCompletionMessage(
                            role = OpenAIProtocol.ChatCompletionRole.assistant,
                            content = streamingText
                        ))
                        streamingText = ""
                    } else {
                        if (historyMessages.isNotEmpty()) {
                            historyMessages.removeAt(historyMessages.size - 1)
                        }
                    }

                    if (modelChatState.value == ModelChatState.Generating) switchToReady()
                }
            }
        }
*/
        fun extractSearchQuery_20241031(resultText: String): String {
        //fun extractSearchQuery(resultText: String): String {
            // 결과 텍스트에서 쿼리 부분을 추출하는 로직
            // 예: "subject: 멀티 LLM에이전트 장애" 부분만 추출
            //val pattern = Regex("subject:(.*)")
            //val pattern = Regex("(subject:.*)") // subject: 를 포함하여 추출
            val pattern = Regex("(subject:[^\"']*)") // subject: 를 포함하여 "" 또는 '' 없는 상태로 추출


            val matchResult = pattern.find(resultText)
            return matchResult?.groupValues?.get(1)?.trim() ?: ""
        }

        fun extractSearchQuery(resultText: String): String {
            // "subject:"로 시작하여 공백 뒤로는 단어가 끝날 때까지 추출
            //val pattern = Regex("subject:[^\\s]+") // subject: 뒤의 텍스트를 공백 전까지 추출
            // "subject:"로 시작하여 한글 또는 영문 텍스트만 추출
            val pattern = Regex("subject:([\\p{L}\\p{N}\\s]+)") // subject: 뒤에 이어지는 한글, 영어 및 숫자와 공백을 포함하여 추출

            val matchResult = pattern.find(resultText)
            return matchResult?.groupValues?.get(0)?.trim() ?: ""
        }

        fun requestGenerateSQ(queryText: String): Deferred<String> = viewModelScope.async(Dispatchers.IO) {
            require(chatable())
            switchToGenerating()

            // "WV_MAIL" 문자열 제거
            val processedQueryText = queryText.replace("WV_MAIL", "").trim()

            // Query Text를 Gmail 검색 쿼리로 변환하는 입력 프롬프트 작성
            val inputPrompt = """
                Convert the following natural language query to a Gmail search query:
                "$processedQueryText"
        
                Gmail search query should use tags like 'subject:'
                make sure only subject keywords are used
            """.trimIndent()

            appendMessage(MessageRole.User, inputPrompt)
            appendMessage(MessageRole.Assistant, "")

            historyMessages.add(ChatCompletionMessage(
                role = OpenAIProtocol.ChatCompletionRole.user,
                content = inputPrompt
            ))

            try {
                val responses = engine.chat.completions.create(
                    messages = historyMessages,
                    stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
                )

                var finishReasonLength = false
                var streamingText = ""

                for (res in responses) {
                    if (!callBackend {
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
                            val searchQuery = extractSearchQuery(streamingText)
                            //streamingText = extractSearchQuery(streamingText)
                            //    .replace("\"", "") // " 제거
                            //    .replace("'", "")  // ' 제거
                            //    .replace("`", "")  // ' 제거

                            updateMessage(MessageRole.Assistant, "[$searchQuery]")
                            //updateMessage(MessageRole.Assistant, "[$streamingText]")

                            res.usage?.let { finalUsage ->
                                report.value = finalUsage.extra?.asTextLabel() ?: ""
                            }
                            if (finishReasonLength) {
                                streamingText += " [output truncated due to context length limit...]"
                                updateMessage(MessageRole.Assistant, streamingText)
                            }
                        }) {
                        break
                    }
                }

                if (streamingText.isNotEmpty()) {
                    historyMessages.add(ChatCompletionMessage(
                        role = OpenAIProtocol.ChatCompletionRole.assistant,
                        content = streamingText
                    ))
                } else {
                    if (historyMessages.isNotEmpty()) {
                        historyMessages.removeAt(historyMessages.size - 1)
                    }
                }

                if (modelChatState.value == ModelChatState.Generating) switchToReady()

                extractSearchQuery(streamingText)
                //streamingText
                //.replace("\"", "") // " 제거
                //.replace("'", "")  // ' 제거 // 결과 반환
                //.replace("`", "")  // ' 제거
            } catch (e: Exception) {
                Log.e("SendMessageView", "Error occurred: ${e.message}")
                throw e
            }
        }
        fun requestGenerateSQ_20241030(queryText: String) : CompletableFuture<String> {
            require(chatable())
            switchToGenerating()
            // "WV_MAIL" 문자열 제거
            val processedQueryText = queryText.replace("WV_MAIL", "").trim()
            val futureResult = CompletableFuture<String>()

            //Convert the following natural language query into a Gmail search query:
            //Gmail search query should use tags like 'from:', 'to:', 'subject:', 'has:attachment', etc.
            /*
            queryText
            val inputPrompt = """
                Convert the following natural language query to a Gmail search query:
                "$processedQueryText"

                Gmail search query should use tags like 'from:', 'to:', 'subject:'
                make sure only from, to, subject keywords are useds
            """.trimIndent()
            */

            //queryText
            val inputPrompt = """
                Convert the following natural language query to a Gmail search query:
                "$processedQueryText"
                
                Gmail search query should use tags like 'subject:'
                make sure only subject keywords are useds
            """.trimIndent()

            appendMessage(MessageRole.User, inputPrompt)
            appendMessage(MessageRole.Assistant, "")

            //executorService.submit {
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        historyMessages.add(ChatCompletionMessage(
                            role = OpenAIProtocol.ChatCompletionRole.user,
                            content = inputPrompt
                        ))

                        //viewModelScope.launch {
                        val responses = engine.chat.completions.create(
                            messages = historyMessages,
                            stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
                        )

                        var finishReasonLength = false
                        var streamingText = ""

                        for (res in responses) {
                            if (!callBackend {
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
                                    //updateMessage(MessageRole.Assistant, streamingText)
                                    // 결과에서 쿼리 부분만 추출
                                    val searchQuery = extractSearchQuery(streamingText)
                                    //updateMessage(MessageRole.Assistant, "streamingText")

                                    // 쿼리 부분을 대괄호로 감싸서 출력
                                    updateMessage(MessageRole.Assistant, "[$searchQuery]")

                                    res.usage?.let { finalUsage ->
                                        report.value = finalUsage.extra?.asTextLabel() ?: ""
                                    }
                                    if (finishReasonLength) {
                                        streamingText += " [output truncated due to context length limit...]"
                                        updateMessage(MessageRole.Assistant, streamingText)
                                    }
                                }) {
                                break
                            }
                        }
                        if (streamingText.isNotEmpty()) {
                            historyMessages.add(ChatCompletionMessage(
                                role = OpenAIProtocol.ChatCompletionRole.assistant,
                                content = streamingText
                            ))
                            streamingText = ""
                        } else {
                            if (historyMessages.isNotEmpty()) {
                                historyMessages.removeAt(historyMessages.size - 1)
                            }
                        }

                        if (modelChatState.value == ModelChatState.Generating) switchToReady()

                        val searchQuery = extractSearchQuery(streamingText)
                        futureResult.complete(searchQuery)  // 결과 반환

                    }
                } catch (e: Exception) {
                    Log.e("SendMessageView", "Error occurred: ${e.message}")
                    futureResult.completeExceptionally(e)
                }
            }
            return futureResult
        }

        //fun requestGenerateEmbeddingVector(queryText: String, gmailHelper: GmailHelper) : CompletableFuture<Unit> {
        fun requestGenerateEmbeddingVector(searchQueryText: String, gmailHelper: GmailHelper) : CompletableFuture<Unit> {
            return CoroutineScope(Dispatchers.IO).future {
                // Gmail query generation
                //val queryCommand = "subject: $searchQueryText"
                val queryCommand = searchQueryText
                Log.d("AppViewModel", "requestGenerateEmbeddingVector : Query Command: $queryCommand")

                // Fetch email IDs
                val messageIds = gmailHelper.query_to_idlist(queryCommand)
                Log.d("AppViewModel", "requestGenerateEmbeddingVector : Message IDs found: $messageIds")

                if (messageIds.isNotEmpty()) {
                    // Load emails
                    gmailHelper.loadMails_idlist(messageIds)
                    Log.d("AppViewModel", "requestGenerateEmbeddingVector : Mails loaded successfully")
                } else {
                    Log.d("AppViewModel", "requestGenerateEmbeddingVector: No messages found for the query.")
                }
            }
            /*          val futureResult = CompletableFuture<Unit>()
            CompletableFuture.runAsync {
                try {

                    // Gmail 쿼리 생성
                    val queryCommand = "subject: $queryText"
                    Log.d("QAUseCase", "Query Command: $queryCommand")

                    // 이메일 ID 목록 가져오기
                    val messageIds = gmailHelper.query_to_idlist(queryCommand)
                    Log.d("QAUseCase", "Message IDs found: $messageIds")

                    if (messageIds.isNotEmpty()) {
                        // 이메일 로드
                        gmailHelper.loadMails_idlist(messageIds)
                        Log.d("QAUseCase", "Mails loaded successfully")
                    } else {
                        Log.d("QAUseCase", "No messages found for the query.")
                    }
                } catch (e:Exception) {
                    // Complete the future exceptionally
                    futureResult.completeExceptionally(e)
                }
            }
            return futureResult
   */
        }
        // 기타 필요한 함수들...
    //}

        fun requestGenerateREACTPostOnD(queryText: String) : CompletableFuture<String> {
            clearHistory()
            require(chatable())
            switchToGenerating()
            // "WV_MAIL" 문자열 제거

            val contentText = "제목: [AIF 6기] 최종발표 심사위원 참석대상자 확인 요청 드립니다. (~10.16 수) 안녕하세요, SKT AI Fellowship 6기 담당자 역량혁신팀 노새미입니다. 어느덧 AI Fellowship 6기의 최종발표가 다가오고 있습니다. 멘토님들께서도 지금 이 순간까지 열정적으로 멘토링을 진행해주고 계실 텐데요, 다시 한번 감사의 말씀 전합니다. 10월 31일(목)으로 예정된 최종발표에는 A, B, C, D 그룹의 리더 및 멘토분들이 심사위원으로 참여하게 됩니다. 각 팀에서는 리더와 멘토 한 분씩, 팀별 최소 2명 이상 참석해 주셔야 합니다. 아래 공유 파일에서 일정을 확인하시고, H21셀의 '참석 여부(O/X)'에 10월 16일(수)까지 참석 가능 여부를 표시해 주시기 바랍니다. 👉 xlsx 아이콘 SKT AI Fellowship 6기_최종심사 안내 및 참석대상자 확인.xlsx * 심사위원은 본인이 속한 그룹 세션에 필수 참석해 주시기 바랍니다. * 심사 기준 및 방법은 참석자 대상으로 추후에 상세 안내 드리겠습니다. 추가로 향후 일정을 안내 드리오니 사전에 일정을 꼭 확인 부탁드립니다. u 최종발표자료 전달 : 10월 28일(월) 오후 u 최종발표 및 심사 : 10월 31일(목), Zoom u 우수팀 발표 : 11월 7일(목), Discord u 수료식 : 11월 19일(화), Supex Hall 최종심사에 대한 상세 내용은 추후에 별도로 안내 드리겠습니다. 문의 사항이 있으시면 언제든지 연락 주시기 바랍니다.  최종자료 전달은 언제야?"
            //val processedContenText = contentText.replace("WV_MAIL", "").trim()
            val futureResult = CompletableFuture<String>()


            // AI 모델에 전달할 프롬프트 구성
            /*
            val inputPrompt = """
                The following is an email content:
                
                "$contentText"
                
                Based on the above email content, please answer the following user query:
                "$queryText"
                
                If the answer is found within the email content, provide a direct and concise answer. If not, reply that the information is not available in the email.
            """.trimIndent()
            */
            val inputPrompt = """
                You are an AI assistant. Please answer the user's question based on the provided email content. 
                If the email contains relevant information, answer directly. Otherwise, indicate that the answer is not available.
            
                Email content:
                "$contentText"
            
                User question:
                "$queryText"
            """.trimIndent()

            /*
            historyMessages.add(ChatCompletionMessage(
                role = OpenAIProtocol.ChatCompletionRole.system,
                content = "You are an assistant that summarizes or Answer from Question from emails."
            ))
            */
            //)
            //queryText
            //Please summarize the following email content:
            //Additionally, identify when the final presentation material is due if mentioned in the email content.

            /*
            val inputPrompt = """
                Please answer the following email content:
                "$processedContenText"
            """.trimIndent()
            */

            //val inputPrompt = contentText.trimIndent()

            appendMessage(MessageRole.User, inputPrompt)
            appendMessage(MessageRole.Assistant, "")

            executorService.submit {
                historyMessages.add(ChatCompletionMessage(
                    role = OpenAIProtocol.ChatCompletionRole.user,
                    content = inputPrompt
                    //content = contentText
                ))

                viewModelScope.launch {
                    val responses = engine.chat.completions.create(
                        messages = historyMessages,
                        stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
                    )

                    var finishReasonLength = false
                    var streamingText = ""

                    for (res in responses) {
                        if (!callBackend {
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
                                //updateMessage(MessageRole.Assistant, "streamingText")
                                updateMessage(MessageRole.Assistant, streamingText)


                                // 쿼리 부분을 대괄호로 감싸서 출력
                                //updateMessage(MessageRole.Assistant, "[$searchQuery]")

                                res.usage?.let { finalUsage ->
                                    report.value = finalUsage.extra?.asTextLabel() ?: ""
                                }
                                if (finishReasonLength) {
                                    streamingText += " [output truncated due to context length limit...]"
                                    updateMessage(MessageRole.Assistant, streamingText)
                                }
                            });
                    }
                    if (streamingText.isNotEmpty()) {
                        historyMessages.add(ChatCompletionMessage(
                            role = OpenAIProtocol.ChatCompletionRole.assistant,
                            content = streamingText
                        ))
                        streamingText = ""
                    } else {
                        if (historyMessages.isNotEmpty()) {
                            historyMessages.removeAt(historyMessages.size - 1)
                        }
                    }

                    if (modelChatState.value == ModelChatState.Generating) switchToReady()

                    //val searchQuery = extractSearchQuery(streamingText)
                    futureResult.complete(streamingText)  // 결과 반환

                }
            }

            return futureResult
        }
        //fun requestGenerateREACT(prompt: String) {
        //fun requestGenerateREACT(prompt: String) : CompletableFuture<String>{
        fun requestGenerateREACT(prompt: String,  qsresult : String) : CompletableFuture<String>{
        //fun requestGenerateREACT(prompt: String, qsresult : String) {
            val futureResult = CompletableFuture<String>()

            if (!chatable()) {
                // 채팅이 불가능한 상태일 때의 처리
                //Log.d("AppViewModel", "현재 채팅을 생성할 수 없는 상태입니다.")
                //return
                futureResult.completeExceptionally(IllegalStateException("채팅을 생성할 수 없는 상태입니다."))
                return futureResult
            }
            Log.d("AppViewModel", "requestGenerateREACT: " + prompt)
            Log.d("AppViewModel", "requestGenerateREACT: " + qsresult)

            if (prompt.toLowerCase().startsWith("wa_mail")) {
                Log.d("AppViewModel", "=============SKILL SET MAIL START wa_mail =============")
                //appendMessage(MessageRole.User, prompt)
                // wa_mail에 대한 추가 처리 로직
                // 예: performMailActions(prompt)
                Log.d("AppViewModel", "=============SKILL SET MAIL END=============")
                futureResult.complete("wa_mail 처리 완료")

            } else if (prompt.toLowerCase().startsWith("wv_mail")) {
                Log.d("AppViewModel", "=============SKILL SET MAIL START wv_mail =============")
                switchToGenerating()
                appendMessage(MessageRole.Assistant, "READY for Privacy Data Query (WV_MAIL)")
                appendMessage(MessageRole.User, prompt)
                //_uiState.value.addMessage(userMessage, USER_PREFIX)

                // wv_mail에 대한 추가 처리 로직
                performWVMailActions(prompt) // 외부 에이전트 호출 등
                Log.d("AppViewModel", "=============SKILL SET MAIL END=============")
                futureResult.complete("wv_mail 처리 완료")

            } else {
                switchToGenerating()
                appendMessage(MessageRole.User, prompt)
                appendMessage(MessageRole.Assistant, "")

                executorService.submit {
                    historyMessages.add(
                        ChatCompletionMessage(
                            role = OpenAIProtocol.ChatCompletionRole.user,
                            content = prompt
                        )
                    )

                    viewModelScope.launch {
                        try {
                            val responses = engine.chat.completions.create(
                                messages = historyMessages,
                                stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
                            )

                            var finishReasonLength = false
                            var streamingText = ""

                            for (res in responses) {
                                if (!callBackend {
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
                                        updateMessage(MessageRole.Assistant, streamingText)
                                        res.usage?.let { finalUsage ->
                                            report.value = finalUsage.extra?.asTextLabel() ?: ""
                                        }
                                        if (finishReasonLength) {
                                            streamingText += " [출력이 문맥 길이 제한으로 잘렸습니다...]"
                                            updateMessage(MessageRole.Assistant, streamingText)
                                        }
                                    });
                            }

                            if (streamingText.isNotEmpty()) {
                                historyMessages.add(
                                    ChatCompletionMessage(
                                        role = OpenAIProtocol.ChatCompletionRole.assistant,
                                        content = streamingText
                                    )
                                )
                                futureResult.complete(streamingText)
                                //streamingText = ""

                            } else {
                                if (historyMessages.isNotEmpty()) {
                                    historyMessages.removeAt(historyMessages.size - 1)
                                }
                                futureResult.complete("응답이 없습니다.")
                            }
                        } catch (e: Exception) {
                            // 예외 처리
                            Log.e("AppViewModel", "응답 생성 중 오류 발생: ${e.localizedMessage}")
                            //appendMessage(
                            //    MessageRole.Assistant,
                            //    e.localizedMessage ?: "알 수 없는 오류가 발생했습니다."
                            //)
                            futureResult.completeExceptionally(e)

                        } finally {
                            if (modelChatState.value == ModelChatState.Generating) switchToReady()
                        }
                    }
                }
            }
            return futureResult
        }

        private fun performWVMailActions(inputString: String) {
            val webActions = WebActions()
            Log.d("CHATVIEW:performWVMailActions", "CHATVIEW:performWVMailActions")
            Log.d("CHATVIEW:performWVMailActions", "inputString: " + inputString)

            webActions.performWVActions(context, inputString , object : AgentCallback {
                override fun invoke(result: String?) {
                    Log.d("CHATVIEW:performWVMailActions:invoke", "CHATVIEW:performWVMailActions:invoke")
                    viewModelScope.launch {
                        if (result != null) {
//                            _uiState.value.addMessage(result, MODEL_PREFI  X_WBA)
                            appendMessage(MessageRole.Assistant, result)
                            //appendMessage(MessageRole.User, prompt)

                            Log.d("CHATVIEW:performWVMailActions:invoke:launch", "CHATVIEW:performWVMailActions:invoke (채팅장): " + result)
//                            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                            //Toast.makeText(context, result, Toast.LENGTH_LONG).show()

                        } else {
//                            _uiState.value.addMessage( "Failed to get result from agent" , USER_PREFIX)
                            Log.d("CHATVIEW:performWVMailActions:invoke:launch", "Failed to get result from agent")
                        }
                    }
                }
            })

        }


        private fun appendMessage(role: MessageRole, text: String) {
            messages.add(MessageData(role, text))
        }


        fun updateMessage(role: MessageRole, text: String) {
            messages[messages.size - 1] = MessageData(role, text)
        }

        fun chatable(): Boolean {
            return modelChatState.value == ModelChatState.Ready
        }

        fun interruptable(): Boolean {
            return modelChatState.value == ModelChatState.Ready
                    || modelChatState.value == ModelChatState.Generating
                    || modelChatState.value == ModelChatState.Falied
        }

        suspend fun <T> callBackend(callback: suspend () -> T): T? {
            return try {
                callback()
            } catch (e: Exception) {
                viewModelScope.launch {
                    val stackTrace = e.stackTraceToString()
                    val errorMessage = e.localizedMessage
                    appendMessage(
                        MessageRole.Assistant,
                        "Operation failed\n\nStack trace:\n$stackTrace\n\nError message:\n$errorMessage"
                    )
                    switchToFailed()
                }
                null
            }
        }
    }
}

enum class ModelInitState {
    Initializing,
    Indexing,
    Paused,
    Downloading,
    Pausing,
    Clearing,
    Deleting,
    Finished
}

enum class ModelChatState {
    Generating,
    Resetting,
    Reloading,
    Terminating,
    Ready,
    Falied
}

enum class MessageRole {
    Assistant,
    User
}

data class DownloadTask(val url: URL, val file: File)

data class MessageData(val role: MessageRole, val text: String, val id: UUID = UUID.randomUUID())

data class AppConfig(
    @SerializedName("model_libs") var modelLibs: MutableList<String>,
    @SerializedName("model_list") val modelList: MutableList<ModelRecord>,
)

data class ModelRecord(
    @SerializedName("model_url") val modelUrl: String,
    @SerializedName("model_id") val modelId: String,
    @SerializedName("estimated_vram_bytes") val estimatedVramBytes: Long?,
    @SerializedName("model_lib") val modelLib: String
)

data class ModelConfig(
    @SerializedName("model_lib") var modelLib: String,
    @SerializedName("model_id") var modelId: String,
    @SerializedName("estimated_vram_bytes") var estimatedVramBytes: Long?,
    @SerializedName("tokenizer_files") val tokenizerFiles: List<String>,
    @SerializedName("context_window_size") val contextWindowSize: Int,
    @SerializedName("prefill_chunk_size") val prefillChunkSize: Int,
)

data class ParamsRecord(
    @SerializedName("dataPath") val dataPath: String
)

data class ParamsConfig(
    @SerializedName("records") val paramsRecords: List<ParamsRecord>
)
