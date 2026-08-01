package com.icinema.cast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CastOverlayViewModel @Inject constructor(
    private val castController: CastController
) : ViewModel() {

    private val _state = MutableStateFlow(CastState())
    val state: StateFlow<CastState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            castController.state.collect { castState ->
                _state.value = castState
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            if (castController.state.value.isPlaying) {
                castController.pause()
            } else {
                castController.play()
            }
        }
    }

    fun stopCasting() {
        viewModelScope.launch {
            castController.stopCasting()
        }
    }

    fun refreshPlaybackPosition() {
        viewModelScope.launch {
            if (castController.state.value.isCasting) {
                castController.refreshPlaybackPosition()
            }
        }
    }
}
