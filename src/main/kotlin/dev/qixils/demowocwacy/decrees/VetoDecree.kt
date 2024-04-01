package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.onStringSelect
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.send
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import dev.qixils.demowocwacy.Task
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class VetoDecree : Decree(
    "Veto",
    "⚖\uFE0F",
    "Repeal a previously passed decree",
    true
) {
    private suspend fun onInit() {
        val available = Bot.selectedDecrees
            .filter { it !is VetoDecree }
            .sortedBy { it.name }
        Bot.state.decrees.veto.options += available.map { it.name }
        Bot.state.decrees.veto.message = Bot.pmChannel.send(
            "Please select a decree to repeal.",
            components = listOf(row(StringSelectMenu(
                "veto",
                options = available.map { SelectOption(
                    it.name,
                    it.name,
                    it.description,
                    it.emoji,
                ) },
            )))
        ).await().idLong
        Bot.saveState()
    }

    private fun repeal(decree: String) {
        Bot.state.decrees.veto.message = 0L
        Bot.state.decrees.veto.options.clear()
        Bot.state.ignoredDecrees.add(decree)
        Bot.state.selectedDecrees.remove(decree)
    }

    override suspend fun execute(init: Boolean) {
        if (init) onInit()
        Bot.jda.onStringSelect("veto") { event -> coroutineScope {
            if (Bot.state.election.primeMinister != event.user.idLong) return@coroutineScope
            if (Bot.state.decrees.veto.message == 0L) return@coroutineScope
            val optionName = event.selectedOptions[0].value
            if (optionName !in Bot.state.decrees.veto.options) return@coroutineScope
            val option = Bot.allDecrees.find { it.name == optionName } ?: return@coroutineScope

            repeal(optionName)

            launch { Bot.saveState() }
            launch { event.reply("Very well. Effective immediately, **${option.displayName}** is repealed.").await() }
            launch { Bot.channel.send("The Prime Minister has chosen to repeal **${option.displayName}**.").await() }
            launch { option.cleanup() }
            launch { event.message.editMessageComponents(event.message.components.map { it.asDisabled() }).await() }
        } }
    }

    override suspend fun onStartTask(task: Task) = coroutineScope {
        if (task != Task.CLOSE_BALLOT) return@coroutineScope
        if (Bot.state.decrees.veto.message == 0L) return@coroutineScope

        val optionName = Bot.state.decrees.veto.options.random()
        val option = Bot.allDecrees.find { it.name == optionName } ?: run {
            Bot.logger.error("Failed to repeal `$optionName`")
            return@coroutineScope
        }

        repeal(optionName)

        launch { Bot.saveState() }
        launch { Bot.pmChannel.send("Indecisiveness will win you no points in the next elections.").await() }
        launch { Bot.channel.send("On behalf of the Prime Minister, I have chosen to repeal **${option.displayName}**.").await() }
        launch { option.cleanup() }
        launch { Bot.closeMessage(Bot.state.decrees.veto.message, "veto") }
    }
}

@Serializable
data class VetoState(
    var message: Long = 0,
    val options: MutableList<String> = mutableListOf(),
)