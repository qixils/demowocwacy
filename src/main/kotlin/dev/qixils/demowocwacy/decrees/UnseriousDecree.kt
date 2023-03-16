package dev.qixils.demowocwacy.decrees

import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

class UnseriousDecree : Decree(
    "1",
    "\ud83e\udd21",
    "move channels to hidden, only unserious remains",
) {
    private var storage: Map<TextChannel, Category> = emptyMap()
    override fun execute() {
        val category: Category = Bot.jda.getCategoriesByName("hidden", true)[0]
        for (channel in Bot.config.unseriousChannels) {

            val channelResolved = Bot.jda.getTextChannelById(channel)!!
            storage += mapOf(Pair(channelResolved, channelResolved.parentCategory!!)) // idk just dont run it if the unseriousChannels aren't in a category :gibbited:
            // TODO need bot.store to be persistent for bot reload

            channelResolved.manager.setParent(category).queue()
            println(channelResolved.positionRaw)
        }
    }

    override fun cleanup() {
        for ((channel, category) in storage) {
            // i guess it remembers its position magically /shrug
            channel.manager.setParent(category).queue()
        }
    }
}