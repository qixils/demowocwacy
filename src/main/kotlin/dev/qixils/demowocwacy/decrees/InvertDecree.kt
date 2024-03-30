package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.utils.ImageProxy
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


class InvertDecree : Decree(
    "Invert",
    "\uD83C\uDF17",
    "Inverts the server's colors",
    false
) {
    private fun invertRGB(rgb: Int): Int {
        // Extract red, green, and blue components
        var red = rgb shr 16 and 0xFF
        var green = rgb shr 8 and 0xFF
        var blue = rgb and 0xFF

        // Invert each component
        red = 255 - red
        green = 255 - green
        blue = 255 - blue

        // Combine components into a single RGB value
        return red shl 16 or (green shl 8) or blue
    }

    private suspend fun invert(imageProxy: ImageProxy): Icon {
        val inputStream = imageProxy.download(4096).await()
        // Read the image from the InputStream
        val image = withContext(Dispatchers.IO) {
            ImageIO.read(inputStream)
        }

        // Get image width and height
        val width = image.width
        val height = image.height

        // Invert colors
        for (y in 0..<height) {
            for (x in 0..<width) {
                // Get the RGB color of the pixel
                val rgb = image.getRGB(x, y)

                // Invert the color
                val invertedRGB: Int = invertRGB(rgb)

                // Set the inverted color to the pixel
                image.setRGB(x, y, invertedRGB)
            }
        }

        // Write the modified image to the OutputStream
        val os = ByteArrayOutputStream()
        return withContext(Dispatchers.IO) {
            ImageIO.write(image, "png", os)
            Icon.from(os.toByteArray(), Icon.IconType.PNG)
        }
    }

    private suspend fun invertGuildIcon() {
        val iconId = Bot.guild.iconId!!
        val iconUrl = String.format(
            Guild.ICON_URL,
            Bot.guild.id,
            iconId,
            "png"
        )

        Bot.guild.manager.setIcon(invert(ImageProxy(iconUrl))).await()
    }

    override suspend fun execute() {
        invertGuildIcon()
        for (emoji in Bot.guild.emojis.sortedBy { it.timeCreated }) {
            if (emoji.isAnimated) continue
            val inverted = invert(emoji.image)
            emoji.delete().await()
            Bot.guild.createEmoji(emoji.name, inverted, *emoji.roles.toTypedArray()).await()
        }
        for (role in Bot.guild.roles) {
            val color = role.color ?: continue
            role.manager.setColor(invertRGB(color.rgb))
        }
    }

    override suspend fun cleanup() {
        execute()
    }
}