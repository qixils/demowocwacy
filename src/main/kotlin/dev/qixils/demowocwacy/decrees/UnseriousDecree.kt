package dev.qixils.demowocwacy.decrees

import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

class UnseriousDecree : Decree(
    "Embrace #unserious",
    "\ud83e\udd21",
    "Archives all discussion channels in favor of #unserious",
) {
    private val storage: MutableMap<TextChannel, Category?> = mutableMapOf()
    override fun execute() {
        val category: Category = Bot.guild.getCategoriesByName("hidden", true).first()
        for (channel in Bot.config.discussionChannels) {

            val channelResolved = Bot.jda.getTextChannelById(channel)!!
            storage[channelResolved] = channelResolved.parentCategory
            // TODO need bot.store to be persistent for bot reload

            channelResolved.manager.setParent(category).queue()
            //println(channelResolved.positionRaw)
        }
    }

    override fun cleanup() {
        for ((channel, category) in storage) {
            // i guess it remembers its position magically /shrug
            channel.manager.setParent(category).queue()
        }
    }
}