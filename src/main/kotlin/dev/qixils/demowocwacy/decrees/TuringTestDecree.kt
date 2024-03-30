package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import java.time.Duration
import java.time.Instant

data class CAPTCHA(
    val id: String,
    val question: String,
    val correct: List<SelectOption>,
    val incorrect: List<SelectOption>,
) {
    val answers: List<SelectOption>
        get() = correct + incorrect
}

class TuringTestDecree : Decree(
    "Turing Test",
    "\uD83E\uDD16",
    "Require users to complete a CAPTCHA every ten minutes to chat",
    true
) {
    private val authorized = mutableMapOf<Long, Instant>()
    private val timeout = Duration.ofMinutes(10)!!

    private val captchas = listOf(
        CAPTCHA(
            "cat",
            "Please select the images of CATS to continue.",
            listOf(
                SelectOption("Lexi", "lexi"),
                SelectOption("Chroma", "chroma"),
                SelectOption("Zion", "zion"),
                SelectOption("Sarah", "sarah"),
            ),
            listOf(
                SelectOption("Nedo", "nedo"),
                SelectOption("Adam", "adam"),
                SelectOption("Endr", "endr"),
                SelectOption("Melody", "melody"),
            ),
        ),
        CAPTCHA(
            "dog",
            "Please select the images of DOGS to continue.",
            listOf(
                SelectOption("Nedo", "nedo"),
                SelectOption("Adam", "adam"),
                SelectOption("Sarah", "sarah"),
            ),
            listOf(
                SelectOption("Lexi", "lexi"),
                SelectOption("Chroma", "chroma"),
                SelectOption("Endr", "endr"),
                SelectOption("Melody", "melody"),
                SelectOption("Zion", "zion"),
            ),
        ),
        CAPTCHA(
            "math",
            "Please select the answers to 16=3(x-1)^2+4 to continue.",
            listOf(
                SelectOption("3", "3"),
                SelectOption("-1", "-1"),
            ),
            listOf(
                SelectOption("-2", "-2"),
                SelectOption("2", "2"),
                SelectOption("-3", "-3"),
                SelectOption("1", "1"),
                SelectOption("-1.5", "-1.5"),
                SelectOption("1.5", "1.5"),
            ),
        ),
        CAPTCHA(
            "minecraft",
            "Please select the images of MINECRAFT MOBS to continue.",
            listOf(
                SelectOption("Chicken", "chicken", emoji = Emoji.fromUnicode("\uD83D\uDC14")),
                SelectOption("Pig", "pig", emoji = Emoji.fromUnicode("\uD83D\uDC37")),
                SelectOption("Squid", "squid", emoji = Emoji.fromUnicode("\uD83E\uDD91")),
            ),
            listOf(
                SelectOption("Sarah", "sarah", emoji = Emoji.fromCustom("averagehtstemfamiliar", 1007515376743088172L, false)),
                SelectOption("Jellyfish", "jellyfish", emoji = Emoji.fromUnicode("\uD83E\uDEBC")),
                SelectOption("Firefly", "firefly", emoji = Emoji.fromUnicode("\uD83D\uDCA1")),
            ),
        ),
    )

    private fun isAuthorized(user: Long): Boolean {
        if (user !in authorized) return false
        val duration = Duration.between(authorized[user], Instant.now())
        if (duration > timeout) {
            authorized.remove(user)
            return false
        }
        return true
    }

    private suspend fun sendCaptcha(it: IReplyCallback) {
        val captcha = captchas.random()
        val component = StringSelectMenu(
            "captcha-${captcha.id}",
            valueRange = 1..captcha.answers.size,
            options = captcha.answers.shuffled(),
        )
        it.reply_(captcha.question, components = listOf(row(component)), ephemeral = true).await()
    }

    override suspend fun execute() {
        Bot.guild.upsertCommand("verify", "Complete a CAPTCHA to validate your humanity")

        Bot.jda.listener<MessageReceivedEvent> {
            if (!isApplicableTo(it.channel)) return@listener
            if (it.message.type.isSystem) return@listener
            if (isAuthorized(it.author.idLong)) return@listener

            it.message.delete().await()
            it.channel.send("${it.author.asMention}: Please type `/verify` to confirm you are human before chatting!").await()
        }

        Bot.jda.listener<SlashCommandInteractionEvent> {
            if (it.fullCommandName != "verify") return@listener
            if (isAuthorized(it.user.idLong)) {
                it.reply_("You are already verified!", ephemeral = true).await()
                return@listener
            }
            sendCaptcha(it)
        }

        Bot.jda.listener<StringSelectInteractionEvent> {
            val captcha = captchas.find { c -> "captcha-${c.id}" == it.component.id } ?: return@listener
            val isSelected = { a: SelectOption -> it.selectedOptions.any { so -> so.value == a.value } }
            val passed = captcha.correct.all(isSelected) && captcha.incorrect.none { a -> !isSelected(a) } // i think this is probably suboptimal but whatever
            if (passed) {
                authorized[it.user.idLong] = Instant.now()
                it.reply_("You have verified your humanity! Congratulations.", ephemeral = true).await()
            } else {
                sendCaptcha(it)
            }
        }
    }
}