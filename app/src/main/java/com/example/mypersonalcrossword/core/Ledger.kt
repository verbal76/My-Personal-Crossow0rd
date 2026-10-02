package com.hag.mypersonalcrossword.core

// ── PAYOUT LEDGER ─────────────────────────────────────────────────────────────

/**
 * Stable identity of a board: the same answers in the same places and directions,
 * whatever order the list is in. The completion payout records it, so a restored
 * copy of a puzzle that was already paid for (for example a saved state taken a
 * frame before the payout) can't pay out a second time.
 */
fun puzzleFingerprint(words: List<PlacedWord>): String {
    val canonical = words
        .map { "${it.word}:${it.startX}:${it.startY}:${if (it.isHorizontal) 'A' else 'D'}" }
        .sorted()
        .joinToString(";")
    var h = -0x340d631b7bdddcdbL          // FNV-1a 64 offset basis
    for (b in canonical.toByteArray(Charsets.UTF_8)) {
        h = h xor (b.toLong() and 0xFF)
        h *= 0x100000001b3L                // FNV-1a 64 prime
    }
    return java.lang.Long.toHexString(h)
}

/** How many paid boards each profile remembers. Older entries can't be restored anyway. */
const val PAID_LEDGER_SIZE = 100

/** Adds [fingerprint] to a comma-separated ledger, newest last, keeping the last [cap]. */
fun appendToLedger(ledger: String, fingerprint: String, cap: Int = PAID_LEDGER_SIZE): String =
    (ledger.split(',').filter { it.isNotBlank() && it != fingerprint } + fingerprint)
        .takeLast(cap)
        .joinToString(",")

fun ledgerContains(ledger: String, fingerprint: String): Boolean =
    ledger.split(',').any { it == fingerprint }
