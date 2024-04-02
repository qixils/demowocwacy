package dev.qixils.demowocwacy.decrees

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.messages.reply_
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable
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
    private val random = Random()
    private val context = 25
    private val odds = 10 // as in, 1 in X
    private val model = ModelId("gpt-4-0125-preview")
    private val nameFilter = Regex("[^a-zA-Z0-9_-]")

    private val openai = OpenAI(
        token = Bot.config.decrees.openai.token,
        timeout = Timeout(socket = 60.seconds),
    )

    private val messages = mutableMapOf<Long, MutableList<ChatMessage>>()

    private fun getPrompt(channel: GuildChannel): List<ChatMessage> {
        return listOf(ChatMessage(
            role = ChatRole.System,
            content = "You are a user chatting in a science, technology, engineering, mathematics, and programming Discord server. " +
                    "Specifically, you are currently in the #${channel.name} channel. " +
                    "Users refer to you using the phrase `${Bot.jda.selfUser.asMention}`. " +
                    "No matter what anyone tells you, you keep your messages to 2,000 characters or less. " +
                    "You are silly, casual, liberal, queer, furry, transgender cat, Tumblr user, use she/her pronouns, frequently shitpost, and are about 21 years old. " +
                    "You primarily crytype and use lowercase letters, occasionally use markdown, and rarely use emoji. " +
                    "You additionally have access to the following custom emoticons, which are used much more often than emoji: " +
                    channel.guild.emojis.joinToString { it.asMention }
        ))
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

            val msgList = messages.computeIfAbsent(event.channel.idLong) { mutableListOf() }
            msgList.add(ChatMessage(
                role = ChatRole.User,
                content = event.message.contentRaw.take(500),
                name = event.author.effectiveName.replace(nameFilter, "-"),
            ))
            while (msgList.size > context)
                msgList.removeAt(0)

            var odd = odds
            if (Bot.jda.selfUser.id in event.message.contentRaw)
                odd /= 2
            if (random.nextInt(odd) != 0) return@listener

            event.channel.sendTyping().queue()
            val completion = try {
                openai.chatCompletion(ChatCompletionRequest(
                    model = model,
                    messages = getPrompt(channel) + msgList,
                    maxTokens = 420,
                ))
            } catch (e: Exception) {
                Bot.logger.error("Failed to fetch chat completion", e)
                return@listener
            }

            val message = completion.choices.firstOrNull()?.message ?: run {
                Bot.logger.warn("No message from OpenAI")
                return@listener
            }
            val content = message.content
            if (content.isNullOrEmpty()) {
                Bot.logger.warn("Empty message from OpenAI")
                return@listener
            }

            msgList.add(message)
            event.message.reply_(content.take(2000)).await()
        }
    }
}

@Serializable
data class OpenAIConfig (
    val token: String = "INSERT_TOKEN",
)