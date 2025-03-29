package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeSTEMification : Decree(
    "Whatyourself?",
    "🗡️",
    "Stabbing is so 2012, it's time for...",
    false
) {
    override suspend fun execute(init: Boolean) {
        val selection = withContext(Dispatchers.IO) {
            val words = javaClass.getResourceAsStream("/verbs.txt")!!.reader().readLines()
            words.shuffled().first()
        }
        Bot.guild.manager.setName("${selection}yourself").await()
    }

    override suspend fun cleanup() {
        Bot.guild.manager.setName("Stabyourself").await()
    }
}
