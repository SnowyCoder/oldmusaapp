package it.cnr.oldmusa.util

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String?,
    val build: String?
) {
    fun isForwardsCompatibleTo(other: SemVer): Boolean {
        if (this.major == 0) return this == other
        if (this.major != other.major) return false
        if (this.minor > other.minor) return false
        return true
    }

    override fun toString(): String {
        return "$major.$minor.$patch" +
                if (preRelease != null) { "-$preRelease" } else { "" } +
                if (build != null) { "+$build" } else { "" }
    }

    companion object {
        // This is a bit lexer than the original
        val regex =
            Regex("""^(0|[1-9]\d*)(?:\.(0|[1-9]\d*)(?:\.(0|[1-9]\d*))?)?(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?${'$'}""")


        fun parseOrNull(str: String): SemVer? {
            val result = regex.matchEntire(str) ?: return null

            val groups = result.groups
            return SemVer(
                groups[1]!!.value.toInt(),
                groups[2]?.value?.toInt() ?: 0,
                groups[3]?.value?.toInt() ?: 0,
                groups[4]?.value,
                groups[5]?.value
            )
        }
    }
}