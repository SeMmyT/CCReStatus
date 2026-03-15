package com.claudescreensaver.data.models

import org.junit.Assert.*
import org.junit.Test

class AgentStatusTest {

    @Test
    fun `parse tool_call status from JSON`() {
        val json = """{"v":1,"status":"tool_call","session_id":"sess-1","instance_name":"dev-laptop","event":"PreToolUse","tool":"Bash","tool_input_summary":"npm test","message":"","requires_input":false,"ts":"2026-03-15T10:00:00Z"}"""
        val status = AgentStatus.fromJson(json)
        assertEquals(AgentState.TOOL_CALL, status.state)
        assertEquals("Bash", status.tool)
        assertEquals("npm test", status.toolInputSummary)
        assertFalse(status.requiresInput)
    }

    @Test
    fun `parse awaiting_input status`() {
        val json = """{"v":1,"status":"awaiting_input","session_id":"sess-1","instance_name":"dev-laptop","event":"Notification","tool":null,"tool_input_summary":"","message":"Permission needed","requires_input":true,"ts":"2026-03-15T10:00:00Z"}"""
        val status = AgentStatus.fromJson(json)
        assertEquals(AgentState.AWAITING_INPUT, status.state)
        assertTrue(status.requiresInput)
        assertEquals("Permission needed", status.message)
    }

    @Test
    fun `unknown status maps to THINKING`() {
        val json = """{"v":1,"status":"unknown_future_state","session_id":"sess-1","instance_name":"dev","event":"FutureEvent","tool":null,"tool_input_summary":"","message":"","requires_input":false,"ts":"2026-03-15T10:00:00Z"}"""
        val status = AgentStatus.fromJson(json)
        assertEquals(AgentState.THINKING, status.state)
    }

    @Test
    fun `DISCONNECTED sentinel has correct defaults`() {
        val d = AgentStatus.DISCONNECTED
        assertEquals(AgentState.IDLE, d.state)
        assertEquals("Not connected", d.message)
        assertNull(d.tool)
        assertFalse(d.requiresInput)
    }
}
