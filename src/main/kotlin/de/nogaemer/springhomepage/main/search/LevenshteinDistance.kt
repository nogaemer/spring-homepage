package de.nogaemer.springhomepage.main.search

object LevenshteinDistance {
    fun distance(aRaw: String, bRaw: String): Int {
        val a = aRaw.lowercase()
        val b = bRaw.lowercase()

        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,        // delete
                    dp[i][j - 1] + 1,        // insert
                    dp[i - 1][j - 1] + cost  // replace
                )
            }
        }

        return dp[a.length][b.length]
    }
}
