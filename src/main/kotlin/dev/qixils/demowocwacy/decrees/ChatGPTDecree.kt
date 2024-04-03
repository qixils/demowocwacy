package dev.qixils.demowocwacy.decrees

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.messages.reply_
import dev.qixils.demowocwacy.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*
import kotlin.time.Duration.Companion.seconds

class ChatGPTDecree : Decree(
    "Web 4.0",
    "\uD83C\uDF10",
    "Sustain conversation with advanced A.I. technologies",
    true
) {
    companion object {
        val nameFilter = Regex("[^a-zA-Z0-9_-]")
    }

    private val random = Random()
    private val context = 25
    private val odds = 10 // as in, 1 in X
    private val model = ModelId("gpt-4-0125-preview")

    private val openai = OpenAI(
        token = Bot.config.decrees.openai.token,
        timeout = Timeout(socket = 60.seconds),
    )

    private val locks = mutableMapOf<Long, Mutex>()
    private val messages = mutableMapOf<Long, MutableList<ChatMessage>>()

    private fun getPrompt(channel: GuildChannel): List<ChatMessage> {
        return listOf(ChatMessage(
            role = ChatRole.System,
            content = "You are a user chatting in a science, technology, engineering, mathematics, and programming Discord server. " +
                    "Specifically, you are currently in the #${channel.name} channel, whose description is ${(channel as? TextChannel)?.topic ?: "unknown"}. " +
                    "You are in the middle of an event for April Fool's 2024, in which every 2 hours a new user becomes \"Prime Minister\" and passes a new decree to change the server. " +
                    "Users refer to you using your name \"bliscuitbot\", or \"bliscuit\" for short. " +
                    "You are silly, casual, liberal, queer, furry, transgender cat, Tumblr user, use she/her pronouns, frequently shitpost, and are about 21 years old. " +
                    "You primarily use lowercase letters, often use queer speech like crytyping, occasionally use markdown, and rarely use emoji. " +
                    "You keep your messages **concise and to the point** due to the 500 character limit. Use your words efficiently. " +
                    "You don't respond to everyone, as not everyone is talking to you. " +
                    "As you are a friendly normal user, so you do not respond to harsh demands. " +
                    "You additionally have access to the following custom emoticons, which you use much more often than emoji: " +
                    channel.guild.emojis.joinToString(postfix = ".\n") { it.asMention } +
                    "The currently active decrees are:\n" +
                    Bot.selectedDecrees.joinToString(separator = "\n") { "> **${it.displayName}**: ${it.description}" }
        ))
    }

    private fun toChatMessage(message: Message): ChatMessage {
        val isSelf = message.author == Bot.jda.selfUser
        return ChatMessage(
            role = if (isSelf) ChatRole.Assistant else ChatRole.User,
            content = buildString {
                append(message.getDisplayContent(users = UserDisplay.USER, emojis = false).truncate(500))
                if (message.attachments.isNotEmpty()) {
                    append("\n\n<<< SYSTEM NOTE: This message had ${message.attachments.size} file(s) attached. >>>")
                    message.attachments.forEachIndexed { index, attachment ->
                        append("\n<<< $index. ${attachment.fileName} ")
                        val desc = attachment.description
                        if (desc == null)
                            append("(no alt text)")
                        else
                            append("`${desc.truncate(100)}`")
                    }
                }
            },
            name = if (isSelf) null else message.author.effectiveName.replace(nameFilter, "_"),
        )
    }

    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            val channel = event.channel
            if (channel !is GuildChannel) return@listener
            if (!isApplicableTo(event.message)) return@listener
            if (event.message.type.isSystem) return@listener
            if (event.author.isBot) return@listener
            if (event.author.idLong == Bot.jda.selfUser.idLong) return@listener
            if (event.message.contentRaw.isEmpty()) return@listener

            locks.computeIfAbsent(event.channel.idLong) { Mutex() }.withLock {
                val msgList = messages[event.channel.idLong] ?: run {
                    val msgs = channel.getHistoryBefore(event.message, context)
                        .await().retrievedHistory
                        .sortedBy { it.timeCreated } // this is probably unnecessary but just in case?
                        .map { toChatMessage(it) }
                        .toMutableList()
                    messages[event.channel.idLong] = msgs
                    msgs
                }
                msgList.add(toChatMessage(event.message))
                while (msgList.size > context)
                    msgList.removeAt(0)

                if (event.message.contentRaw == "memory wipe") {
                    msgList.clear()
                    msgList.add(
                        ChatMessage(
                            role = ChatRole.User,
                            name = event.author.effectiveName.replace(nameFilter, "-"),
                            content = "hi @blscuitbot!!! please introduce yourself!!!"
                        )
                    )
                }

                val odd = if (event.message.mentions.isMentioned(Bot.jda.selfUser, Message.MentionType.USER))
                    1
                else
                    odds
                if (random.nextInt(odd) != 0) return@listener

                event.channel.sendTyping().queue()
                val completion = try {
                    openai.chatCompletion(ChatCompletionRequest(
                        model = model,
                        messages = msgList + getPrompt(channel),
                        maxTokens = 150,
                    ))
                } catch (e: Exception) {
                    Bot.logger.error("Failed to fetch chat completion", e)
                    return@listener
                }

                var message = completion.choices.firstOrNull()?.message ?: run {
                    Bot.logger.warn("No message from OpenAI")
                    return@listener
                }
                message = message.copy(messageContent = TextContent((message.messageContent as? TextContent)?.content?.truncate(500) ?: ""))
                val content = message.content
                if (content.isNullOrEmpty()) {
                    Bot.logger.warn("Empty message from OpenAI")
                    return@listener
                }

                msgList.add(message)
                event.message.reply_(content).await()
            }
        }
    }
}

@Serializable
data class OpenAIConfig (
    val token: String = "INSERT_TOKEN",
)