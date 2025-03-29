package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.Icon.IconType
import net.dv8tion.jda.api.utils.ImageProxy

class HTCDecree : Decree(
    "Yes Pacman",
    "🍒",
    "Embrace the superior game",
    false
) {
    companion object {
        private const val IMAGE_URL = "https://i.qixils.dev/pacyourself.png"
    }

    override suspend fun execute(init: Boolean) {
        val destGuild = Bot.jda.getGuildById(Bot.config.guild)!!

        val invert = Bot.selectedDecrees.filterIsInstance<InvertDecree>().firstOrNull()

        val icon: Icon = if (invert != null) {
            val iconUrl = IMAGE_URL.replace(Regex("\\.gif$"), ".png")
            invert.invert(ImageProxy(iconUrl))
        } else {
            val image: ImageProxy = ImageProxy(IMAGE_URL)
            val animated = image.url.endsWith(".gif")
            val type = if (animated) IconType.GIF else IconType.PNG
            Icon.from(image.download(4096).await(), type)
        }

        destGuild.manager.setIcon(icon).await()
    }
}