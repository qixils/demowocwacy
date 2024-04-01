package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.Permission

class PeanutDecree : Decree(
    "Peanuts",
    "\uD83E\uDD5C",
    "peanuts",
    true,
) {
    override suspend fun execute(init: Boolean) {
        Bot.guild.publicRole.manager.revokePermissions(Permission.NICKNAME_CHANGE).await()
        for (member in Bot.guild.members) {
            if (member.nickname != null) {
                // TODO: save & revert
                Bot.logger.info("Changing nickname of user $member from ${member.nickname}")
            }
            member.modifyNickname("\uD83E\uDD5C").await()
        }
    }
}