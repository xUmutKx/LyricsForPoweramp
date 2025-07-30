package io.github.abhishekabhi789.lyricsforpoweramp.model

data class Timestamp(
    val minutes: Int,
    val seconds: Int,
    val centiseconds: Int
) {
    override fun toString(): String = "[%02d:%02d.%02d]".format(minutes, seconds, centiseconds)

    fun increase(deltaCentiseconds: Int): Timestamp {
        val total = toTotalCentiseconds() + deltaCentiseconds
        return fromTotalCentiseconds(total)
    }

    fun decrease(deltaCentis: Int): Timestamp {
        val total = (toTotalCentiseconds() - deltaCentis).coerceAtLeast(0)
        return fromTotalCentiseconds(total)
    }

    private fun toTotalCentiseconds(): Int {
        return (((minutes * 60) + seconds) * 100) + centiseconds
    }

    companion object {
        fun fromString(timestamp: String): Timestamp? {
            val match =
                Regex("\\[(\\d{2,}):(\\d{2})\\.(\\d{2})]").matchEntire(timestamp) ?: return null
            val (mm, ss, cc) = match.destructured
            return Timestamp(mm.toInt(), ss.toInt(), cc.toInt())
        }

        private fun fromTotalCentiseconds(total: Int): Timestamp {
            val minutes = total / 6000
            val seconds = (total % 6000) / 100
            val centiseconds = total % 100
            return Timestamp(minutes, seconds, centiseconds)
        }
    }
}
