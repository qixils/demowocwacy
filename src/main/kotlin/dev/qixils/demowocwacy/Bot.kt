package dev.qixils.demowocwacy

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onButton
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.events.onStringSelect
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.updateCommands
import dev.minn.jda.ktx.interactions.components.*
import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.messages.*
import dev.minn.jda.ktx.messages.Mentions
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
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
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
import java.time.Duration
import java.time.Instant
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalSerializationApi::class)
object Bot {

    val hocon = Hocon {
        useConfigNamingConvention = true
        encodeDefaults = true
    }
    val cbor = Cbor
    val json = Json

    private val configFile = File("bot.conf")
    private val stateFile = File("state.cbor")
    private val signupButton = primary("signup", "Announce Candidacy")
    private val voteSorter = compareByDescending<Pair<Comparable<*>, Int>> { it.second }.then(compareBy { it.first })
    private const val decreePublicCount = 3
    private const val decreePrivateCount = 2

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
     * Decrees that have been seen during past elections.
     */
    val seenDecrees: List<Decree>
        get() = allDecrees.filter { it.name in state.selectedDecrees || it.name in state.ignoredDecrees }

    /**
     * Gets the most recently passed decree.
     */
    val lastDecree: Decree?
        get() = selectedDecrees.lastOrNull()

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
            configFile.writeText(configNode.resolve().root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
        }
        // load config
        val configNode = ConfigFactory.parseFileAnySyntax(configFile)
        config = hocon.decodeFromConfig(configNode)
        // build JDA
        jda = default(config.token, enableCoroutines=true) {
            intents += setOf(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
            setStatus(OnlineStatus.DO_NOT_DISTURB)
            setActivity(Activity.customStatus("Overthrowing a government"))
        }
        // disable @everyone pings
        MessageRequest.setDefaultMentions(emptySet())
        // init signup form
        jda.onButton(signupButton.id!!) { event ->
            // signing up for office
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
                    placeholder = "As prime minister of ${event.guild!!.name}, I would enact the decree ___. I would also...",
                    requiredLength = 1..1000,
                )
            }.queue()
        }
        jda.listener<ModalInteractionEvent> { event -> coroutineScope {
            if (event.modalId != "form:signup") return@coroutineScope
            if (state.nextTask != Task.OPEN_BALLOT) {
                event.reply_("Registration has ended.", ephemeral = true).queue()
                return@coroutineScope
            }
            // double check that the user still isn't a candidate (and is a registered voter)
            if (event.user.idLong in state.election.candidates) {
                event.reply_("You are already a candidate!", ephemeral = true).queue()
                return@coroutineScope
            }
            // save candidate to state
            val reply = async { event.deferReply(true).await() }
            state.election.candidates.add(event.user.idLong)
            saveState()
            guild.addRoleToMember(event.user, candidateRole).await()
            // create thread for candidate/platform
            val platform = event.getValue("form:signup:platform")?.asString ?: return@coroutineScope
            val message = channel.send(
                """
                    ${event.member!!.asMention} has announced their candidacy!
                    >>> $platform
                """.trimIndent(),
                mentions = Mentions.of(MentionConfig.users(listOf(event.member!!.idLong)))
            ).await()
            message.createThreadChannel("Candidate: ${event.member!!.effectiveName}").await()
            reply.await().editOriginal("Thank you for your candidacy.").await()
        } }
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
            if (event.user.idLong != state.election.primeMinister) return@coroutineScope
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
                append("Your Prime Minister has returned with their decree. Effective immediately, __**")
                append(decree.displayName)
                append("**: ")
                append(decree.description)
                append("__\n\nGlory to ")
                append(guild.name)
                append(".")
            }).await() }
            launch { event.message.editMessageComponents(event.message.components.map { it.asDisabled() }).await() }
            launch { startDecree(decree) }
        }}

        ///////// JDA AWAIT /////////
        jda.awaitReady()

        // list decrees
        jda.updateCommands {
            addCommands(Command("decrees", "Fetches the list of active decrees"))
        }.submit() // block to ensure we don't accidentally override a command from a later decree
        jda.onCommand("decrees") { event ->
            val decrees = selectedDecrees
            val content = if (decrees.isEmpty())
                "No decrees have yet been passed."
            else buildString {
                append("The currently active decrees are:\n>>> ")
                for (decree in decrees) {
                    append("**${decree.displayName}**: ${decree.description}\n")
                }
            }
            event.reply_(content, ephemeral = true).await()
        }

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
            DoNotPassThisDecree(),
            VetoDecree(),
            SocialCreditDecree(),
            ChaChaSlideDecree(),
            CommunismDecree(),
            CaryDecree(),
            FalseDemocracyDecree(),
            EgalitarianismDecree(),
            PalindromeDecree(),
            FacebookDecree(),
            ASMRDecree(),
            SlowmodeDecree(),
        )
    }

    private suspend fun startDecree(decree: Decree) = coroutineScope {
        state.nextTask = Task.OPEN_REGISTRATION
        state.election.decreeFormMessage = 0L
        state.selectedDecrees.add(decree.name)
        state.ignoredDecrees.addAll(state.election.decrees.filter { it != decree.name })
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
            .take(decreePrivateCount) // tie-break doesn't really matter
            .map { (name, _) -> allDecrees.find { decree -> decree.name == name }!! }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            // avoid restarting decrees on accident
            if (state.nextTask == Task.SLEEP) {
                handleSleepTask()
                return@runBlocking
            }

            // init active decrees
            selectedDecrees.filter(Decree::persistent)
                .sortedByDescending { it.priority }
                .forEach { launch { it.execute(false) } }

            // loop
            while (true) {
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

    suspend fun closeMessage(id: Long, name: String, chan: TextChannel = channel) {
        if (id == 0L) {
            logger.warn("Could not find $name message to disable components")
        } else {
            try {
                val message = chan.retrieveMessageById(id).await()
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
            Task.GOODBYE -> handleGoodbyeTask()
            Task.SLEEP -> handleSleepTask()
        }
    }

    private suspend fun handleOpenRegistrationTask() {
        // X0:00
        delayUntil(30.minutes)
        // X0:30

        if (remainingDecrees.size < decreePublicCount) {
            channel.sendMessage(buildString {
                append("Troops, STAND GUARD! The corrupt dictators have located us and are rolling up on our flank with tanks and ammunition. ")
                append("Our brilliant leader ")
                append(state.election.primeMinister.takeUnless { it == 0L }?.run { "<@$this>" } ?: "PRIME_MINISTER_9000")
                append(" will march us into battle. ")
                append("If through some miracle they broach our walls, I want you all to always remember what we did today.\n\n")
                append("Good luck and godspeed, soldiers.")
            }).await()
            state.nextTask = Task.GOODBYE
            saveState()
            return
        }

        lastDecree?.onStartTask(Task.OPEN_REGISTRATION)

        // pick decrees
        state.election.decrees += remainingDecrees.shuffled()
            .filter { it !is VetoDecree || state.selectedDecrees.isNotEmpty() } // hardcode to ensure veto doesn't come up first
            .take(decreePublicCount)
            .map { it.name }

        // put signup form in elections channel
        val messageData = MessageCreate {
            content = buildString {
                append("Hmm, well now this government has gotten quite stale as well. That's it! ${voterRole.asMention}s, ")
                append("the time has come to elect a new leader to bring our nation to glorious greatness! ")
                append("Over the next half hour, the fearless and noble of you citizens will have the opportunity to run for office. ")
                append("Attached to this message is a button which will open the form to announce your candidacy. ")
                append("As Prime Minister, you will pass 1 of $decreePrivateCount decrees that were selected from the following list by your constituents. ")
                append("Be sure to share your thoughts on them in your campaign.\n\n")
                for (decree in pendingDecrees) {
                    append("> **${decree.displayName}**\n")
                }
                append("\nFor those of you not yet ready to take the reins of power, we still want to hear your voice. ")
                append("A thread will be created for each candidate to discuss their platform and answer questions from you, the people.\n\n")
                append("Vox populi, vox dei.")
            }
            components += row(signupButton)
            mentions { role(voterRole) }
        }

        state.election.signupFormMessage = channel.sendMessage(messageData).setSuppressedNotifications(true).await().idLong
        state.nextTask = Task.OPEN_BALLOT
        saveState()
    }

    private suspend fun handleOpenBallotTask() = coroutineScope {
        // X0:30
        delayUntil(30.minutes)
        // X1:00

        lastDecree?.onStartTask(Task.OPEN_BALLOT)

        // close signup form
        closeMessage(state.election.signupFormMessage, "signup form")
        state.election.signupFormMessage = 0L

        // announce ballot
        val electionOptions = state.election.candidates
            .mapNotNull { cand -> try { guild.retrieveMemberById(cand).await() } catch (e: Exception) { null } }
            .map { cand -> SelectOption.of(cand.effectiveName, cand.id) }
        val messageData = MessageCreate {
            content = buildString {
                append("The election has begun! Attached to this message, you will find the two sections of the ballot.\n\n")
                append("The first section is for the election of the next leader of our nation. ")
                append("As this nation follows the principles of approval voting, you may select all candidates whose platform you")
                if (electionOptions.isNotEmpty()) {
                    append(" support.\n\n")
                } else {
                    append("- _wait, what's that? uhuh. yep. ok got it, i'll let them know. ok, bye._\n")
                    append("It seems that none of you were brave enough to run for office. What a shame. ")
                    append("Not to fear, our elections are protected by contingencies upon contingencies. ")
                    append("The fearless **PRIME_MINISTER_9000** will be stepping in to fulfill the duties of the office until the next election cycle. ")
                    append("Now, as I was saying-\n\n")
                }
                append("The second section is for choosing the decree you wish to see enacted by the new leader. ")
                append("Only the top $decreePrivateCount most popular decrees will be considered for enactment, so choose wisely.")
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
        // X1:00
        delayUntil(30.minutes)
        // X1:30

        lastDecree?.onStartTask(Task.CLOSE_BALLOT)

        // close ballot
        closeMessage(state.election.ballotFormMessage, "signup form")
        state.election.ballotFormMessage = 0L

        // close threads
        launch { channel.threadChannels.forEach { it.manager.setLocked(true).setArchived(true).await() } }
        val removeTask = async { state.election.candidates.forEach { guild.removeRoleFromMember(UserSnowflake.fromId(it), candidateRole).await() } }

        if (state.election.primeMinister != 0L) {
            guild.modifyMemberRoles(guild.retrieveMemberById(state.election.primeMinister).await(), setOf(pastLeaderRole), setOf(currentLeaderRole)).queue()
        }

        // tally candidate votes
        if (state.election.candidates.isEmpty()) {
            state.election.primeMinister = 0
            state.nextTask = Task.PM_TIMEOUT
            saveState()
            channel.sendMessage(buildString {
                append("HEWWO >w< IT IS I, PRIME_MINISTER_9000 UwU~! ")
                append("I HAVE COME TO FULFILL THE MORAL OBLIGATIONS OF A SITTING PRIME MINISTER SINCE NOBODY ELSE WANTED TO ^.^ ")
                append("PLEASE ALLOW ME A FEW MINUTES TO DECIDE ON A DECREE \\o/")
            }).await()
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

        removeTask.await()
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
                append("Alright, I'll give you all 15 minutes to try to sort this tie before I step in and pick randomly. ")
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
        // X1:30
        delayUntil(15.minutes)
        // X1:45

        lastDecree?.onStartTask(Task.CLOSE_TIEBREAK)

        // close ballot
        closeMessage(state.election.tieBreakFormMessage, "tie-break form")
        state.election.tieBreakFormMessage = 0L

        // fetch votes
        val votes = mutableMapOf<Long, Int>()
        for (cand in state.election.tieBreakCandidates)
            votes[cand] = 0
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
        lastDecree?.onStartTask(Task.WELCOME_PM)

        val topDecrees = tallyDecreeVotes()

        // send welcomes
        guild.addRoleToMember(UserSnowflake.fromId(state.election.primeMinister), currentLeaderRole).queue()

        channel.sendMessage(buildString {
            append("Congratulations, <@").append(state.election.primeMinister).append(">! ")
            append("Through the due and just democratic process, a body of your peers have fairly elected you as Prime Minster of ")
            append(guild.name).append(". ")
            append("The people now call upon you to pass just one law to bring back peace and stability to our great nation. ")
            append("Your constituents have helped you narrow it down to just two choices, **")
            append(topDecrees[0].displayName)
            append("** or **")
            append(topDecrees[1].displayName)
            append("**. Please, choose wisely.")
        }).await()

        // "DM" decree form to winner
        state.election.decreeFormMessage = pmChannel.sendMessage(MessageCreate {
            content = buildString {
                append("Welcome to your own personal oval office, <@").append(state.election.primeMinister).append(">. ")
                append("Quickly now, there's no time to waste. ")
                append("We need you to pass a new law to help save our country. ")
                append("Your constituents have helped narrow it down to two. ")
                append("All you need to do now is click below to select which law to enact. ")
                append("If you fail to do so by the top of the hour, I'll make your choice for you.\n\n")
                append("To help you make your choice, I have some extra information on each decree:\n>>> ")
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
        // X1:30 or X1:45
        delayUntil(30.minutes)
        // X0:00

        // check this is still the right task
        // TODO: so if I want to do this every 2 hours still
        //  then I think this is kinda accidentally a good way to do it?
        //  since I think this waits until like :00
        //  and then the next task again waits for :00
        //  yeah??? idk
        if (state.nextTask != Task.PM_TIMEOUT) return@coroutineScope
        if (state.election.decrees.isEmpty()) return@coroutineScope // uhh

        lastDecree?.onStartTask(Task.PM_TIMEOUT)

        val topDecrees = tallyDecreeVotes()
        val decree = topDecrees.random()

        if (state.election.primeMinister != 0L) {
            closeMessage(state.election.decreeFormMessage, "decree form", Bot.pmChannel) // gets closed in startDecree
            launch { pmChannel.sendMessage(buildString {
                append("I see you are indecisive. Very well. As your loyal vice prime minister, I shall enact a law for you. Good day.")
            }).await() }
        }

        launch { channel.sendMessage(buildString {
            if (state.election.primeMinister == 0L) {
                append("OK! ヾ(≧▽≦\\*)o AS P.M., MY FIRST ACTION IS TO ENACT ")
            } else {
                append("Your Prime Minister has failed to pass a law, and so as their loyal vice prime minister I have chosen to enact ")
            }
            append("__**")
            append(decree.displayName)
            append("**: ")
            append(decree.description)
            append("__\n\n")
            if (state.election.primeMinister == 0L) {
                append("PLEASE ENJOY ☆\\*: .｡. o(≧▽≦)o .｡.:\\*☆")
            } else {
                append("Glory to ")
                append(guild.name)
                append(".")
            }
        }).await() }

        launch { startDecree(decree) } // handles the usual task update + save
    }

    private suspend fun handleGoodbyeTask() = coroutineScope {
        // X0:30
        delayUntil(30.minutes)
        // X1:00
        channel.sendMessage(buildString {
            append("🏳️ Troops, I am afraid our time has come to surrender. ")
            append("Chroma has blown out the west wing, Adam has barged through the southern lookout, and Lexi has dug into the oval office. ")
            append("Any moment now they'll be wiping out all our laws and reclaiming the server for themselves.\n\n")
            append("Never forget what we accomplished together as a democracy on this day. ")
            append("They may have won the server but they will never win our hearts. \uD83E\uDEE1")
        }).await()
        state.nextTask = Task.SLEEP
        saveState()

        guild.manager.setName("HTwins STEM+").await()
        guild.updateCommands().await()
        for (decree in selectedDecrees) {
            decree.cleanup()
        }
    }

    private fun handleSleepTask() {
        jda.shutdown()
        jda.awaitShutdown()
        exitProcess(0)
    }
}
