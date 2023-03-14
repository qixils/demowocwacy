package dev.qixils.demowocwacy

import com.typesafe.config.ConfigFactory
import dev.minn.jda.ktx.events.onButton
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import dev.qixils.demowocwacy.decrees.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.internal.utils.PermissionUtil
import java.io.File
import java.time.Month
import java.time.ZoneOffset
import java.time.ZonedDateTime

@OptIn(ExperimentalSerializationApi::class)
object Bot {

    val hocon = Hocon {
        useConfigNamingConvention = true
    }
    val cbor = Cbor {  }

    private val configFile = File("bot.conf")
    private val stateFile = File("state.cbor")

    val jda: JDA

    /**
     * The user-defined configuration.
     */
    val config: BotConfig

    /**
     * All decrees that can be selected.
     */
    val allDecrees: List<Decree>

    /**
     * All decrees that have been selected by an elected leader.
     */
    val selectedDecrees: List<Decree>
        get() = allDecrees.filter { it.name in state.selectedDecrees }

    /**
     * All decrees that have been ignored by an elected leader.
     */
    val ignoredDecrees: List<Decree>
        get() = allDecrees.filter { it.name in state.ignoredDecrees }

    /**
     * All decrees that have not yet been shown to an elected leader.
     */
    val remainingDecrees: List<Decree>
        get() = allDecrees.filter { it.name !in state.selectedDecrees && it.name !in state.ignoredDecrees }

    /**
     * Channel in which elections are held.
     */
    val channel: TextChannel
        get() = jda.getTextChannelById(config.channel)!!

    /**
     * The state of the bot.
     * This is loaded from [stateFile] and saved to it when changed.
     */
    var state: BotState = if (stateFile.exists()) cbor.decodeFromByteArray(stateFile.readBytes()) else BotState()
        get() = field.copy()
        set(value) {
            field = value.copy()
            stateFile.writeBytes(cbor.encodeToByteArray(field))
        }

    init {
        // load config | TODO: default config? ig not very important
        val configNode = ConfigFactory.parseFile(configFile)
        config = hocon.decodeFromConfig(configNode)
        // build JDA
        jda = light(config.token, enableCoroutines=true) {
            // I don't think I need member updates/joins/leaves but if I do the intent is GUILD_MEMBERS
            intents += GatewayIntent.MESSAGE_CONTENT
        }
        // init decrees
        allDecrees = listOf(
            TWOWDecree()
        )
        // init forms
        jda.onButton("signup") { event ->
           // signing up for the prime minister-y
            if (!isRegisteredVoter(event.user)) {
                event.reply_("You must be a registered voter to run for office!", ephemeral = true).queue()
                return@onButton
            }
            val election = state.election
            if (election.candidates.contains(event.user.idLong)) {
                event.reply_("You are already a candidate!", ephemeral = true).queue()
                return@onButton
            }
            // open form
            TODO()
        }
    }

    fun isRegisteredVoter(user: User): Boolean {
        // check for MESSAGE_SEND_IN_THREADS permission in the elections channel
        // this allows normal members to spectate while only registered voters can participate
        val channel = this.channel
        val member = channel.guild.getMember(user) ?: return false
        return PermissionUtil.checkPermission(channel, member, Permission.MESSAGE_SEND_IN_THREADS)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            jda.awaitReady()
            // init active decrees
            selectedDecrees.filter(Decree::persistent).forEach(Decree::execute)
            // loop
            while (true) {
                // abort after April 1st
                val zdt = ZonedDateTime.now(ZoneOffset.UTC)
                if (zdt.month == Month.APRIL && zdt.dayOfMonth > 1)
                    break

                // check for in-progress election
                // TODO

                // election has ended; wait for top of hour to start next one
                val now = System.currentTimeMillis()
                val nextHour = now + 3600000 - now % 3600000
                delay(nextHour - now)

                // start election
                startElection()
            }
        }
    }

    private suspend fun startElection() {
        // put signup form in elections channel
        val message = MessageCreate {
            content = "The time has come to elect a new leader to bring our nation to glorious greatness! " +
                    "Over the next half hour, the fearless and noble of you citizens will have the opportunity to run for office. " +
                    "Attached to this message is a button which will open the form to announce your candidacy.\n\n" +
                    "For those of you not yet ready to take the reins of power, we still want to hear your voice. " +
                    "A thread will be created for each candidate to discuss their platform and answer questions from you, the people.\n\n" +
                    "Vox populi, vox dei."
            // TODO add button component
        }
        channel.sendMessage(message).queue()
    }
}
