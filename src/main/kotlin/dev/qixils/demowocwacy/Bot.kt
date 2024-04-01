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
import dev.minn.jda.ktx.util.SLF4J
import dev.qixils.demowocwacy.decrees.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.hocon.encodeToConfig
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.messages.MessageRequest
import net.dv8tion.jda.internal.utils.PermissionUtil
import org.slf4j.Logger
import java.io.File
import java.time.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalSerializationApi::class)
object Bot {

    val hocon = Hocon {
        useConfigNamingConvention = true
    }
    val cbor = Cbor {  }

    private val configFile = File("bot.conf")
    private val stateFile = File("state.cbor")
    private val signupButton = primary("signup", "Announce Candidacy")
    private val voteSorter = compareByDescending<Pair<Comparable<*>, Int>> { it.second }.then(compareBy { it.first })

    val jda: JDA
    val logger: Logger by SLF4J

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
     * The private channel for the Prime Minister.
     */
    val pmChannel: TextChannel
        get() = jda.getTextChannelById(config.pmChannel)!!

    /**
     * Guild in which the bot is running.
     */
    val guild: Guild
        get() = jda.getGuildById(config.guild)!!

    /**
     * Role for Familiar users.
     */
    val familiar: Role
        get() = guild.getRoleById(config.roles.familiar)!!

    /**
     * Unserious channel.
     */
    val unserious: TextChannel
        get() = jda.getTextChannelById(config.decrees.unserious.channel)!!

    /**
     * Role for the current Prime Minister
     */
    val currentLeaderRole: Role
        get() = guild.getRoleById(config.roles.currentLeader)!!

    /**
     * Role for the past Prime Ministers
     */
    val pastLeaderRole: Role
        get() = guild.getRoleById(config.roles.pastLeader)!!

    /**
     * Role for the current election candidates
     */
    val candidateRole: Role
        get() = guild.getRoleById(config.roles.candidate)!!

    /**
     * Role for past voters
     */
    val voterRole: Role
        get() = guild.getRoleById(config.roles.voter)!!

    /**
     * The state of the bot.
     * This is loaded from [stateFile] and saved to it when changed.
     */
    val state: BotState = if (stateFile.exists()) cbor.decodeFromByteArray(stateFile.readBytes()) else BotState()

