import dev.minn.jda.ktx.generics.getChannel
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import net.dv8tion.jda.api.requests.GatewayIntent

fun main() {
    val jda = light("TOKEN", enableCoroutines=true) {
        intents += GatewayIntent.MESSAGE_CONTENT
    }
    jda.awaitReady()

    val pattern = Regex(".+ \\*\\*(\\d+)\\*\\* (?:https?://(?:(?:ptb|canary)\\.)?discord(?:app)?.com/channels/\\d{17,19}/(\\d{17,19})/(\\d{17,19})|<#(\\d{17,19})> ID: (\\d{17,19}))$")
    val starboard = jda.getTextChannelById(375361337464979467L)!!
    val leaderboard = mutableMapOf<Long, Int>()


    starboard.iterableHistory.forEach { message ->
        if (message.author.idLong != 233034619069530113L) return@forEach
        val match = pattern.find(message.contentRaw) ?: return@forEach
        val stars = match.groupValues[1].toIntOrNull() ?: return@forEach
        val channelId = match.groupValues[2].ifEmpty { match.groupValues[4] }
        val channel = jda.getChannel<StandardGuildMessageChannel>(channelId) ?: return@forEach
        val messageId = match.groupValues[3].ifEmpty { match.groupValues[5] }
        val source = try { channel.retrieveMessageById(messageId).complete() }
        catch (e: Exception) { return@forEach }
        leaderboard.compute(source.author.idLong) { _, tally -> (tally ?: 0) + stars }
        println("[${message.timeCreated}] ${source.author} now has ${leaderboard[source.author.idLong]} stars")
    }

    println()
    print('{')
    leaderboard.onEachIndexed { index, entry ->
        print("\"${entry.key}\":${entry.value}")
        if (index != (leaderboard.size - 1))
            print(',')
    }

    print('}')
    println()

    jda.shutdown()
}