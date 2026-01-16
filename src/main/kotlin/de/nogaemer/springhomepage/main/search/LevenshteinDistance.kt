/**
 * Utility object implementing the Levenshtein distance algorithm for string similarity.
 *
 * The Levenshtein distance is the minimum number of single-character edits (insertions,
 * deletions, or substitutions) required to transform one string into another.
 * This is used for "did you mean?" suggestions when ingredient searches don't match exactly.
 *
 * Algorithm: Dynamic programming approach with O(m*n) time and space complexity,
 * where m and n are the lengths of the input strings.
 */
package de.nogaemer.springhomepage.main.search

/**
 * Computes Levenshtein distance between strings for fuzzy matching.
 */
object LevenshteinDistance {
    /**
     * Calculates the edit distance between two strings.
     *
     * Both strings are converted to lowercase before comparison for case-insensitive matching.
     * The algorithm uses dynamic programming with a 2D matrix where dp[i][j] represents
     * the minimum edits needed to transform a[0..i-1] into b[0..j-1].
     *
     * @param aRaw First string to compare
     * @param bRaw Second string to compare
     * @return The minimum number of edit operations needed (0 means identical strings)
     */
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
