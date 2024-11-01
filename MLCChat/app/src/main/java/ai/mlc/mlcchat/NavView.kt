package ai.mlc.mlcchat

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lamrnd.docqa.GmailHelper
import com.lamrnd.docqa.ui.screens.DocsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private lateinit var gmailHelper: GmailHelper

@ExperimentalMaterial3Api
@Composable
fun NavView(appViewModel: AppViewModel = viewModel(), lifecycleOwner: LifecycleOwner, gmailHelper: GmailHelper) {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { StartView(navController, appViewModel) }
        composable("docs") { DocsScreen(onBackClick = { navController.navigateUp() }) }
        composable("chat") {
            ChatView(navController, appViewModel.chatState, appViewModel,
                onOpenDocsClick = {navController.navigate("docs")},
                onInitMailClick = {initMailService(context, lifecycleOwner.lifecycleScope, gmailHelper) },
                onMailLoadClick = {loadMails(context, lifecycleOwner.lifecycleScope, gmailHelper) },
                gmailHelper = gmailHelper // gmailHelper 전달
            ) }

    }
}

// Gmail 서비스를 초기화하고 메일을 로드하는 함수
//private fun initMailService() {
private fun initMailService(context: Context, lifecycleScope: CoroutineScope, gmailHelper: GmailHelper) {

    Toast.makeText(context, "Loading Mails...", Toast.LENGTH_SHORT).show()

    // Use lifecycleScope to launch a coroutine
    lifecycleScope.launch {
        try {
            // Setup Gmail service asynchronously and ensure it's initialized
//            gmailHelper.setupGmailService {
//                // Once Gmail service is initialized, load mails
//                Toast.makeText(context, "Gmail Service Initialized", Toast.LENGTH_SHORT).show()
//            }
            gmailHelper.setupGmailService ()


            // Call loadMails to fetch emails after service setup
            //               lifecycleScope.launch {
            //                   Log.d("MainActivity", "Loading mails...")
            //                   delay(2000)  // 2초 지연
            //                   Log.d("MainActivity", "Loading mails...")
            //                   gmailHelper.loadMails()
            //                   Toast.makeText(this@MainActivity, "Mails loaded successfully", Toast.LENGTH_SHORT).show()
            //               }
        } catch (e: Exception) {
            // Handle any exceptions that occur during mail loading
            Toast.makeText(context, "Failed to load mails: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}


// Gmail 서비스를 초기화하고 메일을 로드하는 함수
private fun loadMails(context: Context, lifecycleScope: CoroutineScope, gmailHelper: GmailHelper) {
    Toast.makeText(context, "Loading Mails...", Toast.LENGTH_SHORT).show()

    var queryText = "AI Fellowship"
    Log.d("MainActivity", "Query Text: $queryText")

    val query_command = "subject: $queryText"
    Log.d("QAUseCase", "Query Command: $query_command")
    // Use lifecycleScope to launch a coroutine
    lifecycleScope.launch {
        val messageIds = gmailHelper.query_to_idlist(query_command)
        Log.d("QAUseCase", "Message IDs found: $messageIds")
        if (messageIds.isNotEmpty()) {
            gmailHelper.loadMails_idlist(messageIds)
            Log.d("QAUseCase", "Mails loaded successfully")

            Toast.makeText(context, "Mails loaded successfully", Toast.LENGTH_SHORT)
                .show()
        }
    }
}
