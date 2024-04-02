package dev.qixils.demowocwacy

fun String.truncate(limit: Int): String {
    if (length > limit)
        return take(limit - 1) + '…'
    return this
}