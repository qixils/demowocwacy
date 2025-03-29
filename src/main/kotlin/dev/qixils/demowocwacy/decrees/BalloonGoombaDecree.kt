package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

class BalloonGoombaDecree : Decree(
    "Balloon Goomba",
    Emoji.fromCustom("willSmith", 537359524743479306L, false),
    "All messages must contain a balloon and a goomba",
    true
) {
    companion object {
        private val wordLists: List<List<String>> = listOf(
            listOf(
                "balloon",
                "\uD83C\uDF88",
                "\uD83D\uDCAC",
                "\uD83D\uDCAD",
                "\uD83D\uDDEF\uFE0F",
                "\uD83D\uDDE8\uFE0F",
                "\uD83D\uDC41\uFE0F\u200D\uD83D\uDDE8\uFE0F",
            ),
            listOf(
                "goomba",
                "<:willSmith:537359524743479306>",
                "<:RTXON:758830361316294716>",
                "<:cursedgoomba:721040855271604294>",
                "<:cursedgoomba8bit:722125487484305440>",
                "<:cursedgoombastomp:778076095957172235>",
                "<:emptygoomba:759206280665235478>",
                "<:goomblushed:758847232937426944>",
                "<:suspicious:942899030273437696>",
                "<:willnew:413535782536609792>",
                "<a:RTXhhnngg:759523330154954763>",
                "<a:RTXwalk:772613600938164224>",
                "<a:hhnngg:759521787309981776>",
            ),
        )
    }

    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener
            if (event.message.type.isSystem) return@listener

            val content = event.message.contentRaw.lowercase()
            var allowed = true
            for (list in wordLists) {
                if (list.none { it in content }) {
                    allowed = false
                    break
                }
            }

            if (allowed) return@listener

            event.message.delete().reason(event.message.id).queueAfter(500, TimeUnit.MILLISECONDS)
        }
    }
}