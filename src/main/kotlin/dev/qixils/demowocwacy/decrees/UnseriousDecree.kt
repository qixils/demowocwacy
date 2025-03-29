package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel

class UnseriousDecree : Decree(
    "Embrace #shamless-chamber",
    "\uD83D\uDE08",
    "Archives all discussion channels in favor of #shamless-chamber",
    false
) {
    val unserious get() = Bot.guild.getTextChannelById(Bot.config.decrees.unserious.channel)!!

    override suspend fun execute(init: Boolean) {
        val storage: MutableMap<Long, Pair<Long?, Int>> = mutableMapOf()
        val category: Category = Bot.guild.getCategoryById(Bot.config.decrees.unserious.hiddenCategory)!!
        for (channel in unserious.parentCategory!!.textChannels) {
            if (channel.idLong == Bot.config.decrees.unserious.channel) continue
            if (channel.idLong in Bot.config.protectedChannels) continue
            // store current category and position
            storage[channel.idLong] = channel.parentCategory?.idLong to (channel.parentCategory?.channels?.filter { it.type.sortBucket == channel.type.sortBucket }?.indexOf(channel) ?: -1)
            // set new category
            channel.manager.setParent(category).queue()
        }
        Bot.state.decrees.unserious.discussionChannels = storage
        Bot.saveState()
    }

    override suspend fun cleanup() {
        val storage: MutableMap<Category?, MutableMap<GuildChannel, Int>> = mutableMapOf()
        for ((channelId, data) in Bot.state.decrees.unserious.discussionChannels) {
            val channel = Bot.guild.getGuildChannelById(channelId)!!
            val category = data.first?.let { Bot.guild.getCategoryById(it) }
            if (category != null)
                (channel as ICategorizableChannel).manager.setParent(category).await()
            if (data.second >= 0)
                storage.getOrPut(category) { mutableMapOf() }[channel] = data.second
        }
        for ((category, channels) in storage.entries) {
            val textPositions = category?.modifyTextChannelPositions() ?: Bot.guild.modifyTextChannelPositions()
            val voicePositions = category?.modifyVoiceChannelPositions() ?: Bot.guild.modifyVoiceChannelPositions()
            for ((channel, position) in channels.entries.sortedBy { it.value }) {
                when (channel.type.sortBucket) {
                    0 -> textPositions.selectPosition(channel).moveTo(position)
                    1 -> voicePositions.selectPosition(channel).moveTo(position)
                    else -> Bot.logger.warn("Unknown sort bucket ${channel.type.sortBucket} for channel ${channel.idLong}, skipping")
                }
            }
            textPositions.queue()
            voicePositions.queue()
        }
    }
}

@Serializable
data class UnseriousState(
    var discussionChannels: Map<Long, Pair<Long?, Int>> = emptyMap(),
)

@Serializable
data class UnseriousConfig(
    // ID for the Unserious channel
    val channel: Long = 0,
    // ID of the category to hide channels in
    val hiddenCategory: Long = 0,
)
