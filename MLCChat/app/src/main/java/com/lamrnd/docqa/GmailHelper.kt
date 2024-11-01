package com.lamrnd.docqa

import android.accounts.AccountManager
import android.app.Activity
//import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.google.api.client.extensions.android.http.AndroidHttp
//import com.google.api.client.googleapis.auth.oauth2.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.lamrnd.docqa.domain.DocumentsUseCase
import com.lamrnd.docqa.domain.readers.Readers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import javax.inject.Inject

//class GmailHelper  @Inject constructor (private val activity: Activity,
class GmailHelper  (
    private val activity: Activity,
    private val documentsUseCase: DocumentsUseCase//, // DocumentsUseCase를 생성자에서 주입받음
    //private val accountPickerLauncher: ActivityResultLauncher<Intent>

) {
    // DocumentsUseCase를 주입받음
    //@Inject
    //lateinit var documentsUseCase: DocumentsUseCase
    //private lateinit var activity: Activity // Activity will be passed manually

    // This function will set the activity from the outside, when needed
    //fun setActivity(activity: Activity) {
    //   this.activity = activity
    //}

    companion object {
        private const val REQUEST_ACCOUNT_PICKER = 1001
        private const val REQUEST_AUTHORIZATION = 1002
    }

    private lateinit var credential: GoogleAccountCredential
    private var gmailService: Gmail? = null
    private lateinit var accountPickerLauncher: ActivityResultLauncher<Intent>

    fun setAccountPickerLauncher(launcher: ActivityResultLauncher<Intent>) {
        this.accountPickerLauncher = launcher
    }

    // OAuth 인증 설정
    fun setupGmailService() {
    //fun setupGmailService(onServiceInitialized: () -> Unit) {
        credential = GoogleAccountCredential.usingOAuth2(
            activity,
            listOf(GmailScopes.GMAIL_READONLY)
        )
        if (!::accountPickerLauncher.isInitialized) {
            throw IllegalStateException("ActivityResultLauncher is not initialized.")
        }
        //Log.d("GmailHelper", "Gmail Service Setup : " + credential)
        // 사용자 계정을 선택하도록 요청
        //activity.startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER)
        val accountPickerIntent = credential.newChooseAccountIntent()
        accountPickerLauncher.launch(accountPickerIntent)

        Log.d("GmailHelper", "Account picker started.")
        //onServiceInitialized()
    }
    fun handleAccountPickerResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data
            val accountName = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                credential.selectedAccountName = accountName
                initializeGmailService {
                    // 필요한 작업 수행
                    Log.d("GmailHelper", "Gmail Service initialized, loading mails.")
                }
            }
        } else {
            // 사용자 취소 또는 오류 처리
            Log.d("GmailHelper", "Authorization failed.")

        }
    }
    // onActivityResult에서 호출하여 Gmail 서비스를 설정
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null && data.extras != null) {
                Log.d("GmailHelper", "onActivityResult"+requestCode.toString())
                val accountName = data.getStringExtra("authAccount")
                Log.d("GmailHelper", "Account selected: $accountName")
                if (accountName != null) {
                    credential.selectedAccountName = accountName
                    Log.d("GmailHelper", "Account selected: $accountName")
                    //initializeGmailService()
                    initializeGmailService {
                        Log.d("GmailHelper", "Gmail Service initialized, loading mails.")
                        //loadMails
                        //onServiceInitialized()
                    }
                    // 권한 요청을 위해 추가로 authorization 인텐트를 시작할 수 있음
                    //activity.startActivityForResult(credential.newChooseAccountIntent(), REQUEST_AUTHORIZATION)

                }
            }
            REQUEST_AUTHORIZATION -> if (resultCode == Activity.RESULT_OK) {
                if (resultCode == Activity.RESULT_OK) {
                    //initializeGmailService()
                    initializeGmailService {
                        Log.d("GmailHelper", "Gmail Service initialized, loading mails.")
                        //loadMails()
                        //onServiceInitialized()
                    }

                    Log.d("GmailHelper", "Authorization successful.")
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Log.d("GmailHelper", "Authorization was canceled by the user.")
                    // Re-attempt authorization if needed
                } else {
                    Log.d("GmailHelper", "Authorization failed with code: $resultCode")
                    // Handle other result codes, possibly re-trigger the authorization flow
                }
                //Log.d("GmailHelper", "onActivityResult"+requestCode.toString())
                //initializeGmailService()
                //Log.d("GmailHelper", "Authorization successful.")
            }else {
                Log.d("GmailHelper", "Authorization failed.")
            }
        }
    }

    //private fun initializeGmailService() {
    private fun initializeGmailService(onServiceInitialized: () -> Unit) {
        if (credential.selectedAccountName == null) {
            Log.e("GmailHelper", "Account not set.")
            return
        }

        try {
            gmailService = Gmail.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Gmail API Compose").build()

            //Log.d("GmailHelper", "Gmail Service initialized successfully")
            //print gmailService
            Log.d("GmailHelper", "Gmail Service initialized successfully"+ gmailService.toString())

            // Service is initialized, call the callback
            onServiceInitialized()
        } catch (e: Exception) {
            Log.e("GmailHelper", "Failed to initialize Gmail Service: ${e.message}")
            // 권한 재요청
            activity.startActivityForResult(credential.newChooseAccountIntent(), REQUEST_AUTHORIZATION)
        }

/*
        Log.d("GmailHelper", "initializeGmailService")
        if (credential.selectedAccountName == null) {
            Log.e("GmailHelper", "Account not set.")
            return
        }
        gmailService = Gmail.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Gmail API Compose")
            .build()

        if (gmailService == null) {
            Log.e("GmailHelper", "initializeGmailService : Failed to initialize Gmail Service")
            Toast.makeText(activity, "Gmail Service not initialized", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("GmailHelper", "initializeGmailService : Gmail Service initialized successfully")
            Toast.makeText(activity, "Gmail Service Initialized", Toast.LENGTH_SHORT).show()
        }
        //Log.d("GmailHelper", "Gmail Service Initialized")
 */
    }


    suspend fun loadMails_all() {
        Log.d("GmailHelper", "---loadMails---")

        // Check if gmailService is initialized
        if (gmailService == null) {
            Log.d("GmailHelper", "gmailService is null")
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "Gmail Service not initialized", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Coroutine to handle mail loading
        withContext(Dispatchers.IO) {
            try {
                Log.d("GmailHelper", "Loading mails")

                // Fetch the messages
                val messageListResponse = gmailService!!.users().messages().list("me").execute()
                val messages = messageListResponse.messages

                Log.d("GmailHelper", "Messages: $messages")

                // Check if messages are null or empty
                if (messages.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "No mails found", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }
                // Log the number of messages
                Log.d("GmailHelper", "Number of mails fetched: ${messages.size}")

                // Loop through each message and read it
                for (message in messages) {
                    Log.d("GmailHelper", "Message ID: ${message.id}")
                    readMessage(message)
                }

                // If mails are successfully loaded, show a success message
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Mails loaded successfully", Toast.LENGTH_SHORT).show()
                }

            } catch (e: UserRecoverableAuthIOException) {
                // 사용자에게 권한 요청을 위해 인텐트 시작
                Log.e("GmailHelper", "UserRecoverableAuthIOException: Need remote consent")
                activity.startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    //Toast.makeText(activity, "Failed to load mails", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Helper function to recursively find the email body
    fun getEmailBody(parts: List<MessagePart>?): String {
        parts?.forEach { part ->
            when (part.mimeType) {
                "text/plain" -> {
                    val bodyData = part.body?.data
                    return if (bodyData != null) {
                        String(Base64.getUrlDecoder().decode(bodyData))
                    } else {
                        "No body found"
                    }
                }
                "text/html" -> {
                    // Optionally handle HTML body if necessary
                    val bodyData = part.body?.data
                    return if (bodyData != null) {
                        String(Base64.getUrlDecoder().decode(bodyData))
                    } else {
                        "No body found"
                    }
                }
                "multipart/alternative", "multipart/mixed" -> {
                    // Recursively check the parts within the multipart
                    return getEmailBody(part.parts)
                }
            }
        }
        return "No body found"
    }

    suspend fun loadMails() {
        Log.d("GmailHelper", "---loadMails---")

        // Check if gmailService is initialized
        if (gmailService == null) {
            Log.d("GmailHelper", "gmailService is null")
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "Gmail Service not initialized", Toast.LENGTH_SHORT).show()
            }
            return
        }

        //val messageIds = listOf("messageId1", "messageId2", "messageId3")
        //val messageIds = listOf("191c0f32b3c8c766", "191c09ad8ab62f74")
        val messageIds = listOf("191c0f32b3c8c766")

        //val query = "from:"
        //val query = "subject:재산세"
        //val query = "subject:AI Fellowship"
        // Coroutine to handle mail loading
        withContext(Dispatchers.IO) {
        //withContext(Dispatchers.Main) {
            try {
                Log.d("GmailHelper", "Loading mails with mailIds")


                // 메일 ID 리스트에서 각 메일을 가져옴
                for (messageId in messageIds) {
                    // 특정 메일 ID에 해당하는 메일 가져오기
                    val message = gmailService!!.users().messages().get("me", messageId).execute()

                    // 메일 정보를 로그로 출력
                    Log.d("GmailHelper", "Message ID: ${message.id}, Subject: ${message.payload.headers.find { it.name == "Subject" }?.value}")

                    // 메일 처리 (예: readMessage 호출)
                    //readMessage(message)
                    //readMessage_embedding(message)


                    // 메일의 본문을 추출
                    val emailSubject = message.payload.headers.find { it.name == "Subject" }?.value
                    val emailContent = getEmailBody(message.payload.parts)
                    /*
                    //val emailContent = message.snippet
                    // Get the full body of the message (assuming it's in the plain text part)
                    val bodyData = message.payload.parts?.find { it.mimeType == "text/plain" }?.body?.data

                    // Decode the body data if present
                    val emailContent = if (bodyData != null) {
                        String(Base64.getUrlDecoder().decode(bodyData))
                    } else {
                        "No body found"
                    }
                    */
                    Log.d("GmailHelper", "Email Subject: $emailSubject")
                    Log.d("GmailHelper", "Email Content: $emailContent")

                    // Convert emailSubject and emailContent to Strings first
                    val subjectString = emailSubject!!.toString()
                    val contentString = emailContent.toString()

                    // Concatenate them
                    val combinedString = subjectString + " " + contentString

                    // Convert the concatenated String to an InputStream
                    val inputStream = combinedString.byteInputStream()

                    //Log.d("GmailHelper", "Message ID: ${message.id}, Subject: ${message.payload.headers.find { it.name == "Subject" }?.value}")

                    // DocumentsUseCase를 사용하여 이메일 내용을 저장
                    //val inputStream = emailSubject!!.byteInputStream() + " " + emailContent.byteInputStream()
                    //val inputStream = emailSubject!!.toString().byteInputStream() + " " + emailContent.toString().byteInputStream()
                    //withContext(Dispatchers.Main) {
                        documentsUseCase.addDocument(
                            inputStream,
                            "Email_${message.id}",
                            Readers.DocumentType.EMAIL
                        )
                        Log.d("GmailHelper", "Email content successfully saved with ID: ${message.id}")
                    //}
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    //Toast.makeText(activity, "Failed to load mails", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

/*
    private suspend fun buildGmailQueryWithLLM(queryText: String, llmService: LLMService): String {
        // LLM을 통해 자연어를 분석하고 Gmail 검색 태그로 변환
        val prompt = """
        Convert the following natural language query into a Gmail search query:
        "$queryText"
        
        Gmail search query should use tags like 'from:', 'to:', 'subject:', 'has:attachment', etc.
    """.trimIndent()

        // LLM 서비스 호출하여 변환된 쿼리 받기
        val response = llmService.query(prompt)
        val generatedQuery = response.trim()

        // 변환된 쿼리 출력 및 반환
        Log.d("LLMQuery", "Generated Gmail Query: $generatedQuery")
        return generatedQuery
    }
*/

    // Function to query message IDs and then load and process the emails
    suspend fun queryAndLoadMails(query: String) {
        // Step 1: Query to get the list of message IDs
        val messageIds = query_to_idlist(query)

        // Check if messageIds is not empty
        if (messageIds.isEmpty()) {
            Log.d("GmailHelper", "No messages found for query: $query")
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "No messages found", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Step 2: Load and process the emails using the message IDs
        loadMails_idlist(messageIds)
    }

    suspend fun query_to_idlist(query: String): List<String> {
        Log.d("GmailHelper", "AF LLM Query Result : ---query_to_idlist---")

        // Check if gmailService is initialized
        if (gmailService == null) {
            Log.d("GmailHelper", "AF LLM Query Result : gmailService is null")
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "Gmail Service not initialized", Toast.LENGTH_SHORT).show()
            }
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d("GmailHelper", "AF LLM Query Result :  Query: $query")
                // Perform a query to fetch message IDs
                val messageList = gmailService!!.users().messages().list("me").setQ(query).execute()

                // Extract message IDs from the result
                val messageIds = messageList.messages?.map { it.id } ?: emptyList()

                Log.d("GmailHelper", "Query: $query, Found ${messageIds.size} messages")

                // Return the list of message IDs
                messageIds
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Failed to retrieve message IDs", Toast.LENGTH_SHORT).show()
                }
                emptyList() // Return an empty list in case of an error
            }
        }
    }

    //suspend fun loadMails_idlist() {
    suspend fun loadMails_idlist(messageIds: List<String>) {
            Log.d("GmailHelper", "---loadMails---")

        // Check if gmailService is initialized
        if (gmailService == null) {
            Log.d("GmailHelper", "gmailService is null")
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "Gmail Service not initialized", Toast.LENGTH_SHORT).show()
            }
            return
        }

        //val messageIds = listOf("messageId1", "messageId2", "messageId3")
        //val messageIds = listOf("191c0f32b3c8c766", "191c09ad8ab62f74")
        //val messageIds = listOf("191c0f32b3c8c766")

        //val query = "from:"
        //val query = "subject:재산세"
        //val query = "subject:AI Fellowship"
        // Coroutine to handle mail loading
        Log.d("GmailHelper", "Loading mails with mailIds")
        withContext(Dispatchers.IO) {
            //withContext(Dispatchers.Main) {
            try {
                Log.d("GmailHelper", "Loading mails with mailIds")


                // 메일 ID 리스트에서 각 메일을 가져옴
                for (messageId in messageIds) {
                    // 특정 메일 ID에 해당하는 메일 가져오기
                    val message = gmailService!!.users().messages().get("me", messageId).execute()

                    // 메일 정보를 로그로 출력
                    Log.d(
                        "GmailHelper",
                        "Message ID: ${message.id}, Subject: ${message.payload.headers.find { it.name == "Subject" }?.value}"
                    )

                    // 메일 처리 (예: readMessage 호출)

                    // 메일의 본문을 추출
                    //val emailSubject = message.payload.headers.find { it.name == "Subject" }?.value
                    //val emailContent = getEmailBody(message.payload.parts)

                    // 메일의 제목과 본문을 추출
                    val emailSubject = message.payload.headers.find { it.name == "Subject" }?.value
                        ?: "No subject found"
                    val emailContent = getEmailBody(message.payload.parts)// ?: "No content found"

                    /*
                    //val emailContent = message.snippet
                    // Get the full body of the message (assuming it's in the plain text part)
                    val bodyData = message.payload.parts?.find { it.mimeType == "text/plain" }?.body?.data

                    // Decode the body data if present
                    val emailContent = if (bodyData != null) {
                        String(Base64.getUrlDecoder().decode(bodyData))
                    } else {
                        "No body found"
                    }
                    */


                    // Convert emailSubject and emailContent to Strings first
                    //val subjectString = emailSubject!!.toString()
                    //val contentString = emailContent.toString()

                    //if (emailSubject.isNotEmpty() && emailContent.isNotEmpty()) {
                    //emailContent = No body found 는 스킵


                    try {
                        if (emailSubject.isNotEmpty() &&  emailContent != "No body found") {
                            Log.d("GmailHelper", "Email Subject: $emailSubject")
                            Log.d("GmailHelper", "Email Content: $emailContent")
                            //Log.d("GmailHelper", "isNotEmpty : Email Subject: $emailSubject")
                            val combinedString = "$emailSubject $emailContent"
                            // Convert the concatenated String to an InputStream
                            val inputStream = combinedString.byteInputStream()

                            documentsUseCase.addDocument(
                                inputStream,
                                "Email_${message.id}",
                                Readers.DocumentType.EMAIL
                            )
                            Log.d(
                                "GmailHelper",
                                "Email content successfully saved with ID: ${message.id}"
                            )
                        } else {
                            Log.d("GmailHelper", "Email with subject '$emailSubject' has no valid body content, skipping.")
                        }
                    } catch (e: Exception) {
                        Log.e("GmailHelper", "Error processing email with subject '$emailSubject': ${e.message}", e)
                        //Toast.makeText(context, "An error occurred while processing emails.", Toast.LENGTH_SHORT).show()
                    }



                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Failed to load mails", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    suspend fun loadMails_query() {
        Log.d("GmailHelper", "---loadMails---")

        // Check if gmailService is initialized
        if (gmailService == null) {
            Log.d("GmailHelper", "gmailService is null")
            withContext(Dispatchers.Main) {
                Toast.makeText(activity, "Gmail Service not initialized", Toast.LENGTH_SHORT).show()
            }
            return
        }

        //val messageIds = listOf("messageId1", "messageId2", "messageId3")
        val messageIds = listOf("191c0f32b3c8c766", "191c09ad8ab62f74")

        //val query = "from:"
        //val query = "subject:재산세"
        val query = "subject:AI Fellowship"
        // Coroutine to handle mail loading
        withContext(Dispatchers.IO) {
            try {
                Log.d("GmailHelper", "Loading mails")

                // Fetch the messages
                //val messageListResponse = gmailService!!.users().messages().list("me").execute()
                // Gmail API를 사용하여 특정 쿼리로 메일 검색
                val messageListResponse = gmailService!!.users().messages().list("me")
                    .setQ(query) // 검색 쿼리 설정
                    .execute()
                val messages = messageListResponse.messages

                Log.d("GmailHelper", "Messages: $messages")

                // Check if messages are null or empty
                if (messages.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "No mails found", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }
                // Log the number of messages
                Log.d("GmailHelper", "Number of mails fetched: ${messages.size}")

                // Loop through each message and read it
                for (message in messages) {
                    Log.d("GmailHelper", "Message ID: ${message.id}")
                    readMessage(message)
                }

                // If mails are successfully loaded, show a success message
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Mails loaded successfully", Toast.LENGTH_SHORT).show()
                }

            } catch (e: UserRecoverableAuthIOException) {
                // 사용자에게 권한 요청을 위해 인텐트 시작
                Log.e("GmailHelper", "UserRecoverableAuthIOException: Need remote consent")
                activity.startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Failed to load mails", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    // Gmail API를 사용하여 메일 가져오기
    //suspend fun loadMails() {
    /*
    suspend fun loadMails() {
        Log.d("GmailHelper", "---loadMails---")
        if (gmailService == null) {
            Log.d("GmailHelper", "gmailService is null")
            Toast.makeText(activity, "Gmail Service not initialized", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("GmailHelper", "gmailService is not null")
        //withContext(Dispatchers.IO) {
            try {
                Log.d("GmailHelper", "Loading mails")
                val messages = gmailService!!.users().messages().list("me").execute().messages
                Log.d("GmailHelper", "Messages: $messages")
                for (message in messages) {
                    Log.d("GmailHelper", "Message ID: ${message.id}")
                }
                messages?.let {
                    for (message in it) {
                        Log.d("GmailHelper", "Message: $message")
                        readMessage(message)
                    }
                }
                Log.d("GmailHelper", "Mails loaded successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Failed to load mails", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

     */

    // 개별 메시지 읽기
    private fun readMessage(message: Message) {
        val msg = gmailService!!.users().messages().get("me", message.id).execute()
        val subject = msg.payload.headers.find { it.name == "Subject" }?.value
        val body = msg.snippet
/*
        // Get the full body of the message (assuming it's in the plain text part)
        val bodyData = msg.payload.parts?.find { it.mimeType == "text/plain" }?.body?.data

        // Decode the body data if present
        val body = if (bodyData != null) {
            String(Base64.getUrlDecoder().decode(bodyData))
        } else {
            "No body found"
        }
*/
        // Print the subject and full body
        println("Subject: $subject")
        println("Body: $body")
/*
        // 메일의 본문을 추출
        //val emailSubject = message.payload.headers.find { it.name == "Subject" }?.value
        //val emailContent = message.snippet
        val emailSubject = subject
        val emailContent = bodyData

        Log.d("GmailHelper", "Email Subject: $emailSubject")
        Log.d("GmailHelper", "Email Content: $emailContent")

        // Convert emailSubject and emailContent to Strings first
        val subjectString = emailSubject!!.toString()
        val contentString = emailContent.toString()

        // Concatenate them
        val combinedString = subjectString + " " + contentString

        // Convert the concatenated String to an InputStream
        val inputStream = combinedString.byteInputStream()

        documentsUseCase.addDocument(inputStream, "Email_${message.id}", Readers.DocumentType.EMAIL)
*/
    }
}