    init {
        // create default config if it doesn't exist
        if (!configFile.exists()) {
            val configNode = hocon.encodeToConfig(BotConfig())
            configFile.writeText(configNode.resolve().root().render())
        }
        // load config
        val configNode = ConfigFactory.parseFileAnySyntax(configFile)
        config = hocon.decodeFromConfig(configNode)
        // build JDA
        jda = light(config.token, enableCoroutines=true) {
            intents += setOf(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
        }
        // disable @everyone pings
        MessageRequest.setDefaultMentions(emptySet())
        // init decrees
        allDecrees = listOf(
            TWOWDecree(),
            UnseriousDecree(),
            DemoteLexiDecree(),
            UndeleteDecree(),
            HTCDecree(),
            PeanutDecree(),
            Literally1984Decree(),
            BlindnessEpidemic(),
            EmbraceChristianityDecree(),
            ReverseDecree(),
            InvertDecree(),
            TuringTestDecree(),
            SpeechlessDecree(),
            ChatGPTDecree(),
            NoMathDecree(),
            DeSTEMification(),
            DyslexiaDecree(),
            JanitorDecree(),
            DadDecree(),
            CloneDecree(),
            EmbraceDiseaseDecree(),
            NoFifthGlyphDecree(),
            GentlepeopleDecree(),
            R9KDecree(),
            DoNotPassThisLawDecree(),
        )
        // init signup form
        jda.onButton(signupButton.id!!) { event ->
           // signing up for office
            if (!isRegisteredVoter(event.user)) {
                event.reply_("You must be a registered voter to run for office!", ephemeral = true).queue()
                return@onButton
            }
            if (event.user.idLong in state.election.candidates) {
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
        jda.listener<ModalInteractionEvent> { event ->
            if (event.modalId == "form:signup") {
                // double check that the user still isn't a candidate (and is a registered voter)
                if (!isRegisteredVoter(event.user)) {
                    event.reply_("You must be a registered voter to run for office!", ephemeral = true).queue()
                    return@listener
                }
                if (event.user.idLong in state.election.candidates) {
                    event.reply_("You are already a candidate!", ephemeral = true).queue()
                    return@listener
                }
                // save candidate to state
                state.election.candidates.add(event.user.idLong)
                saveState()
                guild.addRoleToMember(event.user, candidateRole).queue()
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
        // init ballots
        jda.onStringSelect("vote:candidate") {event ->
            state.election.candidateVotes[event.user.idLong] = event.selectedOptions.map{ it.value.toLong() }.filter { state.election.candidates.contains(it) }
            saveState()
            event.reply_(content = "Your approved candidates have been recorded.", ephemeral = true).queue()
            if (voterRole !in event.member!!.roles)
                guild.addRoleToMember(event.user, voterRole).queue()
        }
        jda.onStringSelect("vote:decree") { event ->
            val decree = event.selectedOptions.first().value
            if (decree !in state.election.decrees) {
                event.reply_(content = "That decree is not currently available to vote on.", ephemeral = true).queue()
                return@onStringSelect
            }
            state.election.decreeVotes[event.user.idLong] = decree
            saveState()
            event.reply_(content = "Your desired decree has been recorded.", ephemeral = true).queue()
            if (voterRole !in event.member!!.roles)
                guild.addRoleToMember(event.user, voterRole).queue()
        }
        jda.onStringSelect("vote:tiebreak") { event ->
            // candidate tie-break
            val candidate = event.selectedOptions.first().value.toLong()
            if (candidate !in state.election.tieBreakCandidates) {
                event.reply_(content = "That candidate is not currently available to vote on.", ephemeral = true).queue()
                return@onStringSelect
            }
            state.election.tieBreakVotes[event.user.idLong] = candidate
            saveState()
            event.reply_(content = "Your desired candidate has been recorded.", ephemeral = true).queue()
            if (voterRole !in event.member!!.roles)
                guild.addRoleToMember(event.user, voterRole).queue()
        }
        // init decree listener
        jda.listener<ButtonInteractionEvent> { event -> coroutineScope {
            val split = event.button.id?.split(':', limit = 2) ?: return@coroutineScope
            if (split.size < 2) return@coroutineScope
            if (split[0] != "pick-decree") return@coroutineScope
            val decree = pendingDecrees.find { it.name == split[1] }
            if (decree == null) {
                event.reply(MessageCreate {
                    content = "I was unable to find the decree you requested. This may be an error. I apologize. <@140564059417346049>"
                    mentions { user(140564059417346049L) }
                }).queue()
                return@coroutineScope
            }
            launch { event.reply("Thank you. **${decree.displayName}** shall be enacted shortly.").await() }
            launch { channel.sendMessage(buildString {
                append("Your Prime Minister has returned with their decree. Effective immediately, **")
                append(decree.displayName)
                append("**: ")
                append(decree.description)
                append("\nGlory to ")
                append(guild.name)
                append(".")
            }).await() }
            launch { event.message.editMessageComponents(event.message.components.map { it.asDisabled() }).await() }
            launch { startDecree(decree) }
        }}
    }

    private suspend fun startDecree(decree: Decree) = coroutineScope {
        state.nextTask = Task.OPEN_REGISTRATION
        state.election.decreeFormMessage = 0L
        state.selectedDecrees.add(decree.name)
        state.ignoredDecrees.addAll(pendingDecrees.map { it.name }.filter { it != decree.name })
        state.election.decrees.clear()
        state.election.decreeVotes.clear()
        saveState()
        decree.execute(true)
    }

    private suspend fun saveState(state: BotState) {
        withContext(Dispatchers.IO) {
            stateFile.writeBytes(cbor.encodeToByteArray(state))
        }
    }

    suspend fun saveState() {
        saveState(state)
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

    fun tallyDecreeVotes(): List<Decree> {
        val decrees = mutableMapOf<String, Int>()
        for (decree in state.election.decrees)
            decrees[decree] = 0
        for ((voter, decree) in state.election.decreeVotes.entries) {
            if (decree !in decrees) {
                logger.warn("User $voter voted for unknown decree $decree")
                continue
            }
            decrees[decree] = decrees[decree]!! + 1
        }
        return decrees.toList()
            .sortedWith(voteSorter)
            .take(2) // tie-break doesn't really matter
            .map { (name, _) -> allDecrees.find { decree -> decree.name == name }!! }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            jda.awaitReady()
            // init active decrees
            selectedDecrees.filter(Decree::persistent).forEach { it.execute(false) }
            // loop
            while (true) {
                // abort after April 1st
                // TODO: replace with check for remaining decrees; send goodbye message; schedule cleanup for 2hrs later
                val zdt = ZonedDateTime.now(ZoneOffset.UTC)
                if (zdt.month == Month.APRIL && zdt.dayOfMonth > 1)
                    break
                // begin election handling
                handleElection()
            }
        }
    }

    private suspend fun delayUntil(duration: Long) {
        val now = System.currentTimeMillis()
        val delayTo = now + duration - (now % duration)
        logger.info("Sleeping until ${Instant.ofEpochMilli(delayTo)} (Task: ${state.nextTask})")
        delay(delayTo - now)
    }

    private suspend fun delayUntil(duration: Duration) {
        delayUntil(duration.toMillis())
    }

    private suspend fun delayUntil(duration: kotlin.time.Duration) {
        delayUntil(duration.inWholeMilliseconds)
    }

    private suspend fun closeMessage(id: Long, name: String) {
        if (id == 0L) {
            logger.warn("Could not find $name message to disable components")
        } else {
            try {
                val message = channel.retrieveMessageById(id).await()
                message.editMessageComponents(message.components.map { it.asDisabled() }).await()
            } catch (e: Exception) {
                logger.warn("Failed to disable components for $name message", e)
            }
        }
    }

    private suspend fun handleElection() {
        when (state.nextTask) {
            Task.OPEN_REGISTRATION -> handleOpenRegistrationTask()
            Task.OPEN_BALLOT -> handleOpenBallotTask()
            Task.CLOSE_BALLOT -> handleCloseBallotTask()
            Task.CLOSE_TIEBREAK -> handleCloseTieBreakTask()
            Task.WELCOME_PM -> handleWelcomePMTask()
            Task.PM_TIMEOUT -> handlePMTimeoutTask()
        }
    }

    private suspend fun handleOpenRegistrationTask() {
        // wait for start of election cycle (top of the hour)
        delayUntil(1.hours)

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

        state.election.signupFormMessage = channel.sendMessage(messageData).await().idLong
        state.nextTask = Task.OPEN_BALLOT
        saveState()
    }

    private suspend fun handleOpenBallotTask() = coroutineScope {
        // sleep until XX:30
        delayUntil(30.minutes)

        // close signup form
        closeMessage(state.election.signupFormMessage, "signup form")
        state.election.signupFormMessage = 0L

        // announce ballot
        val electionOptions = state.election.candidates
            .mapNotNull { cand -> try { guild.retrieveMemberById(cand).await() } catch (e: Exception) { null } }
            .map { cand -> SelectOption.of(cand.effectiveName, cand.id) }
        val messageData = MessageCreate {
            content = buildString {
                append("The election has begun! Attached to this message, you will find the two sections of the ballot.\n")
                append("The first section is for the election of the next leader of our nation. ")
                append("As this nation follows the principles of approval voting, you may select all candidates whose platform you")
                if (electionOptions.isNotEmpty()) {
                    append(" support.\n")
                } else {
                    append("- _wait, what's that? uhuh. yep. ok got it, i'll let them know. ok, bye._\n")
                    append("It seems that none of you were brave enough to run for office. What a shame. ")
                    append("Not to fear, our elections are protected by contingencies upon contingencies. ")
                    append("The fearless **PRIME_MINISTER_9000** will be stepping in to fulfill the duties of the office until the next election cycle. ")
                    append("Now, as I was saying-\n")
                }
                append("The second section is for choosing the decree you wish to see enacted by the new leader. ")
                append("Only the top three most popular decrees will be considered for enactment, so choose wisely.")
            }
            components += listOf(
                row(StringSelectMenu("vote:candidate") {
                    if (electionOptions.isNotEmpty()) {
                        addOptions(electionOptions)
                    } else {
                        option("PRIME_MINISTER_9000", "PRIME_MINISTER_9000", "I WILL MAKE HTSTEM GREAT AGAIN", Emoji.fromUnicode("\uD83E\uDD16"), true)
                        isDisabled = true
                    }
                    setRequiredRange(1, SelectMenu.OPTIONS_MAX_AMOUNT)
                }),
                row(StringSelectMenu("vote:decree") {
                    for (decree in pendingDecrees) {
                        option(decree.name, decree.name, emoji = decree.emoji)
                    }
                })
            )
        }

        state.election.ballotFormMessage = channel.sendMessage(messageData).await().idLong
        state.nextTask = Task.CLOSE_BALLOT
        saveState()
    }

    private suspend fun handleCloseBallotTask() = coroutineScope {
        // sleep until XX:40
        delayUntil(10.minutes)

        // close ballot
        closeMessage(state.election.ballotFormMessage, "signup form")
        state.election.ballotFormMessage = 0L

        // close threads
        launch { channel.threadChannels.forEach { it.manager.setLocked(true).await() } }
        launch { state.election.candidates.forEach { guild.removeRoleFromMember(UserSnowflake.fromId(it), candidateRole).await() } }

        if (state.election.primeMinister != 0L) {
            guild.modifyMemberRoles(guild.retrieveMemberById(state.election.primeMinister).await(), setOf(pastLeaderRole), setOf(currentLeaderRole)).queue()
        }

        // tally candidate votes
        if (state.election.candidates.isEmpty()) {
            state.election.primeMinister = 0
            state.nextTask = Task.PM_TIMEOUT
            saveState()
            return@coroutineScope
        }

        val votes = mutableMapOf<Long, Int>()
        for (candidate in state.election.candidates)
            votes[candidate] = 0
        for ((voter, vote) in state.election.candidateVotes.entries) {
            for (candidate in vote) {
                if (candidate !in votes) {
                    logger.warn("User $voter voted for unknown candidate $candidate")
                    continue
                }
                votes[candidate] = votes[candidate]!! + 1
            }
        }
        val sortedVotes = votes.toList().sortedWith(voteSorter)
        val winners = sortedVotes.takeWhile { it.second == sortedVotes.first().second }.map { it.first }

        state.election.candidates.clear()
        state.election.candidateVotes.clear()

        if (winners.size == 1) {
            state.election.primeMinister = winners[0]
            state.nextTask = Task.WELCOME_PM
            saveState()
            return@coroutineScope
        }

        // resort to 5min FPTP tiebreaker
        state.election.tieBreakCandidates.addAll(winners)
        state.election.tieBreakFormMessage = channel.sendMessage(MessageCreate {
            content = buildString {
                append("Ah, an indecisive bunch, are we? ")
                append("Alright, I'll give you all five minutes to try to sort this tie before I step in and pick randomly. ")
                append("Please select your favorite of the candidates below.")
            }
            components += row(StringSelectMenu("vote:tiebreak") {
                for (candidate in winners) {
                    option(guild.retrieveMemberById(candidate).await().effectiveName, candidate.toString())
                }
            })
        }).await().idLong

        state.nextTask = Task.CLOSE_TIEBREAK
        saveState()
    }

    private suspend fun handleCloseTieBreakTask() = coroutineScope {
        // sleep until XX:45
        delayUntil(5.minutes)

        // close ballot
        closeMessage(state.election.tieBreakFormMessage, "tie-break form")
        state.election.tieBreakFormMessage = 0L

        // fetch votes
        val votes = mutableMapOf<Long, Int>()
        for ((voter, candidate) in state.election.tieBreakVotes) {
            if (candidate !in state.election.tieBreakCandidates) {
                logger.warn("User $voter voted for unknown candidate $candidate")
                continue
            }
            votes[candidate] = votes[candidate]!! + 1
        }
        val newSortedVotes = votes.toList().sortedWith(voteSorter)

        // save data
        state.election.tieBreakVotes.clear()
        state.election.tieBreakCandidates.clear()
        state.election.primeMinister = newSortedVotes.takeWhile { it.second == newSortedVotes.first().second }.map { it.first }.random()
        state.nextTask = Task.WELCOME_PM
        saveState()
    }

    private suspend fun handleWelcomePMTask() = coroutineScope {
        val topDecrees = tallyDecreeVotes()

        // send welcomes
        guild.addRoleToMember(UserSnowflake.fromId(state.election.primeMinister), currentLeaderRole).queue()

        channel.sendMessage(buildString {
            append("Congratulations, <@").append(state.election.primeMinister).append(">! ")
            append("Through the due and just democratic process, a body of your peers have fairly elected you as Prime Minster of ")
            append(guild.name).append(". ")
            append("The people now call upon you to pass just one law to bring back peace and stability to our great nation. ")
            append("Your constituents have helped you narrow it down to just two choices, ")
            append(topDecrees[0].displayName)
            append(" or ")
            append(topDecrees[1].displayName)
            append(". Please, choose wisely.")
        }).await()

        // "DM" decree form to winner
        state.election.decreeFormMessage = pmChannel.sendMessage(MessageCreate {
            content = buildString {
                append("Welcome to your own personal oval office, <@").append(state.election.primeMinister).append(">. ")
                append("Quickly now, there's no time to waste. ")
                append("We need you to pass a new law to help save our country. ")
                append("Your constituents have helped narrow it down to two. ")
                append("All you need to do now is click below to select which law to enact. ")
                append("If you fail to do so in the next 10 minutes, I'll make your choice for you.\n\n")
                append("To help you make your choice, I have some extra information on each decree:\n>>>")
                for (decree in topDecrees) {
                    append("**").append(decree.displayName).append(":** ").append(decree.description).append('\n')
                }
            }
            components += row(
                *topDecrees.map { button("pick-decree:${it.name}", it.name, it.emoji, ButtonStyle.PRIMARY) }.toTypedArray()
            )
            mentions { user(state.election.primeMinister) }
        }).await().idLong

        state.nextTask = Task.PM_TIMEOUT
        saveState()
    }

    private suspend fun handlePMTimeoutTask() = coroutineScope {
        // wait until XX:00
        delayUntil(20.minutes)

        // check this is still the right task
        // TODO: so if I want to do this every 2 hours still
        //  then I think this is kinda accidentally a good way to do it?
        //  since I think this waits until like :00
        //  and then the next task again waits for :00
        //  yeah??? idk
        if (state.nextTask != Task.PM_TIMEOUT) return@coroutineScope
        if (state.election.decrees.isEmpty()) return@coroutineScope // uhh

        val topDecrees = tallyDecreeVotes()
        val decree = topDecrees.random()

        if (state.election.primeMinister != 0L) {
            closeMessage(state.election.decreeFormMessage, "decree form") // gets closed in startDecree
            launch { channel.sendMessage(buildString {
                append("I see you are indecisive. Very well. As your loyal vice prime minister, I shall enact a law for you. Good day.")
            }).await() }
        }

        launch { channel.sendMessage(buildString {
            if (state.election.primeMinister == 0L) {
                append("HELLO >w< IT IS I, PRIME_MINISTER_9000 UwU~! ")
                append("I HAVE COME TO FULFILL THE MORAL OBLIGATIONS OF A SITTING PRIME MINISTER SINCE NOBODY ELSE WANTED TO ^.^ ")
                append("AS P.M., MY FIRST ACTION IS TO ENACT ")
            } else {
                append("Your Prime Minister has failed to pass a law, and so as their loyal vice prime minister I have chosen to enact ")
            }
            append("**")
            append(decree.displayName)
            append("**: ")
            append(decree.description)
            append('\n')
            if (state.election.primeMinister == 0L) {
                append("PLEASE ENJOY \\o/")
            } else {
                append("Glory to ")
                append(guild.name)
                append(".")
            }
        }).await() }

        launch { startDecree(decree) } // handles the usual task update + save
    }
}
