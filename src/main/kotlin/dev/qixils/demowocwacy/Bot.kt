package dev.qixils.demowocwacy

import com.typesafe.config.ConfigFactory
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import dev.qixils.demowocwacy.decrees.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.requests.GatewayIntent
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
object Bot {

    val hocon = Hocon {
        useConfigNamingConvention = true
    }
    val cbor = Cbor {  }

    private val configFile = File("bot.conf")
    private val stateFile = File("state.cbor")

    val jda: JDA
    val config: BotConfig
    val allDecrees: List<Decree>
    val selectedDecrees: List<Decree>
        get() = allDecrees.filter { it.name in state.selectedDecrees }
    val ignoredDecrees: List<Decree>
        get() = allDecrees.filter { it.name in state.ignoredDecrees }
    val remainingDecrees: List<Decree>
        get() = allDecrees.filter { it.name !in state.selectedDecrees && it.name !in state.ignoredDecrees }

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
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // election cycle
        ElectionCycle.start()
    }
}
