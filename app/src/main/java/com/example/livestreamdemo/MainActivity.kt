package com.example.livestreamdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.livestreamdemo.chat.Auth.JSON_FACTORY
import com.example.livestreamdemo.databinding.ActivityMainBinding
import com.example.livestreamdemo.databinding.CustomExoControllerLiveBinding
import com.example.livestreamdemo.youtubeExtractor.VideoMeta
import com.example.livestreamdemo.youtubeExtractor.YouTubeExtractor
import com.example.livestreamdemo.youtubeExtractor.YtFile
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.google.api.services.youtube.model.LiveChatMessageListResponse
import com.google.api.services.youtube.model.LiveChatMessageSnippet
import com.google.api.services.youtube.model.LiveChatTextMessageDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.security.GeneralSecurityException


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var customExoBinding: CustomExoControllerLiveBinding
    private var exoPlayer: ExoPlayer? = null
    private val liveVideoUrl = "https://www.youtube.com/watch?v=Mxp_4OT96QY"
    var mList = ArrayList<LiveChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private var credential: GoogleCredential? = null
    private val APPLICATION_NAME = "LiveVideoStreamDemo"
    private val jsonFactory: JsonFactory = JacksonFactory()
    private var youtubeService: YouTube? = null
    private var liveChatId: String = ""
    private var message: String = ""


    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.result
                credential = GoogleCredential().setAccessToken(account.idToken)

                CoroutineScope(IO).launch {
                    try {
                        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
                        youtubeService = YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                            .setApplicationName(APPLICATION_NAME).build()
                        val yt: com.google.api.services.youtube.model.LiveChatMessage =
                            com.google.api.services.youtube.model.LiveChatMessage()
                        val liveChatSnip: LiveChatMessageSnippet = LiveChatMessageSnippet()
                        liveChatSnip.liveChatId = liveChatId
                        val tvM: LiveChatTextMessageDetails = LiveChatTextMessageDetails()
                        tvM.messageText = message
                        liveChatSnip.setTextMessageDetails(tvM)
                        liveChatSnip.setType("textMessageEvent")
                        yt.setSnippet(liveChatSnip)

                        val response =
                            youtubeService!!.liveChatMessages().insert(listOf("snippet"), yt)
                                .execute()
                        Log.e("TAG", "response :$response: ")
                        println(response)
                    } catch (e: Exception) {

                        Log.e("TAG", "$e ")
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.clLiveVideoRoot.keepScreenOn = true
        AppConstants.isLiveVideo = true
        val videoControlView = binding.root.findViewById<View>(R.id.clExoControllerLive)
        customExoBinding = CustomExoControllerLiveBinding.bind(videoControlView)
        setCustomController()
        playLiveVideo()

        val videoId = getVideoIdFromUrl(liveVideoUrl)

        CoroutineScope(IO).launch {

            val youtube = YouTube.Builder(NetHttpTransport(),
                JacksonFactory(),
                HttpRequestInitializer { request -> }).setApplicationName("LiveVideoStreamDemo")
                .setYouTubeRequestInitializer(YouTubeRequestInitializer("AIzaSyBGmJHGXh4Ygum5NrfpQK8Mu6mH3QZmSuM"))
                .build()

            var nextPageToken: String? = null

            val message = binding.message.text.toString()

            do {
                val videoRequest = youtube.videos().list(listOf("liveStreamingDetails"))
                videoRequest.id = listOf(videoId)
                val listResponse = videoRequest.execute()
                val videoList = listResponse.items


                if (videoList.isNotEmpty()) {
                    val liveStreamingDetails = videoList[0].liveStreamingDetails
                    if (liveStreamingDetails != null) {
                        val liveChatId = liveStreamingDetails.activeLiveChatId
                        if (!liveChatId.isNullOrBlank()) {
                            val liveChatRequest =
                                youtube.liveChatMessages().list(liveChatId, listOf("snippet"))
                            liveChatRequest.part =
                                listOf("snippet") // Only fetch snippets for efficiency
                            liveChatRequest.pageToken = nextPageToken

                            val chatListResponse = liveChatRequest.execute()

                            val newMessages = mapLiveChatMessages(chatListResponse)
                            mList.addAll(newMessages)

                            CoroutineScope(Main).launch {
                                chatAdapter.updateList(mList)
                                binding.send.setOnClickListener {
                                    sendMessage(
                                        liveChatId1 = liveChatId,
                                        message1 = message,
                                        youtube = youtube
                                    )
                                }
                            }

                            nextPageToken = chatListResponse.nextPageToken
                        }
                    }
                }

                // Implement a reasonable delay to avoid overwhelming YouTube's API
                delay(5000) // Adjust delay based on your needs and fair usage considerations
            } while (nextPageToken != null)
        }

        chatAdapter = ChatAdapter(mList = mList, context = this)
        binding.chatList.adapter = chatAdapter
    }

    private fun sendMessage(liveChatId1: String, message1: String, youtube: YouTube) {

        liveChatId = liveChatId1
        message = message1

        CoroutineScope(IO).launch {
            try {
                val signInIntent = GoogleSignIn.getClient(
                    this@MainActivity, GoogleSignInOptions.DEFAULT_SIGN_IN
                ).signInIntent
                resultLauncher.launch(signInIntent)
            } catch (e: Exception) {
                print(e)
            }
        }
    }


    private fun mapLiveChatMessages(response: LiveChatMessageListResponse): List<LiveChatMessage> {
        Log.e("TAG", "mapLiveChatMessages: $response")

        return response.items?.mapNotNull { liveChatMessage ->
            LiveChatMessage(etag = liveChatMessage.etag,
                id = liveChatMessage.id,
                kind = liveChatMessage.kind,
                snippet = liveChatMessage.snippet?.let { snippet ->
                    Snippet(
                        authorChannelId = snippet.authorChannelId,
                        displayMessage = snippet.displayMessage ?: "",
                        hasDisplayContent = snippet.hasDisplayContent ?: false,
                        liveChatId = snippet.liveChatId,
                        publishedAt = snippet.publishedAt,
                        textMessageDetails = snippet.textMessageDetails?.let { textMessageDetails ->
                            TextMessageDetails(messageText = textMessageDetails.messageText ?: "")
                        }!!,
                        type = snippet.type
                    )
                }!!,
                authorDetails = liveChatMessage.authorDetails?.let { authorDetail ->
                    // Use non-null assertion for `authorDetail` within this scope
                    AuthorDetails(
                        channelId = authorDetail.channelId,
                        channelUrl = authorDetail.channelUrl,
                        displayName = authorDetail.displayName,
                        profileImageUrl = authorDetail.profileImageUrl,
                        isVerified = authorDetail.isVerified ?: false,
                        isChatOwner = authorDetail.isChatOwner ?: false,
                        isChatSponsor = authorDetail.isChatSponsor ?: false,
                        isChatModerator = authorDetail.isChatModerator ?: false
                    )
                } ?: run {
                    // Provide default values when `authorDetails` is null
                    Log.w(
                        "TAG",
                        "Author details missing for LiveChatMessage with id: ${liveChatMessage.id}"
                    )
                    AuthorDetails(
                        channelId = "",
                        channelUrl = "",
                        displayName = "",
                        profileImageUrl = "",
                        isVerified = false,
                        isChatOwner = false,
                        isChatSponsor = false,
                        isChatModerator = false
                    )
                })
        }?.toList() ?: emptyList()
    }


    /*private fun mapLiveChatMessages(response: LiveChatMessageListResponse): ArrayList<LiveChatMessage> {
        return response.items?.map { liveChatMessage ->
            LiveChatMessage(etag = liveChatMessage.etag,
                id = liveChatMessage.id,
                kind = liveChatMessage.kind,
                snippet = liveChatMessage.snippet.let { snippet ->
                    Snippet(
                        authorChannelId = snippet.authorChannelId,
                        displayMessage = snippet.displayMessage,
                        hasDisplayContent = snippet.hasDisplayContent,
                        liveChatId = snippet.liveChatId,
                        publishedAt = snippet.publishedAt,
                        textMessageDetails = snippet.textMessageDetails.let { textMessageDetails ->
                            TextMessageDetails(
                                messageText = textMessageDetails.messageText
                            )
                        },
                        type = snippet.type
                    )
                },
                authorDetails = liveChatMessage.authorDetails.let { authorDetail ->
                    AuthorDetails(
                        channelId = authorDetail.channelId ?: "",
                        channelUrl = authorDetail.channelUrl ?: "",
                        displayName = authorDetail.displayName ?: "",
                        profileImageUrl = authorDetail.profileImageUrl ?: "",
                        isVerified = authorDetail.isVerified ?: false,
                        isChatOwner = authorDetail.isChatOwner ?: false,
                        isChatSponsor = authorDetail.isChatSponsor ?: false,
                        isChatModerator = authorDetail.isChatModerator ?: false,
                    )
                }
            )
        }?.toCollection(ArrayList()) ?: arrayListOf()
    }*/


    private fun getVideoIdFromUrl(url: String?): String? {
        val YOUTUBE_VIDEO_REGEX =
            "(?:https?://)?(?:(?:www|m)\\.)?youtu(?:.be|be.com)/(?:watch\\?v=)?([^#&?]+)"
        val regex = Regex(YOUTUBE_VIDEO_REGEX)
        val match = regex.find(url ?: "")
        return match?.groupValues?.get(1)
    }

    private fun playLiveVideo() {
        exoPlayer =
            ExoPlayer.Builder(this).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(DefaultMediaSourceFactory(this).setLiveTargetOffsetMs(5000))
                .build()
        extractAndPlayVideo()
        binding.videoPlayer.player = exoPlayer
    }

    @SuppressLint("StaticFieldLeak")
    private fun extractAndPlayVideo() {
        object : YouTubeExtractor(this) {
            override fun onExtractionComplete(
                ytFiles: SparseArray<YtFile>?, videoMeta: VideoMeta?
            ) {
                if (ytFiles != null) {
                    var videoTag = 160
                    val audioTag = getAudioTag(ytFiles)
                    if (ytFiles[133] != null && ytFiles[133].format.height != -1) {
                        videoTag = 133
                    }
                    val youtubeAudioTag = ytFiles[audioTag].url
                    val youtubeVideoTag = ytFiles[videoTag].url
                    if (videoMeta != null) {
                        customExoBinding.titleToolbar.text = videoMeta.title
                    } else {
                        customExoBinding.titleToolbar.visibility = View.GONE
                    }
                    initializePlayer(youtubeVideoTag, youtubeAudioTag)
                } else {
                    toast("ytFiles is null, please Check")
                }
            }
        }.extract(liveVideoUrl, true, true)
    }

    private fun initializePlayer(youtubeVideoTag: String, youtubeAudioTag: String) {
        // This function is for Youtube only
        val videoSource = ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
            .createMediaSource(MediaItem.fromUri(youtubeVideoTag))
        val audioSource = ProgressiveMediaSource.Factory(DefaultHttpDataSource.Factory())
            .createMediaSource(MediaItem.fromUri(youtubeAudioTag))

        val dashVideoUri = Uri.parse(YouTubeExtractor.dashManifestUrl)
        val hlsVideoUri = Uri.parse(YouTubeExtractor.hlsManifestUrl)

        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
        val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(hlsVideoUri))
        // Create a dash media source pointing to a dash manifest uri.
        val mediaSource: MediaSource = DashMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(dashVideoUri))
        exoPlayer?.setMediaSource(hlsMediaSource)
        playVimeoVideo()
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(
            this, Util.getUserAgent(this, "yourApplicationName")
        )
        return ProgressiveMediaSource.Factory(dataSourceFactory, DefaultExtractorsFactory())
            .createMediaSource(MediaItem.fromUri(uri))
    }

    private fun setCustomController() {
        exoPlayer?.seekToDefaultPosition()
//        exoPlayer?.setPlaybackSpeed(2f)
        customExoBinding.btnExoSettings.setOnClickListener {
//            bottomSheetDialog?.show()
        }

        customExoBinding.btnBack.setOnClickListener {
            finish()
            exoPlayer?.pause()
            exoPlayer?.release()
        }

        customExoBinding.exoPlay.setOnClickListener {
            startPlayer()
            customExoBinding.exoPlay.visibility = View.GONE
            customExoBinding.exoPause.visibility = View.VISIBLE
        }

        customExoBinding.exoPause.setOnClickListener {
            pausePlayer()
            customExoBinding.exoPause.visibility = View.GONE
            customExoBinding.exoPlay.visibility = View.VISIBLE
        }

        customExoBinding.exoProgress.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
            }
        })
    }

    private fun startPlayer() {
        exoPlayer?.play()
    }

    private fun pausePlayer() {
        exoPlayer?.pause()
    }

    private fun playVimeoVideo() {
        exoPlayer?.apply {
            prepare()
            addPlayerListener()
            play()
            seekTo(0, 0)
        }
    }

    private fun addPlayerListener() {
        exoPlayer?.addAnalyticsListener(object : AnalyticsListener {

            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
                error: IOException,
                wasCanceled: Boolean
            ) {
                super.onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled)
                runOnUiThread {
                    toast("Exo player error in onLoadError()")
                    releasePlayer()
                }
            }

            override fun onPlayerError(
                eventTime: AnalyticsListener.EventTime, error: PlaybackException
            ) {
                super.onPlayerError(eventTime, error)
                runOnUiThread {
                    toast("Exo player error in onPlayerError()")
                    releasePlayer()
                }
            }
        })
    }

    private fun releasePlayer() {
        exoPlayer?.let {
            exoPlayer!!.release()
            exoPlayer = null
        }
    }

    fun getAudioTag(ytFiles: SparseArray<YtFile>): Int {
        // check this link for audio tags info - https://prnt.sc/GXA84ZNFsxKv
        val numbers = listOf(256, 141, 251)
        var x = 140
        for (i in numbers.indices) {
            if (ytFiles[numbers[i]] != null && ytFiles[numbers[i]].format.height != -1) {
                x = numbers[i]
                break
            }
        }
        return x
    }

    fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}