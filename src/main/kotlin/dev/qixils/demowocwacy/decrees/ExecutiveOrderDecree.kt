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
import net.dv8tion.jda.api.interactions.commands.build.OptionData

class ExecutiveOrderDecree : Decree(
    "Executive Order",
    "\uD83D\uDCDC",
    "Pass a previously skipped decree",
    true
) {
    private suspend fun onInit() {
        val available = Bot.ignoredDecrees
            .filter { it !is ExecutiveOrderDecree } // failsafe lol?
            .shuffled()
            .take(OptionData.MAX_CHOICES)
            .sortedBy { it.name }
        Bot.state.decrees.executiveOrder.options += available.map { it.name }
        Bot.state.decrees.executiveOrder.message = Bot.pmChannel.send(
            "Please select a decree to pass.",
            components = listOf(row(StringSelectMenu(
                "executive-order",
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

    private fun pass(decree: String) {
        Bot.state.decrees.executiveOrder.message = 0L
        Bot.state.decrees.executiveOrder.options.clear()
        Bot.state.selectedDecrees.add(decree)
        Bot.state.ignoredDecrees.remove(decree)
    }

    override suspend fun execute(init: Boolean) {
        if (init) onInit()
        Bot.jda.onStringSelect("executive-order") { event -> coroutineScope {
            if (Bot.state.election.primeMinister != event.user.idLong) return@coroutineScope
            if (Bot.state.decrees.executiveOrder.message == 0L) return@coroutineScope
            val optionName = event.selectedOptions[0].value
            if (optionName !in Bot.state.decrees.executiveOrder.options) return@coroutineScope
            val option = Bot.allDecrees.find { it.name == optionName } ?: return@coroutineScope

            pass(optionName)

            launch { Bot.saveState() }
            launch { event.reply("Very well. Effective immediately, **${option.displayName}** is now law.").await() }
            launch { Bot.channel.send("The Prime Minister has chosen to pass __**${option.displayName}**: ${option.description}__.").await() }
            launch { option.execute(true) }
            launch { event.message.editMessageComponents(event.message.components.map { it.asDisabled() }).await() }
        } }
    }

    override suspend fun onStartTask(task: Task) = coroutineScope {
        if (task != Task.CLOSE_BALLOT) return@coroutineScope
        if (Bot.state.decrees.executiveOrder.message == 0L) return@coroutineScope

        val optionName = Bot.state.decrees.executiveOrder.options.random()
        val option = Bot.allDecrees.find { it.name == optionName } ?: run {
            Bot.logger.error("Failed to pass `$optionName`")
            return@coroutineScope
        }

        pass(optionName)

        launch { Bot.saveState() }
        launch { Bot.pmChannel.send("Indecisiveness will win you no points in the next elections.").await() }
        launch { Bot.channel.send("On behalf of the Prime Minister, I have chosen to pass __**${option.displayName}**: ${option.description}__.").await() }
        launch { option.execute(true) }
        launch { Bot.closeMessage(Bot.state.decrees.executiveOrder.message, "executive order", Bot.pmChannel) }
    }
}

@Serializable
data class ExecutiveOrderState(
    var message: Long = 0,
    val options: MutableList<String> = mutableListOf(),
)