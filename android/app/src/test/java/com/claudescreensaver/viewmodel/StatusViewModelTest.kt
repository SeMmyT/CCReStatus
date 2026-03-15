package com.claudescreensaver.viewmodel

import com.claudescreensaver.data.models.AgentState
import com.claudescreensaver.data.network.ConnectionState
import com.claudescreensaver.data.network.SseClient
import org.junit.Assert.*
import org.junit.Test

class StatusViewModelTest {

    @Test
    fun `initial UI state is disconnected`() {
        val vm = StatusViewModel(SseClient())
        val state = vm.uiState.value
        assertEquals(AgentState.IDLE, state.agentStatus.state)
        assertEquals(ConnectionState.DISCONNECTED, state.connectionState)
    }
}
