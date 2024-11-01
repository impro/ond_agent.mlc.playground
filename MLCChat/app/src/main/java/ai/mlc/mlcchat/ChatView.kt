package ai.mlc.mlcchat

import ai.mlc.mlcllm.MLCEngine
import ai.mlc.mlcllm.OpenAIProtocol
import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConnectingAirports
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.lamrnd.docqa.GmailHelper
import com.lamrnd.docqa.domain.llm.LLAMAOnDAPI
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import com.lamrnd.docqa.ui.viewModels.ChatViewModel
import com.lamrnd.docqa.ui.viewModels.ResponseType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@ExperimentalMaterial3Api
@Composable
fun ChatView(
    //navController: NavController, chatState: AppViewModel.ChatState
    navController: NavController, chatState: AppViewModel.ChatState, viewModel: AppViewModel
    , onOpenDocsClick: (() -> Unit), onInitMailClick: (() -> Unit), onMailLoadClick: (() -> Unit),
    gmailHelper: GmailHelper
) {
    val localFocusManager = LocalFocusManager.current
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    //text = "MLCChat: " + chatState.modelName.value.split("-")[0],
                    //text = "OnD_Agent-MLCChat: " + chatState.modelName.value.split("-")[0],
                    text = "Privacy Aware IntelliChat: " + chatState.modelName.value.split("-")[0],
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 18.sp // 원하는 글자 크기로 설정
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
            navigationIcon = {
                IconButton(
                    onClick = { navController.popBackStack() },
                    enabled = chatState.interruptable()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "back home page",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            actions = {
                IconButton(onClick = onOpenDocsClick) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Open Documents"
                    )
                }

                // 메일 로딩 버튼 추가
                IconButton(onClick = onInitMailClick) {
                    Icon(
                        imageVector = Icons.Default.ConnectingAirports, // 이메일 아이콘
                        contentDescription = "Init Mail Service"
                    )
                }
                // 메일 로딩 버튼 추가
                IconButton(onClick = onMailLoadClick) {
                    Icon(
                        imageVector = Icons.Default.Email, // 이메일 아이콘
                        contentDescription = "Load Mails"
                    )
                }

            //actions = {
                IconButton(
                    onClick = { chatState.requestResetChat() },
                    enabled = chatState.interruptable()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = "reset the chat",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            })
    }, modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(onTap = {
            localFocusManager.clearFocus()
        })
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 10.dp)
        ) {
            val lazyColumnListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            Text(
                text = chatState.report.value,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 5.dp)
            )
            Divider(thickness = 1.dp, modifier = Modifier.padding(vertical = 5.dp))
            LazyColumn(
                modifier = Modifier.weight(9f),
                verticalArrangement = Arrangement.spacedBy(5.dp, alignment = Alignment.Bottom),
                state = lazyColumnListState
            ) {
                coroutineScope.launch {
                    lazyColumnListState.animateScrollToItem(chatState.messages.size)
                }
                items(
                    items = chatState.messages,
                    key = { message -> message.id },
                ) { message ->
                    MessageView(messageData = message)
                }
                item {
                    // place holder item for scrolling to the bottom
                }
            }
            Divider(thickness = 1.dp, modifier = Modifier.padding(top = 5.dp))
            //SendMessageView(chatState = chatState)
            SendMessageView(viewModel = viewModel, chatState = chatState, gmailHelper = gmailHelper)

        }
    }
}

