package com.lamrnd.docqa.ui.viewModels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.lamrnd.docqa.GmailHelper
import com.lamrnd.docqa.data.RetrievedContext
import com.lamrnd.docqa.domain.QAUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    val qaUseCase: QAUseCase,
    //val gmailHelper: GmailHelper,
) : ViewModel() {

    // qaUseCase를 통해 documentsUseCase에 접근
    val documentsUseCase = qaUseCase.documentsUseCase
    //val gmailHelper
    //val gmailHelper = gmailHelper
    val questionState = mutableStateOf("")
    val responseState = mutableStateOf("")
    val responseOnDeviceState = mutableStateOf("")
    val isGeneratingResponseState = mutableStateOf(false)
    val retrievedContextListState = mutableStateOf(emptyList<RetrievedContext>())
    // Add a new state to track the current type of response
    val responseTypeState = mutableStateOf<ResponseType>(ResponseType.NONE)

    // Method to handle invalid responses like "<bos><bos><bos>"
    fun handleInvalidResponse() {
        responseOnDeviceState.value = "No Valid Result" // Set the message to be shown in the chat
        isGeneratingResponseState.value = false
        responseTypeState.value = ResponseType.ON_DEVICE_RESULT
    }
}



sealed class ResponseType {
    object NONE : ResponseType()
    object QUERY_RESULT : ResponseType()
    object ON_DEVICE_RESULT : ResponseType()
}
