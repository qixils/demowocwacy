package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.Icon.IconType
import net.dv8tion.jda.api.utils.ImageProxy

class HTCDecree : Decree(
    "Become HTC",
    "\uD83D\uDCD7",
    "Become a clone of the sister server HTwins Central",
    false
) {
    companion object {
        private const val FALLBACK = "https://cdn.discordapp.com/icons/184755239952318464/a_986b6070401b3d2053ea35b6252c6ac9.gif"
    }

    override suspend fun execute(init: Boolean) {
        val sourceGuild = Bot.jda.getGuildById(Bot.config.decrees.htc.guild)
        val destGuild = Bot.jda.getGuildById(Bot.config.guild)!!

        val invert = Bot.selectedDecrees.filterIsInstance<InvertDecree>().firstOrNull()

        val icon: Icon = if (invert != null) {
            val iconUrl = sourceGuild?.let {
                String.format(
                    Guild.ICON_URL,
                    Bot.guild.id,
                    it.iconId,
                    "png"
                )
            } ?: FALLBACK.replace(Regex("\\.gif$"), ".png")
            invert.invert(ImageProxy(iconUrl))
        } else {
            val image: ImageProxy = sourceGuild?.icon ?: ImageProxy(FALLBACK)
            val animated = sourceGuild?.iconId?.startsWith("a_") ?: FALLBACK.endsWith(".gif")
            val type = if (animated) IconType.GIF else IconType.PNG
            Icon.from(image.download(4096).await(), type)
        }

        destGuild.manager.setIcon(icon).await()
    }
}

@Serializable
data class HTCConfig (
    val guild: Long = 0, // source of impersonation
)