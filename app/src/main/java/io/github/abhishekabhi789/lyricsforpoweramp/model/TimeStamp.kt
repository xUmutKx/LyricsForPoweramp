package io.github.abhishekabhi789.lyricsforpoweramp.model

data class Timestamp(
    val minutes: Int,
    val seconds: Int,
    val centiseconds: Int
) {
    override fun toString(): String = "[%02d:%02d.%02d]".format(minutes, seconds, centiseconds)

    fun increase(deltaCentiseconds: Int): Timestamp {
        val total = toTotalCentiseconds() + deltaCentiseconds
        return fromMillis(total.times(10L))
    }

    fun decrease(deltaCentiseconds: Int): Timestamp {
        val total = (toTotalCentiseconds() - deltaCentiseconds).coerceAtLeast(0)
        return fromMillis(total.times(10L))
    }

    fun toTotalCentiseconds(): Int {
        return (((minutes * 60) + seconds) * 100) + centiseconds
    }

    companion object {
        fun fromString(timestamp: String): Timestamp? {
            val match =
                Regex("\\[(\\d{2,}):(\\d{2})\\.(\\d{2})]").matchEntire(timestamp) ?: return null
            val (mm, ss, cc) = match.destructured
            return Timestamp(mm.toInt(), ss.toInt(), cc.toInt())
        }

        fun fromMillis(ms: Long): Timestamp {
            return ms.div(10).let { centiseconds ->
                Timestamp(
                    minutes = (centiseconds / 6000).toInt(),
                    seconds = ((centiseconds % 6000) / 100).toInt(),
                    centiseconds = (centiseconds % 100).toInt()
                )
            }
        }
    }
}
