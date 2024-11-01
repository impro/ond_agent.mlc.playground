package ai.mlc.mlcchat

import ai.mlc.mlcchat.ui.theme.MLCChatTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.lamrnd.docqa.GmailHelper
import com.lamrnd.docqa.domain.DocumentsUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    //val lifecycleOwner = LocalLifecycleOwner.current

    private lateinit var gmailHelper: GmailHelper

    @Inject
    lateinit var documentsUseCase: DocumentsUseCase // DocumentsUseCase를 주입받음

    // ActivityResultLauncher를 선언합니다.
    private val accountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 결과를 GmailHelper로 전달합니다.
        gmailHelper.handleAccountPickerResult(result)
    }
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        gmailHelper = GmailHelper(documentsUseCase).apply {
//            setActivity(this@MainActivity) // Manually pass the activity
//
//        }

        // GmailHelper를 초기화하면서 Activity와 accountPickerLauncher를 전달합니다.
        gmailHelper = GmailHelper(
            activity = this,
            documentsUseCase = documentsUseCase,
            //accountPickerLauncher = accountPickerLauncher
        )

        // accountPickerLauncher를 설정합니다.
        gmailHelper.setAccountPickerLauncher(accountPickerLauncher)

        setContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                MLCChatTheme {
                    NavView(
                        //appViewModel = viewModel(),
                        lifecycleOwner = LocalLifecycleOwner.current,
                        gmailHelper = gmailHelper
                    )
                }
            }
        }
    }
}