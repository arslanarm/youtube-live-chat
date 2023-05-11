package me.plony.livechat

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import me.plony.livechat.exceptions.FetchError
import me.plony.livechat.exceptions.LiveChatInitializationError
import me.plony.livechat.exceptions.StreamIsClosed
import me.plony.livechat.output.OutputStrategy
import me.plony.livechat.output.PlainOutputStrategy
import me.plony.livechat.serialization.ChatMessages
import me.plony.livechat.serialization.YoutubeConfiguration
import me.plony.livechat.timeout.DelayTimeoutStrategy
import me.plony.livechat.timeout.TimeoutStrategy
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun LiveChat(
    baseUrl: String,
    httpClient: HttpClient = LiveChat.createDefaultClient(),
    timeoutStrategy: TimeoutStrategy = DelayTimeoutStrategy()
) = LiveChat(baseUrl, PlainOutputStrategy(), httpClient, timeoutStrategy)

class LiveChat<T>(
    val baseUrl: String,
    val outputStrategy: OutputStrategy<T>,
    val httpClient: HttpClient = createDefaultClient(),
    val timeoutStrategy: TimeoutStrategy = DelayTimeoutStrategy(),
) {
    companion object {
        val INITIAL_DATA_REGEX =
            Regex("(?:window\\s*\\[\\s*[\"']ytInitialData[\"']\\s*]|ytInitialData)\\s*=\\s*(\\{.+?})\\s*;\\s*(?:var\\s+meta|</script|\\n)")
        val YOUTUBE_CFG_REGEX = Regex("ytcfg\\.set\\s*\\(\\s*(\\{.+?})\\s*\\)\\s*;")
        val SUBMENU_ITEMS_PATH = listOf(
            "contents",
            "twoColumnWatchNextResults",
            "conversationBar",
            "liveChatRenderer",
            "header",
            "liveChatHeaderRenderer",
            "viewSelector",
            "sortFilterSubMenuRenderer",
            "subMenuItems"
        )
        val CONTINUATION_INFO_PATH = listOf("continuation", "reloadContinuationData", "continuation")
        val JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        fun createDefaultClient() = HttpClient {
            install(ContentNegotiation) {
                json(JSON)
            }
        }
    }

    var continuationInfo: String by Delegates.notNull()
    var cfgData: YoutubeConfiguration by Delegates.notNull()

    suspend fun init() {
        // Get HTML Page of the stream
        val htmlPage = httpClient.get(baseUrl).bodyAsText()

        // To get live chat from innertube, api requires to have 'continuation' and 'innertubeContext'
        // Continuation is found in 'initialData' object in <script> tag
        // InnertubeContext is found in 'ytcfg' object in <script> tag

        // Find initial data
        val initialMatcher = INITIAL_DATA_REGEX.find(htmlPage)
        if (initialMatcher == null || initialMatcher.groupValues.isEmpty()) throw LiveChatInitializationError("Cannot find youtube live chat key, regex in the library is wrong")

        // Find ytcfg
        val cfgMatcher = YOUTUBE_CFG_REGEX.find(htmlPage)
        if (cfgMatcher == null || cfgMatcher.groupValues.isEmpty()) throw LiveChatInitializationError("Cannot find youtube configuration")

        // Deserialize initial data into JsonObject
        val initialJsonData = initialMatcher.groupValues[0].removePrefix("ytInitialData = ").removeSuffix(";</script")
        val initialData = JSON.decodeFromString(JsonObject.serializer(), initialJsonData)

        val cfgJsonData = cfgMatcher.groupValues[0].removePrefix("ytcfg.set(").removeSuffix(");")

        // Live chat is located in sub menus
        // If sub menus are empty -> no stream
        val subMenuItems = initialData.getByKeys(SUBMENU_ITEMS_PATH)?.jsonArray
        if (subMenuItems.isNullOrEmpty()) throw StreamIsClosed()

        // Extract continuation data from live chat
        continuationInfo = subMenuItems[1].jsonObject.getByKeys(CONTINUATION_INFO_PATH)?.jsonPrimitive?.content
            ?: throw LiveChatInitializationError("Cannot find first continuation info")
        cfgData = JSON.decodeFromString(cfgJsonData)

        outputStrategy.init(this)
    }

    suspend fun nextMessages(): T = outputStrategy.generateOutput()

    internal suspend fun fetchNextMessages(): List<ChatMessage>? {
        // Require them to be not null
        cfgData
        continuationInfo

        // Every call to live chat api returns a timeout that we should follow
        // Wait for the timeout that was given from the previous time
        timeoutStrategy.wait()

        // Call the API
        val apiUrl = "https://www.youtube.com/youtubei/v1/live_chat/get_live_chat?key=${cfgData.innertubeApiKey}"
        val response = httpClient.post(apiUrl) {
            setBody(
                Context(
                    cfgData.innertubeContext,
                    continuationInfo
                )
            )
            contentType(ContentType.Application.Json)
        }

        // Handle status codes. Sometimes, innertube can return 503
        if (response.status == HttpStatusCode.ServiceUnavailable)
            return null
        if (response.status != HttpStatusCode.OK)
            throw FetchError("Status code: ${response.status}")
        // Convert innertube api's data schemes to our scheme
        val chatMessageInfo = runCatching { response.body<ChatMessages>() }
            .onFailure { println(response.bodyAsText()) }
            .getOrThrow()
        val actions = chatMessageInfo.continuationContents.liveChatContinuation.actions ?: return null
        val chatMessages = mutableListOf<ChatMessage>()
        for (action in actions) {
            if (action.addChatItemAction == null) continue
            val renderer = action.addChatItemAction.item.liveChatTextMessageRenderer
                ?: continue
            val runs = renderer.message.runs
            // Text is a collection of 'runs' it is either text value, or emoji
            val text = buildString {
                for (run in runs) {
                    if (run.text.isNotBlank()) {
                        append(run.text)
                    } else if (run.emoji != null) {
                        if (run.emoji.isCustomEmoji)
                            when (run.emoji.image.thumbnails.size) {
                                1 -> append(" ${run.emoji.image.thumbnails[0].url} ")
                                2 -> append(" ${run.emoji.image.thumbnails[1].url} ")
                            }
                        else
                            append(run.emoji.emojiId)
                    }
                }
            }
            chatMessages.add(
                ChatMessage(
                    renderer.authorName.simpleText,
                    text,
                    Instant.fromEpochMilliseconds(renderer.timestampUsec.toLong() / 1000)
                )
            )
        }
        // Find the next continuation. If there is none, that means stream is closed
        if (chatMessageInfo.continuationContents.liveChatContinuation.continuations.isEmpty())
            throw StreamIsClosed()
        val newContinuation = chatMessageInfo.continuationContents.liveChatContinuation.continuations[0]
        // Figure out the necessary timeout
        if (newContinuation.timedContinuationData == null) {
            this.continuationInfo = newContinuation.invalidationContinuationData!!.continuation
            val timeout = newContinuation.invalidationContinuationData.timeoutMs
            timeoutStrategy.setInvalidationTimeout(timeout.milliseconds)
        } else {
            this.continuationInfo = newContinuation.timedContinuationData.continuation
            val timeout =  newContinuation.timedContinuationData.timeoutMs
            timeoutStrategy.setTimedTimeout(timeout.milliseconds)
        }
        return chatMessages
    }

    @Serializable
    data class Context(
        val context: YoutubeConfiguration.InnertubeContext,
        val continuation: String
    )
}

private fun JsonObject.getByKeys(keys: List<String>): JsonElement? {
    require(keys.isNotEmpty())
    if (keys.size == 1)
        return get(keys[0])
    return get(keys[0])?.jsonObject?.getByKeys(keys.drop(1))
}
