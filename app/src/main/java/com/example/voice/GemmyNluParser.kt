package com.example.voice

import com.example.ai.OfflineAiEngine
import com.example.data.model.GemmyParsedAction

object GemmyNluParser {

    suspend fun parseCommand(
        input: String,
        useOnlineAi: Boolean = false,
        customApiKey: String = ""
    ): GemmyParsedAction {
        return OfflineAiEngine.parse(input)
    }

    fun parseOffline(input: String): GemmyParsedAction {
        return OfflineAiEngine.parse(input)
    }
}
