package com.icinema.pages.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerReducerTest {
    private val reducer = PlayerReducer()

    @Test
    fun `stores error detail and clears it with the next error reset`() {
        val failed = reducer.reduce(
            PlayerContract.UiState(),
            PlayerContract.Mutation.ErrorChanged(
                message = "当前线路播放失败",
                detail = "PlaybackException: response code 503"
            )
        )

        assertEquals("当前线路播放失败", failed.error)
        assertEquals("PlaybackException: response code 503", failed.errorDetail)

        val reset = reducer.reduce(failed, PlayerContract.Mutation.ErrorChanged(null))

        assertNull(reset.error)
        assertNull(reset.errorDetail)
    }
}
