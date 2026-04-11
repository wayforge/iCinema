package com.icinema.pages.home

import android.app.Activity
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
import com.icinema.pages.detail.DetailActivity
import com.icinema.pages.favorite.FavoriteActivity
import com.icinema.pages.history.HistoryActivity
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
                    val detailLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        val shouldRefresh = result.data?.getBooleanExtra(PlayerActivity.EXTRA_HOME_REFRESH, false) == true
                        if (result.resultCode == Activity.RESULT_OK && shouldRefresh) {
                            homeViewModel.refreshPlaybackDrivenSections()
                        }
                    }
                    val playerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        val shouldRefresh = result.data?.getBooleanExtra(PlayerActivity.EXTRA_HOME_REFRESH, false) == true
                        if (result.resultCode == Activity.RESULT_OK && shouldRefresh) {
                            homeViewModel.refreshPlaybackDrivenSections()
                        }
                    }
                    val snackbarHostState = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }

                    androidx.compose.runtime.LaunchedEffect(homeViewModel) {
                        homeViewModel.uiEffect.collect { effect ->
                            when (effect) {
                                is HomeContract.UiEffect.ShowError -> {
                                    snackbarHostState.showSnackbar(effect.message)
                                }
                                is HomeContract.UiEffect.ShowToast -> {
                                    snackbarHostState.showSnackbar(effect.message)
                                }
                                is HomeContract.UiEffect.OpenDetail -> {
                                    detailLauncher.launch(
                                        Intent(this@HomeActivity, DetailActivity::class.java).apply {
                                            putExtra(DetailActivity.EXTRA_VIDEO_ID, effect.videoId)
                                        }
                                    )
                                }
                                is HomeContract.UiEffect.OpenPlayer -> {
                                    playerLauncher.launch(
                                        PlayerActivity.createIntent(
                                            context = this@HomeActivity,
                                            videoId = effect.videoId,
                                            sourceKey = effect.sourceKey,
                                            episodeIndex = effect.episodeIndex
                                        )
                                    )
                                }
                            }
                        }
                    }

                    HomeScreen(
                        viewModel = homeViewModel,
                        snackbarHostState = snackbarHostState,
                        onVideoClick = { videoId ->
                            homeViewModel.handleIntent(HomeContract.UiIntent.OpenVideoDetail(videoId))
                        },
                        onContinueWatchingClick = { videoId, sourceKey, episodeIndex ->
                            homeViewModel.handleIntent(
                                HomeContract.UiIntent.OpenContinueWatching(
                                    videoId = videoId,
                                    sourceKey = sourceKey,
                                    episodeIndex = episodeIndex
                                )
                            )
                        },
                        onOpenCategoryEditor = {
                            categoryEditorLauncher.launch(CategoryActivity.start(this@HomeActivity))
                        },
                        onOpenHistory = {
                            HistoryActivity.start(this@HomeActivity)
                        },
                        onOpenFavorite = {
                            FavoriteActivity.start(this@HomeActivity)
                        }
                    )
                }
            }
        }
    }
}
