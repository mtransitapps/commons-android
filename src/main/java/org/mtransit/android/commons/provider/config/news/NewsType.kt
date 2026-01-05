package org.mtransit.android.commons.provider.config.news

enum class NewsType(val id: Int) {

    RSS(0),
    YOUTUBE(1),
    TWITTER(2),
    INSTAGRAM(3),

    CA_STO(100),
    CA_WINNIPEG(101),

    ;

    companion object {
        fun fromId(id: Int) = entries.singleOrNull { it.id == id }
    }
}