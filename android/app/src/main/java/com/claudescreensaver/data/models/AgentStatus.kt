package com.claudescreensaver.data.models

import org.json.JSONObject

enum class AgentState(val value: String) {
    IDLE("idle"),
    THINKING("thinking"),
    TOOL_CALL("tool_call"),
    AWAITING_INPUT("awaiting_input"),
    ERROR("error"),
    COMPLETE("complete");

    companion object {
        fun fromString(s: String): AgentState =
            entries.firstOrNull { it.value == s } ?: THINKING
    }
}

data class AgentStatus(
    val state: AgentState,
    val sessionId: String,
    val instanceName: String,
    val event: String,
    val tool: String?,
    val toolInputSummary: String,
    val message: String,
    val requiresInput: Boolean,
    val agentId: String? = null,
    val agentType: String? = null,
    val timestamp: String = "",
) {
    companion object {
        fun fromJson(json: String): AgentStatus {
            val obj = JSONObject(json)
            return AgentStatus(
                state = AgentState.fromString(obj.optString("status", "thinking")),
                sessionId = obj.optString("session_id", ""),
                instanceName = obj.optString("instance_name", ""),
                event = obj.optString("event", ""),
                tool = obj.optString("tool").takeIf { it.isNotEmpty() && it != "null" },
                toolInputSummary = obj.optString("tool_input_summary", ""),
                message = obj.optString("message", ""),
                requiresInput = obj.optBoolean("requires_input", false),
                agentId = obj.optString("agent_id").takeIf { it.isNotEmpty() && it != "null" },
                agentType = obj.optString("agent_type").takeIf { it.isNotEmpty() && it != "null" },
                timestamp = obj.optString("ts", ""),
            )
        }

        val DISCONNECTED = AgentStatus(
            state = AgentState.IDLE,
            sessionId = "",
            instanceName = "",
            event = "",
            tool = null,
            toolInputSummary = "",
            message = "Not connected",
            requiresInput = false,
        )
    }
}
