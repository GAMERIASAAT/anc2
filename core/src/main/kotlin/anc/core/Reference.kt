package anc.core

/** External reference for a module (CVE, advisory URL, etc.). */
sealed class Reference {
    abstract val display: String

    data class CVE(val id: String) : Reference() {
        override val display = "CVE-$id"
    }
    data class URL(val url: String) : Reference() {
        override val display = url
    }
    data class Advisory(val id: String, val source: String = "") : Reference() {
        override val display = if (source.isNotBlank()) "$source-$id" else id
    }
    data class BID(val id: String) : Reference() {
        override val display = "BID-$id"
    }
}
