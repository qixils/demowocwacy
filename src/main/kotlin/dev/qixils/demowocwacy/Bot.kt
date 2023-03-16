package dev.qixils.demowocwacy

import com.typesafe.config.ConfigFactory
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onButton
import dev.minn.jda.ktx.events.onStringSelect
import dev.minn.jda.ktx.interactions.components.*
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.messages.*
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
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.messages.MessageRequest
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
    private val signupButton = primary("signup", "Announce Candidacy")
    private val voteButton = primary("vote", "Open Ballot")

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
     * Gets decrees that have been selected by an elected leader.
     */
    val selectedDecrees: List<Decree>
        get() = allDecrees.filter { it.name in state.selectedDecrees }

    /**
     * Gets decrees that have been ignored by an elected leader.
     */
    val ignoredDecrees: List<Decree>
        get() = allDecrees.filter { it.name in state.ignoredDecrees }

    /**
     * Gets pending decrees that are waiting to be selected or ignored.
     */
    val pendingDecrees: List<Decree>
        get() = allDecrees.filter { it.name in state.election.decrees }

    /**
     * All decrees that have not yet been drawn from the random hat.
     */
    val remainingDecrees: List<Decree>
        get() = allDecrees.filter { it.name !in state.selectedDecrees && it.name !in state.ignoredDecrees && it.name !in state.election.decrees }

    /**
     * Channel in which elections are held.
     */
    val channel: TextChannel
        get() = jda.getTextChannelById(config.channel)!!

    /**
     * Guild in which the bot is running.
     */
    val guild: Guild
        get() = jda.getGuildById(config.guild)!!

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
        // disable @everyone pings
        MessageRequest.setDefaultMentions(emptySet())
        // init decrees
        allDecrees = listOf(
            TWOWDecree(),
            UnseriousDecree()
        )
        // init forms
        jda.onButton(signupButton.id!!) { event ->
           // signing up for office
            if (!isRegisteredVoter(event.user)) {
                event.reply_("You must be a registered voter to run for office!", ephemeral = true).queue()
                return@onButton
            }
            val election = state.election
            if (event.user.idLong in election.candidates) {
                event.reply_("You are already a candidate!", ephemeral = true).queue()
                return@onButton
            }
            // open form
            event.replyModal(id = "form:signup", title = "Announce Candidacy") {
                paragraph(
                    id = "form:signup:platform",
                    label = "Describe what you would do as prime minister",
                    required = true,
                    placeholder = "As prime minister of ${event.guild!!.name}, I would enact the decree ___. I would also..."
                )
            }
        }
        jda.onButton(voteButton.id!!) { event ->
            if (!isRegisteredVoter(event.user)) {
                event.reply_("You must be a registered voter to vote!", ephemeral = true).queue()
                return@onButton
            }
            val election = state.election
            if (election.votes.containsKey(event.user.idLong)) {
                event.reply_("You have already voted!", ephemeral = true).queue()
                return@onButton
            }

            // btw todo maybe have to make sure only 25 candidates per menu
            val electionOptions = election.candidates.map { cand: Long -> SelectOption.of(jda.retrieveUserById(cand).await().name, cand.toString()) }

            // dm?
            event.reply_("select candidates here u\\*ᵤ\\*u", ephemeral = true, components = listOf(row(
                StringSelectMenu("menu:vote") {
                    addOptions(electionOptions)
                    setRequiredRange(2,25)
                }
            ))).queue()
        }
        // todo check if within voting time
        jda.onStringSelect("menu:vote"){event ->
            state.election.votes += mapOf(
                Pair(event.user.idLong, event.selectedOptions.map{ it.value.toLong() })
            )
            event.reply_(content="thamkies for votimgg:333 ").queue()
        }
            // TODO: here is where I would open a form. except forms don't actually support choices yet.
            //  so i think it will have to just be an ephemeral message with an action row for:
            //   - selecting approved candidates
            //   - selecting favorite decree (i think just one)
            //   - submitting
        jda.listener<ModalInteractionEvent> { event ->
            if (event.modalId == "form:signup") {
                val election = state.election
                // double check that the user still isn't a candidate (and is a registered voter)
                if (!isRegisteredVoter(event.user)) {
                    event.reply_("You must be a registered voter to run for office!", ephemeral = true).queue()
                    return@listener
                }
                if (event.user.idLong in election.candidates) {
                    event.reply_("You are already a candidate!", ephemeral = true).queue()
                    return@listener
                }
                // save candidate to state
                election.candidates.add(event.user.idLong)
                state = state.copy(election = election)
                // create thread for candidate/platform
                val platform = event.getValue("form:signup:platform")?.asString ?: return@listener
                val message = channel.send(
                    """
                        ${event.member!!.asMention} has announced their candidacy!
                        >>> $platform
                    """.trimIndent(),
                    mentions = Mentions.of(MentionConfig.users(listOf(event.member!!.idLong)))
                ).await()
                message.createThreadChannel("Candidate: ${event.member!!.effectiveName}").queue()
            }
        }
    }

    fun isGuild(guild: Long): Boolean {
        return guild == config.guild
    }

    fun isGuild(guild: Guild): Boolean {
        return isGuild(guild.idLong)
    }

    fun isInGuild(channel: GuildChannel): Boolean {
        return isGuild(channel.guild)
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
                // begin election handling
                handleElection()
            }
        }
    }

    private suspend fun handleElection() {
        // TODO: state management (resume from prior session)
        handleElectionRegistrationPhase()
        handleElectionVotingPhase()
    }

    private suspend fun handleElectionRegistrationPhase() {
        // wait for start of election cycle (top of the hour)
        var now = System.currentTimeMillis()
        val nextHour = now + 3600000 - now % 3600000
        delay(nextHour - now)
        // put signup form in elections channel
        val messageData = MessageCreate {
            content = "The time has come to elect a new leader to bring our nation to glorious greatness! " +
                    "Over the next half hour, the fearless and noble of you citizens will have the opportunity to run for office. " +
                    "Attached to this message is a button which will open the form to announce your candidacy.\n\n" +
                    "For those of you not yet ready to take the reins of power, we still want to hear your voice. " +
                    "A thread will be created for each candidate to discuss their platform and answer questions from you, the people.\n\n" +
                    "Vox populi, vox dei."
            components += row(signupButton)
        }
        val message = channel.sendMessage(messageData).await()
        // sleep until XX:30
        now = System.currentTimeMillis()
        val nextHalfHour = now + 1800000 - now % 1800000
        delay(nextHalfHour - now)
        // close signup form
        message.editMessageComponents(row(signupButton.asDisabled())).queue()
    }

    private suspend fun handleElectionVotingPhase() {
        // announce ballot
        val messageData = MessageCreate {
            content = "" // todo
            components += row(voteButton)
        }
        val message = channel.sendMessage(messageData).await()
        // sleep until XX:40
        val now = System.currentTimeMillis()
        val nextTenMins = now + 600000 - now % 600000
        delay(nextTenMins - now)
        // close ballot and threads
        message.editMessageComponents(row(voteButton.asDisabled())).queue()
        channel.threadChannels.forEach { it.manager.setLocked(true).queue() }
        // tally votes
        // TODO
        // check for tie for first place
        // TODO
        if (true) {
            // resort to short FPTP tie breaker
            // TODO
        }
        // announce winner
        // TODO
        // DM decree form
        // TODO
    }
}