@Composable
fun MessageView(messageData: MessageData) {
    // default render the Assistant text as MarkdownText
    var useMarkdown by remember { mutableStateOf(true) }

    SelectionContainer {
        if (messageData.role == MessageRole.Assistant) {
            Column {
                if (messageData.text.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Show as Markdown",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(end = 8.dp)
                                .widthIn(max = 300.dp)
                        )
                        Switch(
                            checked = useMarkdown,
                            onCheckedChange = { useMarkdown = it }
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (useMarkdown) {
                        MarkdownText(
                            isTextSelectable = true,
                            modifier = Modifier
                                .wrapContentWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .padding(5.dp)
                                .widthIn(max = 300.dp),
                            markdown = messageData.text,
                        )
                    } else {
                        Text(
                            text = messageData.text,
                            textAlign = TextAlign.Left,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .wrapContentWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .padding(5.dp)
                                .widthIn(max = 300.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = messageData.text,
                    textAlign = TextAlign.Right,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .wrapContentWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(5.dp)
                        )
                        .padding(5.dp)
                        .widthIn(max = 300.dp)
                )

            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
//fun SendMessageView(chatState: AppViewModel.ChatState) {
fun SendMessageView(viewModel: AppViewModel, chatState: AppViewModel.ChatState, gmailHelper: GmailHelper
) {

    val localFocusManager = LocalFocusManager.current
    val context = LocalContext.current
    val engine = MLCEngine()
    //val chatViewModel: ChatViewModel = hiltViewModel()

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth()
            .padding(bottom = 5.dp)
    ) {
        var text by rememberSaveable { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()
        var historyMessages = mutableListOf<ChatCompletionMessage>()

        IconButton(onClick = {

            if (text.trim().isEmpty()) {
                Toast.makeText(context, "Enter a query to execute", Toast.LENGTH_LONG).show()
                return@IconButton
            }
            localFocusManager.clearFocus()
            //val currentText = text // text 값을 로컬 변수에 저장

            //var queryText = "AI Fellowship"
            var queryText = text
            Log.d("SendMessageView", "Query Text: $queryText")
            var processedQueryText = queryText

            val inputPrompt = """
                Convert the following natural language query to a Gmail search query:
                "$processedQueryText"
                
                Gmail search query should use tags like 'subject:'
                make sure only subject keywords are useds
            """.trimIndent()
            Log.d("SendMessageView", "Input Prompt: $inputPrompt")

            coroutineScope.launch {
                try {
                    // `await()`을 사용하여 `requestGenerateSQ`의 결과를 비동기적으로 기다림
                    val result = chatState.requestGenerateSQ(text).await()
                    Log.d("SendMessageView", "첫 번째 결과: $result")
                    result.replace("`", "")  // ' 제거
                    Log.d("SendMessageView", "첫 번째 결과 processing : $result")

                    if (result.isNullOrEmpty()) {
                        Log.e("SendMessageView", "첫 번째 결과가 null입니다.")
                        return@launch
                    }

                    Log.d("SendMessageView", "초기 text 값 2: $text")
                    Log.d("SendMessageView", "초기 text 값 local 2: $queryText") // currentText 사용

                    // 결과가 null이 아닌 경우에만 requestGenerateEmbeddingVector 호출
                    chatState.requestGenerateEmbeddingVector(result, gmailHelper)

                } catch (e: Exception) {
                    Log.e("SendMessageView", "에러 발생: ${e.message}")
                }
            }
            /*
// 변경된 부분: thenComposeAsync 사용
            chatState.requestGenerateSQ(text).thenComposeAsync { result ->
                Log.d("SendMessageView", "첫 번째 결과: $result")

                if (result == null) {
                    Log.e("SendMessageView", "첫 번째 결과가 null입니다.")
                    return@thenComposeAsync CompletableFuture.completedFuture(null)
                }

                Log.d("SendMessageView", "초기 text 값 2: $text")
                Log.d("SendMessageView", "초기 text 값 local 2: $queryText") // currentText 사용

                // 결과가 null이 아닌 경우에만 requestGenerateEmbeddingVector 호출
                chatState.requestGenerateEmbeddingVector(result, gmailHelper)
            }.thenAcceptAsync { secondReactResult ->
                Log.d("SendMessageView", "세번째 파이프라인 결과: $secondReactResult")
            }.exceptionally { e ->
                Log.e("SendMessageView", "에러 발생: ${e.message}")
                null
            }*/
            /*
            // 변경된 부분: thenApplyAsync 사용
            chatState.requestGenerateSQ(text).thenApplyAsync { result ->
                Log.d("SendMessageView", "첫 번째 결과: $result")

                if (result == null) {
                    Log.e("SendMessageView", "첫 번째 결과가 null입니다.")
                    return@thenApplyAsync null
                }

                Log.d("SendMessageView", "초기 text 값 2: $text")
                Log.d("SendMessageView", "초기 text 값 local 2: $queryText") // currentText 사용

                // 결과가 null이 아닌 경우에만 requestGenerateEmbeddingVector 호출
                result?.let {
                    chatState.requestGenerateEmbeddingVector(it, gmailHelper)
                }

                result // result 값을 반환
            }.thenAcceptAsync { secondReactResult ->
                Log.d("SendMessageView", "세번째 파이프라인 결과: $secondReactResult")
            }.exceptionally { e ->
                Log.e("SendMessageView", "에러 발생: ${e.message}")
                null
            }*/
/*TRY 20241030_2
            // 변경된 부분: thenCompose 사용
            chatState.requestGenerateSQ(text).thenCompose { result ->

                Log.d("SendMessageView", "첫 번째 결과: $result")

                if (result == null) {
                    Log.e("SendMessageView", "첫 번째 결과가 null입니다.")
                    return@thenCompose CompletableFuture.completedFuture(null)
                }

                Log.d("SendMessageView", "초기 text 값 2: $text")
                Log.d("SendMessageView", "초기 text 값 local 2: $queryText") // currentText 사용

                // 결과가 null이 아닌 경우에만 requestGenerateEmbeddingVector 호출
                result?.let {
                    chatState.requestGenerateEmbeddingVector(it, gmailHelper)
                } ?: CompletableFuture.completedFuture(null) // null 처리

            }.thenAccept { secondReactResult ->
                Log.d("SendMessageView", "세번째 파이프라인 결과: $secondReactResult")
            }.exceptionally { e ->
                Log.e("SendMessageView", "에러 발생: ${e.message}")
                null
            }
 */
            /*TRY 20241030_1
            // 변경된 부분: thenCompose 대신 thenApply 사용
            chatState.requestGenerateSQ(text).thenApply { result ->
                Log.d("SendMessageView", "첫 번째 결과: $result")

                if (result == null) {
                    Log.e("SendMessageView", "첫 번째 결과가 null입니다.")
                }

                Log.d("SendMessageView", "초기 text 값 2: $text")
                Log.d("SendMessageView", "초기 text 값 local 2: $queryText") // currentText 사용

                // 결과가 null이 아닌 경우에만 requestGenerateEmbeddingVector 호출
                result?.let {
                    chatState.requestGenerateEmbeddingVector(it, gmailHelper)
                }

                result // result 값을 반환
            }.thenAccept { secondReactResult ->
                Log.d("SendMessageView", "세번째 파이프라인 결과: $secondReactResult")
            }.exceptionally { e ->
                Log.e("SendMessageView", "에러 발생: ${e.message}")
                null
            }
 */           /*
            chatState.requestGenerateSQ(text).thenCompose { result ->
                Log.d("SendMessageView", "첫 번째 결과: $result")
                if (result == null) {
                    Log.e("SendMessageView", "첫 번째 결과가 null입니다.")
                }
                Log.d("SendMessageView", "초기 text 값 2: $text")
                Log.d("SendMessageView", "초기 text 값 local 2: $queryText") // currentText 사용

                //chatState.requestGenerateEmbeddingVector(queryText, gmailHelper)
                chatState.requestGenerateEmbeddingVector(result, gmailHelper)

                }.thenAccept { secondReactResult ->
                Log.d("SendMessageView", "세번째 파이프라인 결과: $secondReactResult")
            }.exceptionally { e ->
                Log.e("SendMessageView", "에러 발생: ${e.message}")
                null
            }
*/
            /*
            //state.appendMessage(MessageRole.User, inputPrompt)
            chatState.updateMessage(MessageRole.User, inputPrompt)
            //state.updateMessage(MessageRole.Assistant, streamingText)

            //appendMessage(MessageRole.Assistant, "")

            //executorService.submit {
            historyMessages.add(
                ChatCompletionMessage(
                    role = OpenAIProtocol.ChatCompletionRole.user,
                    content = inputPrompt
                )
            )
            Log.d("SendMessageView", "historyMessages: $historyMessages")

            coroutineScope.launch {
                val responses = engine.chat.completions.create(
                    messages = historyMessages,
                    stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
                )
                Log.d("SendMessageView", "responses: $responses")
                var finishReasonLength = false
                var streamingText = ""
                var finalText = ""

                //for (res in responses) {
                    /*
                    if (chatState?.callBackend {
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
                            chatState.updateMessage(MessageRole.Assistant, streamingText)
                            res.usage?.let { finalUsage ->
                                chatState.report.value = finalUsage.extra?.asTextLabel() ?: ""
                            }
                            if (finishReasonLength) {
                                streamingText += " [output truncated due to context length limit...]"
                                chatState.updateMessage(MessageRole.Assistant, streamingText)
                            }
                        });
                    */
                //}
                for (res in responses) {
                    chatState?.let { state ->  // Safely handle nullable chatState
                         for (choice in res.choices) {
                            // Handle streaming content
                            choice.delta.content?.let { content ->
                                streamingText += content.asText()
                                finalText += content.asText()
                            }

                            // Check finish reason
                            choice.finish_reason?.let { finishReason ->
                                if (finishReason == "length") {
                                    finishReasonLength = true
                                }
                            }

                            // Update message
                            //state.updateMessage(MessageRole.Assistant, streamingText)

                            // Handle usage reporting
                            res.usage?.let { finalUsage ->
                                state.report.value = finalUsage.extra?.asTextLabel() ?: ""
                            }

                            // Add truncation message if needed
//                            if (finishReasonLength) {
//                                streamingText += " [output truncated due to context length limit...]"
//                                //state.updateMessage(MessageRole.Assistant, streamingText)
//                            }
                        }
                    }
                }
                // 최종 텍스트 업데이트
                if (finishReasonLength) {
                    finalText += " [output truncated due to context length limit...]"
                }
                //chatState.updateMessage(MessageRole.Assistant, finalText)
                Log.d("SendMessageView", "finalText: $finalText")
            }
            //}
*/
//            val query_command = "subject: $queryText"
//            Log.d("QAUseCase", "Query Command: $query_command")
//            // Use lifecycleScope to launch a coroutine
//            //lifecycleScope.launch {
//            coroutineScope.launch {
//
//                val messageIds = gmailHelper.query_to_idlist(query_command)
//                Log.d("QAUseCase", "Message IDs found: $messageIds")
//                if (messageIds.isNotEmpty()) {
//                    gmailHelper.loadMails_idlist(messageIds)
//                    Log.d("QAUseCase", "Mails loaded successfully")
//
//                    Toast.makeText(context, "Mails loaded successfully", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            }

/*
            // Store the question in ViewModel
            viewModel.questionState.value = text

            // Clear input field and hide the keyboard
            questionText = ""
            keyboardController?.hide()

            // 입력 필드 초기화 및 키보드 숨기기
            text = ""
            keyboardController?.hide()
  */
            // Set loading state
            //chatViewviewModelModel.isGeneratingResponseState.value = true

            // Call the LLM service to convert natural language to a Gmail query
            /*
            viewModel.qaUseCase.buildGmailQueryWithLLM(
                queryText = chatViewModel.questionState.value, // Pass the question text to LLM
                onResponse = { queryResult ->
                    // Handle the result from the LLM

                    // Stop the loading state
                    chatViewModel.isGeneratingResponseState.value = false

                    // Update the ViewModel with the response
                    chatViewModel.responseState.value = queryResult.response
                    //Log.d(" chatViewModel.qaUseCase.buildGmailQueryWithLLM : ", "queryResult.response : ${queryResult.response}")
                    chatViewModel.retrievedContextListState.value = queryResult.context
                    chatViewModel.responseTypeState.value = ResponseType.QUERY_RESULT

                    // Optionally, log the result
                    Log.d("LLMQuery", "LLM Query Result1: ${queryResult.response}")

                    // rememberCoroutineScope()로 코루틴 실행
                    coroutineScope.launch {
                        try {
                            // LLM의 결과로 Gmail에서 메일 ID 리스트를 가져옴
                            Log.d("LLMQuery", "LLM Query Result2: ${queryResult.response}")

                            val messageIds = gmailHelper.query_to_idlist(queryResult.response)
                            Log.d("QAUseCase", "Message IDs found: $messageIds")

                            // 메일 ID가 존재하면 메일을 로드
                            if (messageIds.isNotEmpty()) {
                                gmailHelper.loadMails_idlist(messageIds)
                                Log.d("QAUseCase", "Mails loaded successfully")

                                // UI 업데이트
                                Toast.makeText(context, "Mails loaded successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No mails found for the query.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("QAUseCase", "Error loading mails", e)
                            //Toast.makeText(context, "Failed to load mails", Toast.LENGTH_SHORT).show()
                        } finally {
                            // 로딩 상태 해제
                            chatViewModel.isGeneratingResponseState.value = false
                        }
                    }

                }
            )
             */
        }) {
            //Icon(imageVector = Icons.Default.Search, contentDescription = "Submit query")
            Icon(
                imageVector = Icons.Default.Email, // 이메일 아이콘
                contentDescription = "Load Mails"
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(text = "Input Query for WV_MAIL") },
            modifier = Modifier
                .weight(9f),
        )
        //val context = LocalContext.current  // 현재 Compose의 Context 가져오기

        IconButton(
            modifier = Modifier.background(Color.Blue, CircleShape),
            onClick = {
                if (text.trim().isEmpty()) {
                    Toast.makeText(context, "Enter a query to execute", Toast.LENGTH_LONG).show()
                    return@IconButton
                }

                // 쿼리 처리 여부에 따라 shouldProcess 값 설정
                //val shouldProcess = true // 또는 false로 설정 가능
                val shouldProcess = false // 또는 false로 설정 가능

                var queryText  = text

                //** CoroutineScope를 사용하여 비동기 호출
/*
                CoroutineScope(Dispatchers.Main).launch {
                    val queryResult = LLAMAOnDAPI(context).generateResponse(text, shouldProcess)

                    if (queryResult.isNullOrEmpty()) {
                        Toast.makeText(context, "Failed to generate response", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        // 쿼리 결과를 화면에 표시
                        Toast.makeText(context, "Generated Query: $queryResult", Toast.LENGTH_LONG)
                            .show()
                        Log.d("QueryResult", "Generated Query: $queryResult")
                    }
                }
*/
                //2
                coroutineScope.launch {
                    try {
                        // `await()`을 사용하여 `requestGenerateSQ`의 결과를 비동기적으로 기다림
                        val result = chatState.requestGenerateSQ(text).await()
                        Log.d("SendMessageView", "첫 번째 결과: $result")
                        result.replace("`", "")  // ' 제거
                        Log.d("SendMessageView", "첫 번째 결과 processing : $result")

                        if (result.isNullOrEmpty()) {
                            Log.e("SendMessageView", "첫 번째 결과가 null입니다.")
                            return@launch
                        }

                        Log.d("SendMessageView", "초기 text 값 2: $text")
                        Log.d("SendMessageView", "초기 text 값 local 2: $queryText") // currentText 사용

                        // 결과가 null이 아닌 경우에만 requestGenerateEmbeddingVector 호출
                        chatState.requestGenerateEmbeddingVector(result, gmailHelper)

                    } catch (e: Exception) {
                        Log.e("SendMessageView", "에러 발생: ${e.message}")
                    }
                }

                //chatViewModel.questionState.value = questionText

                //questionText = ""
                //chatViewModel.isGeneratingResponseState.value = true
                Log.d("SendMessageView", "query text : $text")
                //viewModel.qaUseCase.getAnswer
                Log.d("SendMessageView", "getAnswer START: $text")
                viewModel.qaUseCase.getAnswer_base(
                    text, //chatViewModel.questionState.value,
                    context.getString(R.string.prompt_1),
                    onResponse = { queryResult ->
                        Log.d(" chatViewModel.qaUseCase.getAnswer : ", "QueryResult: ${queryResult.response}")
                    },
                    onOnDeviceResponse = { onDeviceQueryResult ->
                        Log.d(" chatViewModel.qaUseCase.getAnswer : ", "OnDeviceQueryResult: ${onDeviceQueryResult.responses}")
                    }
                )
                Log.d("SendMessageView", "getAnswer END: $text")

                /*
                viewModel.qaUseCase.getAnswer(
                    text, //chatViewModel.questionState.value,
                    context.getString(R.string.prompt_1),
                    onResponse = { queryResult ->
                        // QueryResult에 대한 처리 로직
                        //chatViewModel.isGeneratingResponseState.value = false
                        //chatViewModel.responseState.value = queryResult.response
                        //chatViewModel.retrievedContextListState.value = queryResult.context
                        //chatViewModel.responseTypeState.value = ResponseType.QUERY_RESULT
                        Log.d(" chatViewModel.qaUseCase.getAnswer : ", "QueryResult: ${queryResult.response}")
                    },
                    onOnDeviceResponse = { onDeviceQueryResult ->
                        // OnDeviceQueryResult에 대한 처리 로직
                        //chatViewModel.isGeneratingResponseState.value = false
                           //chatViewModel.responseState.value = onDeviceQueryResult.responses
                        //chatViewModel.responseOnDeviceState.value = onDeviceQueryResult.responses
                        //chatViewModel.retrievedContextListState.value = onDeviceQueryResult.context
                        //chatViewModel.responseTypeState.value = ResponseType.ON_DEVICE_RESULT
                        Log.d(" chatViewModel.qaUseCase.getAnswer : ", "OnDeviceQueryResult: ${onDeviceQueryResult.responses}")
                    }
                )
                 */
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Send query",
                tint = Color.White
            )
        }


        IconButton(
            onClick = {
                localFocusManager.clearFocus()
                Log.d("SendMessageView", "query text : $text")
                val currentText = text // text 값을 로컬 변수에 저장
                Log.d("SendMessageView", "초기 querytext 값 LOCAL 1: $currentText")

                chatState.requestGenerateSQ_20241030(text).thenCompose { result ->
                    Log.d("SendMessageView", "첫 번째 결과: $result")
                    // result 값이 null인지 확인
                    if (result == null) {
                        Log.e("SendMessageView", "첫 번째 결과가 null입니다.")
                    }
                    // result를 사용하여 비동기적으로 requestGenerateREACT 호출
                    //chatState.requestGenerateREACT(text, result)
                    Log.d("SendMessageView", "requestGenerateREACT")
                    Log.d("SendMessageView", "초기 text 값 2: $text")
                    Log.d("SendMessageView", "초기 text 값 local 2: $currentText") // currentText 사용

                    //chatState.requestGenerateREACT(text)
                    //chatState.requestGenerateREACT(currentText, result)
                    chatState.requestGenerateREACTPostOnD(currentText)
                }/*.thenCompose{ firstReactResult ->
                    // requestGenerateREACT의 결과를 처리
                    Log.d("SendMessageView", "두번째 파이프라인 결과: $firstReactResult")

                    // firstReactResult를 사용하여 비동기적으로 requestGenerateREACT 호출
                    Log.d("SendMessageView", "requestGenerateREACTPostOnD")
                    Log.d("SendMessageView", "초기 text 값 local 3: $currentText") // currentText 사용

                    //chatState.requestGenerateREACT(text)
                    chatState.requestGenerateREACTPostOnD(currentText)

                }*/.thenAccept { secondReactResult ->
                    // requestGenerateREACT의 결과를 처리
                    Log.d("SendMessageView", "세번째 파이프라인 결과: $secondReactResult")
                }.exceptionally { e ->
                    Log.e("SendMessageView", "에러 발생: ${e.message}")
                    null
                }
                /*
                viewModel.viewModelScope.launch {
                    try {
                        // 첫 번째 요청 실행 및 결과 저장
                        val firstResult = chatState.requestGenerateSQ(text)
                        Log.d("SendMessageView", "첫 번째 결과: $firstResult")

                        // 첫 번째 결과를 기반으로 두 번째 요청 실행
                        val secondResult = chatState.requestGenerateREACT(text, firstResult)
                        Log.d("SendMessageView", "두 번째 결과: $secondResult")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                viewModel.viewModelScope.launch {
                    try {
                        // 첫 번째 요청 수행 후 두 번째 요청 순차적으로 실행
                        //chatState.requestGenerate(text) // 첫 번째 요청
                        //Log.d("SendMessageView", "requestGenerate")
                        //chatState.requestGenerateSQ(text) // 첫 번째 요청
                        //chatState.requestGenerateREACT(text) // 첫 번째 요청이 끝난 후 두 번째 요청

                        // 첫 번째 요청 실행 및 결과 저장
                        //val firstResult = chatState.requestGenerateSQ(text)
                        //val firstResult = chatState.requestGenerateSQ(text).join() // String 값을 동기적으로 가져옴

                        chatState.requestGenerateSQ(text).thenApply { result ->
                            // result는 String 타입입니다.
                            val firstResult = result
                            Log.d("SendMessageView", "첫 번째 결과: $firstResult")

                            // 첫 번째 결과를 기반으로 두 번째 요청 실행
                            //val secondResult = chatState.requestGenerateREACT(text, firstResult)
                            val secondResult = chatState.requestGenerateREACT(text)
                            Log.d("SendMessageView", "두 번째 결과: $secondResult")
                        }.exceptionally { e ->
                            e.printStackTrace()
                            null
                        }

                        //Log.d("SendMessageView", "첫 번째 결과: $firstResult")

                        // 첫 번째 결과를 기반으로 두 번째 요청 실행
                        //val secondResult = chatState.requestGenerateREACT(text, firstResult)
                        //Log.d("SendMessageView", "두 번째 결과: $secondResult")

                        //Log.d("SendMessageView", "requestGenerateREACT")
                    } catch (e: Exception) {
                        // 예외 처리
                        e.printStackTrace()
                    }
                }
                */
                text = ""
            },
            modifier = Modifier
                .aspectRatio(1f)
                .weight(1f),
            enabled = (text != "" && chatState.chatable())
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "send message",
            )
        }
    }
}
/*
@ExperimentalMaterial3Api
@Composable
//fun SendMessageView(chatState: AppViewModel.ChatState) {
fun SendMessageViewSQ(viewModel: AppViewModel, chatState: AppViewModel.ChatState) {

        val localFocusManager = LocalFocusManager.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .fillMaxWidth()
            .padding(bottom = 5.dp)
    ) {
        var text by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(text = "Input Query for WV_MAIL") },
            modifier = Modifier
                .weight(9f),
        )
        IconButton(
            onClick = {
                localFocusManager.clearFocus()
                viewModel.viewModelScope.launch {
                    try {
                        // 첫 번째 요청 수행 후 두 번째 요청 순차적으로 실행
                        //chatState.requestGenerate(text) // 첫 번째 요청
                        //Log.d("SendMessageView", "requestGenerate")
                        chatState.requestGenerateSQ(text) // 첫 번째 요청
//                        chatState.requestGenerateREACT(text) // 첫 번째 요청이 끝난 후 두 번째 요청
                        Log.d("SendMessageView", "requestGenerateREACT")
                    } catch (e: Exception) {
                        // 예외 처리
                        e.printStackTrace()
                    }
                }

                text = ""
            },
            modifier = Modifier
                .aspectRatio(1f)
                .weight(1f),
            enabled = (text != "" && chatState.chatable())
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "send message",
            )
        }
    }
}
 */
@Preview
@Composable
fun MessageViewPreviewWithMarkdown() {
    MessageView(
        messageData = MessageData(
            role = MessageRole.Assistant, text = """
# Sample  Header
* Markdown  
* [Link](https://example.com)  
<a href="https://www.google.com/">Google</a>
"""
        )
    )
}
