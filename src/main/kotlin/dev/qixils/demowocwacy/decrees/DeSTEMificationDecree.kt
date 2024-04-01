package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class DeSTEMification : Decree(
    "Stexit",
    "\uD83E\uDDEA",
    "Out with the STEM, in with the...",
    false
) {
    private val science = Bot.guild.getTextChannelById(Bot.config.decrees.stem.science)!!
    private val technology = Bot.guild.getTextChannelById(Bot.config.decrees.stem.technology)!!
    private val engineering = Bot.guild.getTextChannelById(Bot.config.decrees.stem.engineering)!!
    private val mathematics = Bot.guild.getTextChannelById(Bot.config.decrees.stem.mathematics)!!

    override suspend fun execute(init: Boolean) {
        val selections = withContext(Dispatchers.IO) {
            val words = javaClass.getResourceAsStream("words.txt")!!.reader().readLines()
            words.shuffled().take(4)
        }
        val acronym = selections.joinToString("") { it[0].uppercase() }
        Bot.guild.manager.setName("HTwins $acronym+").await()
        science.manager.setName(selections[0]).await()
        technology.manager.setName(selections[1]).await()
        engineering.manager.setName(selections[2]).await()
        mathematics.manager.setName(selections[3]).await()
    }

    override suspend fun cleanup() {
        Bot.guild.manager.setName("HTwins STEM+").await()
        science.manager.setName("science").await()
        technology.manager.setName("technology").await()
        engineering.manager.setName("engineering").await()
        mathematics.manager.setName("mathematics").await()
    }
}

@Serializable
data class STEMConfig(
    val science: Long = 0,
    val technology: Long = 0,
    val engineering: Long = 0,
    val mathematics: Long = 0,
)