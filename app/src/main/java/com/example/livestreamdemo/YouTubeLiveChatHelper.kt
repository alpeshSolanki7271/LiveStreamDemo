package com.example.livestreamdemo

import android.os.AsyncTask
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.LiveChatMessageSnippet

class YouTubeLiveChatHelper(
    private val credential: GoogleCredential
) {

    private val youtube: YouTube =
        YouTube.Builder(credential.transport, JacksonFactory(), credential)
            .setApplicationName("YourAppName").build()

    fun sendMessage(liveChatId: String, messageText: String) {
        SendMessageTask(youtube, liveChatId, messageText).execute()
    }

    private class SendMessageTask(
        private val youtube: YouTube,
        private val liveChatId: String,
        private val messageText: String
    ) : AsyncTask<Void, Void, com.google.api.services.youtube.model.LiveChatMessage>() {

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): com.google.api.services.youtube.model.LiveChatMessage? {
            val liveChatMessage = com.google.api.services.youtube.model.LiveChatMessage()
            val snippet = LiveChatMessageSnippet()
            snippet.liveChatId = liveChatId
            snippet.textMessageDetails =
                LiveChatMessageSnippet().textMessageDetails.setMessageText(messageText)
            liveChatMessage.snippet = snippet

            return try {
                youtube.liveChatMessages().insert(listOf("snippet"), liveChatMessage).execute()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}