package dev.qixils.demowocwacy.decrees

import dev.qixils.demowocwacy.decrees.base.AutoModDecree

class NoFifthGlyphDecree : AutoModDecree(
    "Stabyourslf",
    "\uD83C\uDDEA",
    "Post without using our fifth glyph",
//    true,
) {
//    override suspend fun execute(init: Boolean) {
//        Bot.jda.listener<MessageReceivedEvent> { event ->
//            if (!isApplicableTo(event.message)) return@listener
//
//            if (!event.member!!.effectiveName.contains('e') && !event.message.contentRaw.contains('e')) return@listener
//
//            event.message.delete().reason(event.message.id).queueAfter(500, TimeUnit.MILLISECONDS)
//        }
//    }
}