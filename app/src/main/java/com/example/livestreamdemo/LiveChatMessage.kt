package com.example.livestreamdemo

import com.google.api.client.util.DateTime

data class LiveChatMessage(
    val id: String,
    val kind: String,
    val etag: String,
    val snippet: Snippet,
    val authorDetails: AuthorDetails
)

data class Snippet(
    val authorChannelId: String,
    val displayMessage: String,
    val hasDisplayContent: Boolean,
    val liveChatId: String,
    val publishedAt: DateTime,
    val textMessageDetails: TextMessageDetails,
    val type: String
)

data class TextMessageDetails(
    val messageText: String
)

data class AuthorDetails(
    val channelId: String,
    val channelUrl: String,
    val displayName: String,
    val profileImageUrl: String,
    val isVerified: Boolean,
    val isChatOwner: Boolean,
    val isChatSponsor: Boolean,
    val isChatModerator: Boolean,
)

