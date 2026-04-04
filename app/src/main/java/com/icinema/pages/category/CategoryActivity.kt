package com.icinema.pages.category

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icinema.ui.theme.iCinemaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CHANGED = "extra_changed"

        fun start(context: Context): Intent {
            return Intent(context, CategoryActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            iCinemaTheme {
                Surface {
                    CategoryScreen(
                        viewModel = viewModel<CategoryViewModel>(),
                        onSaved = { changed ->
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(EXTRA_CHANGED, changed)
                            )
                            finish()
                        }
                    )
                }
            }
        }
    }
}
