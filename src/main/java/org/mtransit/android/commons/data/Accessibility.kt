package org.mtransit.android.commons.data

@Suppress("MemberVisibilityCanBePrivate", "unused")
object Accessibility {

    const val UNKNOWN = 0 // no info
    const val POSSIBLE = 1
    const val NOT_POSSIBLE = 2

    const val UNKNOWN_CHAR = "" // TODO ?
    const val POSSIBLE_CHAR = "[a11y]"
    const val NOT_POSSIBLE_CHAR = "[!a11y]"

    const val DEFAULT = UNKNOWN

    const val HTML_POSSIBLE = "&#9855;"

    @JvmStatic
    @JvmOverloads
    fun decorate(name: String, accessible: Int, before: Boolean = false): String {
        return when (accessible) {
            POSSIBLE -> if (name.isEmpty()) POSSIBLE_CHAR else if (before) "$POSSIBLE_CHAR $name" else "$name $POSSIBLE_CHAR"
            NOT_POSSIBLE -> if (name.isEmpty()) NOT_POSSIBLE_CHAR else if (before) "$NOT_POSSIBLE_CHAR $name" else "$name $NOT_POSSIBLE_CHAR"
            else -> name
        }
    }

    @JvmStatic
    fun combine(stopAccessible: Int, tripAccessible: Int): Int {
        if (stopAccessible == tripAccessible) {
            return stopAccessible
        }
        if (stopAccessible == UNKNOWN) {
            return tripAccessible
        }
        if (tripAccessible == UNKNOWN) {
            return stopAccessible
        }
        if (stopAccessible == NOT_POSSIBLE || tripAccessible == NOT_POSSIBLE) {
            return NOT_POSSIBLE
        }
        return UNKNOWN
    }
}