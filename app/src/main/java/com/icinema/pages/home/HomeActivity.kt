package com.icinema.pages.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icinema.pages.category.CategoryActivity
import com.icinema.pages.player.PlayerActivity
import com.icinema.ui.theme.iCinemaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, HomeActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            iCinemaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val homeViewModel = viewModel<HomeViewModel>()
                    val categoryEditorLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        val changed = result.data?.getBooleanExtra(CategoryActivity.EXTRA_CHANGED, false) == true
                        if (result.resultCode == RESULT_OK && changed) {
                            homeViewModel.reloadCategories()
                        }
                    }

                    HomeScreen(
                        viewModel = homeViewModel,
                        onVideoClick = { videoId ->
                            PlayerActivity.start(
                                context = this@HomeActivity,
                                videoId = videoId,
                                sourceKey = null,
                                episodeIndex = 0
                            )
                        },
                        onOpenCategoryEditor = {
                            categoryEditorLauncher.launch(CategoryActivity.start(this@HomeActivity))
                        }
                    )
                }
            }
        }
    }
}
