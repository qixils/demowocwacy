package dev.qixils.demowocwacy.decrees

import dev.qixils.demowocwacy.decrees.base.AutoModDecree

// TODO: remember to exlcude 9.0 lol
class NoFirstGlyphDecree : AutoModDecree(
    "Stbyourself",
    "\uD83C\uDDE6",
    "Best to get in front of this l\\*wsuit from 9.0, looks like he still owns the tr\\*dem\\*rk on the first glyph",
//    true,
) {
//    override suspend fun execute(init: Boolean) {
//        Bot.jda.listener<MessageReceivedEvent> { event ->
//            if (!isApplicableTo(event.message)) return@listener
//
//            if (!event.member!!.effectiveName.contains('a') && !event.message.contentRaw.contains('a')) return@listener
//
//            event.message.delete().reason(event.message.id).queueAfter(500, TimeUnit.MILLISECONDS)
//        }
//    }
}