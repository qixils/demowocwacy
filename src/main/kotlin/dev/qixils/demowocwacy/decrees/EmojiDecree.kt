package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

class EmojiDecree : Decree(
    "Talk Like an Egyptian",
    "\uD83D\uDCAC",
    "Communicate only through pictograms",
    true
) {
    private val blocks = listOf(
        Character.UnicodeBlock.EGYPTIAN_HIEROGLYPHS,
        Character.UnicodeBlock.EGYPTIAN_HIEROGLYPH_FORMAT_CONTROLS,
        Character.UnicodeBlock.ANATOLIAN_HIEROGLYPHS,
        Character.UnicodeBlock.MEROITIC_HIEROGLYPHS,
    )

    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener

            // https://unicode.org/reports/tr51/#Emoji_Properties_and_Data_Files
            // unicode publishes massive text files of all emoji sequences which could be checked against
            // but i'd rather be lenient
            if (event.message.contentRaw.any {
                !Character.isExtendedPictographic(it.code)
                        && !Character.isEmojiComponent(it.code)
                        && Character.UnicodeBlock.of(it.code) !in blocks
            }) {
                event.message.delete().queueAfter(500, TimeUnit.MILLISECONDS)
            }
        }
    }
}