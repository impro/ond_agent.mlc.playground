package com.lamrnd.docqa.ui.screens

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ConnectingAirports
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Email // 이메일 아이콘 추가
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
//import com.lamrnd.docqa.R
import ai.mlc.mlcchat.R
import com.lamrnd.docqa.data.QueryResult
import com.lamrnd.docqa.ui.theme.DocQATheme
import com.lamrnd.docqa.ui.viewModels.ChatViewModel
import com.lamrnd.docqa.ui.viewModels.ResponseType
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.lifecycle.lifecycleScope
import com.lamrnd.docqa.GmailHelper
import com.lamrnd.docqa.domain.DocumentsUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

// GmailHelper 객체를 생성 (필요한 인스턴스 주입)
//@Inject
//lateinit var documentsUseCase: DocumentsUseCase // DocumentsUseCase를 주입받음
//lateinit var gmailHelper: GmailHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenDocsClick: (() -> Unit),
    onInitMailClick: (() -> Unit),
    onMailLoadClick: (() -> Unit), // 메일 로딩 버튼 클릭 시 동작하는 함수 추가
    gmailHelper: GmailHelper  // gmailHelper를 파라미터로 추가

) {
    DocQATheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    //title = { Text(text = "OnD RAG Based Answer", style = MaterialTheme.typography.headlineSmall) },
                    title = { Text(text = "Privacy Aware IntelliChat", 
                                style = MaterialTheme.typography.headlineSmall,
                                //style = MaterialTheme.typography.h6.copy(fontSize = 12.sp) // Change 12.sp to your desired size
                                //modifier = Modifier.size(100.dp), // Adjust size as needed
                                fontSize = 20.sp // Use your desired font size
                                ) },
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
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                Column {
                    QALayout(chatViewModel)
                    Spacer(modifier = Modifier.height(8.dp))
                    //QueryInput(chatViewModel)
                    QueryInput(chatViewModel, gmailHelper)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.QALayout(chatViewModel: ChatViewModel) {
    val question by remember { chatViewModel.questionState }
    val response by remember { chatViewModel.responseState }
    val ondResponse by remember { chatViewModel.responseOnDeviceState }
    val isGeneratingResponse by remember { chatViewModel.isGeneratingResponseState }
    val retrievedContextList by remember { chatViewModel.retrievedContextListState }
    val responseType by remember { chatViewModel.responseTypeState } // Track the response type
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .weight(1f),
    ) {
        if (question.trim().isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(75.dp),
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.LightGray
                )
                Text(
                    text = "Enter a query to see answers",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
        } else {
            LazyColumn {
                item {
                    Text(text = question, style = MaterialTheme.typography.headlineLarge)
                    if (isGeneratingResponse) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                item {
                    if (!isGeneratingResponse) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier =
                            Modifier
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            MarkdownText(
                                modifier = Modifier.fillMaxWidth(),
                                markdown = response,
                                style =
                                    TextStyle(
                                        color = Color.Black,
                                        fontSize = 14.sp,
                                    )
                            )
                            //if (responseType == ResponseType.ON_DEVICE_RESULT) {
                                // Display additional OnDeviceQueryResult information
                                Text(
                                    text = "On-Device Results : ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                // Here, you can add more details if available from OnDeviceQueryResult
                                // Display each response from the OnDeviceQueryResult
                                // Display each response from the OnDeviceQueryResult
                            /*
                                ondResponse.forEachIndexed { index, result ->
                                    Text(
                                        text = "Result #$index: $result",
                                        style = TextStyle(
                                            color = Color.Black,
                                            fontSize = 12.sp,
                                        ),
                                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                    )
                                }
                            */
                                //val allResults = ondResponse.joinToString(separator = " ")

                                Text(
                                    text = ondResponse,
                                    style = TextStyle(
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                    ),
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                )
                            /*
                                retrievedContextList.forEachIndexed { index, result ->
                                    Text(
                                        text = "Result #$index: ${result.context}",
                                        style = TextStyle(
                                            color = Color.Black,
                                            fontSize = 12.sp,
                                        ),
                                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                    )
                                }
                             */
                            //}
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        val sendIntent: Intent =
                                            Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, response)
                                                type = "text/plain"
                                            }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share the response",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Context", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                if (!isGeneratingResponse) {
                    items(retrievedContextList) { retrievedContext ->
                        Column(
                            modifier =
                            Modifier
                                .padding(8.dp)
                                .background(Color.Cyan, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "\"${retrievedContext.context}\"",
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                            Text(
                                text = retrievedContext.fileName,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
//@Inject
//lateinit var documentsUseCase: DocumentsUseCase // DocumentsUseCase를 주입받음


@Composable
//fun QueryInput(chatViewModel: ChatViewModel) {
fun QueryInput(chatViewModel: ChatViewModel, gmailHelper: GmailHelper) {
//fun QueryInput(chatViewModel: ChatViewModel) {

    var questionText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    //val gmailHelper = GmailHelper(activity = context as Activity, documentsUseCase = chatViewModel.documentsUseCase)
    //val gmailHelper = chatViewModel.gmailHelper
    //gmailHelper.setupGmailService();


    Log.d("QueryInput", "GmailHelper created")
    Log.d("QueryInput", "chatViewModel.documentsUseCase : ${chatViewModel.documentsUseCase}")
    //val gmailHelper = GmailHelper(activity = context as Activity, documentsUseCase = documentsUseCase)

    // Compose에서 사용할 수 있는 코루틴 스코프를 얻음
    val coroutineScope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically) {

        // Mail loading button
        IconButton(onClick = {
            if (questionText.trim().isEmpty()) {
                Toast.makeText(context, "Enter a query to execute", Toast.LENGTH_LONG).show()
                return@IconButton
            }

            // Store the question in ViewModel
            chatViewModel.questionState.value = questionText

            // Clear input field and hide the keyboard
            questionText = ""
            keyboardController?.hide()

            // Set loading state
            chatViewModel.isGeneratingResponseState.value = true

            // Call the LLM service to convert natural language to a Gmail query
            chatViewModel.qaUseCase.buildGmailQueryWithLLM(
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
        }) {
            //Icon(imageVector = Icons.Default.Search, contentDescription = "Submit query")
            Icon(
                imageVector = Icons.Default.Email, // 이메일 아이콘
                contentDescription = "Load Mails"
            )
        }

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            value = questionText,
            onValueChange = { questionText = it },
            shape = RoundedCornerShape(16.dp),
            colors =
                TextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    disabledTextColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
            placeholder = { Text(text = "Ask documents...") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            modifier = Modifier.background(Color.Blue, CircleShape),
            onClick = {
                keyboardController?.hide()
                if (!chatViewModel.qaUseCase.canGenerateAnswers()) {
                    Toast.makeText(context, "Add documents to execute queries", Toast.LENGTH_LONG)
                        .show()
                    return@IconButton
                }
                if (questionText.trim().isEmpty()) {
                    Toast.makeText(context, "Enter a query to execute", Toast.LENGTH_LONG).show()
                    return@IconButton
                }

                chatViewModel.questionState.value = questionText
                questionText = ""
                chatViewModel.isGeneratingResponseState.value = true
                chatViewModel.qaUseCase.getAnswer(
                    chatViewModel.questionState.value,
                    context.getString(R.string.prompt_1),
                    onResponse = { queryResult ->
                        // QueryResult에 대한 처리 로직
                        chatViewModel.isGeneratingResponseState.value = false
                        chatViewModel.responseState.value = queryResult.response
                        chatViewModel.retrievedContextListState.value = queryResult.context
                        chatViewModel.responseTypeState.value = ResponseType.QUERY_RESULT
                    },
                    onOnDeviceResponse = { onDeviceQueryResult ->
                        // OnDeviceQueryResult에 대한 처리 로직
                        chatViewModel.isGeneratingResponseState.value = false
                        //chatViewModel.responseState.value = onDeviceQueryResult.responses
                        chatViewModel.responseOnDeviceState.value = onDeviceQueryResult.responses
                        chatViewModel.retrievedContextListState.value = onDeviceQueryResult.context
                        chatViewModel.responseTypeState.value = ResponseType.ON_DEVICE_RESULT
                        Log.d(" chatViewModel.qaUseCase.getAnswer : ", "OnDeviceQueryResult: ${onDeviceQueryResult.responses}")
                    }
                )


                /*
                {

                    chatViewModel.isGeneratingResponseState.value = false
                    chatViewModel.responseState.value = it.responses
                    //chatViewModel.responseState.value = it.
                    chatViewModel.retrievedContextListState.value = it.context
                }
                 */
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Send query",
                tint = Color.White
            )
        }

    }
}
