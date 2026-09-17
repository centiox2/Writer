package com.example.rhyme

/**
 * Normalizes input words for phonetic and rhyme analysis.
 * Handles:
 * - Capitalization (case-insensitivity)
 * - Leading and trailing punctuation
 * - Contractions (e.g. don't -> dont / don't)
 * - Hyphenated words
 * - Whitespace trimming
 * - Extraction of words from cursor/selection positions in lyrics
 */
object WordNormalizer {

  // Common English contractions mapped to spoken base/expanded forms or standardized keys
  private val CONTRACTION_EXPANSIONS = mapOf(
    "can't" to "cant",
    "won't" to "wont",
    "don't" to "dont",
    "ain't" to "aint",
    "i'm" to "im",
    "you're" to "youre",
    "he's" to "hes",
    "she's" to "shes",
    "it's" to "its",
    "we're" to "were",
    "they're" to "theyre",
    "i've" to "ive",
    "you've" to "youve",
    "we've" to "weve",
    "they've" to "theyve",
    "i'll" to "ill",
    "you'll" to "youll",
    "he'll" to "hell",
    "she'll" to "shell",
    "we'll" to "well",
    "they'll" to "theyll",
    "i'd" to "id",
    "you'd" to "youd",
    "he'd" to "hed",
    "she'd" to "shed",
    "we'd" to "wed",
    "they'd" to "theyd",
    "couldn't" to "couldnt",
    "shouldn't" to "shouldnt",
    "wouldn't" to "wouldnt",
    "hasn't" to "hasnt",
    "haven't" to "havent",
    "hadn't" to "hadnt",
    "isn't" to "isnt",
    "aren't" to "arent",
    "wasn't" to "wasnt",
    "weren't" to "werent",
    "let's" to "lets",
    "that's" to "thats",
    "who's" to "whos",
    "what's" to "whats",
    "where's" to "wheres",
    "there's" to "theres",
    "here's" to "heres"
  )

  /**
   * Cleans and normalizes a word string for dictionary lookup.
   * Strips all outer punctuation, normalizes curly apostrophes, and lowers case.
   */
  fun normalize(rawInput: String): String {
    if (rawInput.isBlank()) return ""
    // Normalize quotes/apostrophes
    val uncurled = rawInput.trim()
      .replace('’', '\'')
      .replace('‘', '\'')
      .replace('“', '"')
      .replace('”', '"')

    // Remove leading/trailing non-alphanumeric chars, except internal apostrophe or hyphen
    var start = 0
    while (start < uncurled.length && !uncurled[start].isLetterOrDigit()) {
      start++
    }
    var end = uncurled.length
    while (end > start && !uncurled[end - 1].isLetterOrDigit()) {
      end--
    }

    if (start >= end) return ""
    val trimmed = uncurled.substring(start, end).lowercase()

    // Handle contractions: e.g. "don't" -> "dont"
    CONTRACTION_EXPANSIONS[trimmed]?.let { return it }

    return trimmed
  }

  /**
   * Returns alternative lookup variants for contractions and compounds,
   * e.g. "don't" -> ["don't", "dont"], "cant" -> ["cant", "can't"].
   */
  fun getLookupVariants(word: String): List<String> {
    val norm = normalize(word)
    if (norm.isEmpty()) return emptyList()

    val variants = mutableListOf(norm)

    // Check contraction map
    CONTRACTION_EXPANSIONS[norm]?.let { expanded ->
      if (!variants.contains(expanded)) variants.add(expanded)
    }

    // If word contains apostrophe, add version without apostrophe
    if (norm.contains('\'')) {
      val stripped = norm.replace("'", "")
      if (stripped.isNotEmpty() && !variants.contains(stripped)) {
        variants.add(stripped)
      }
    } else {
      // Try adding standard apostrophe if it matches known contractions reverse
      CONTRACTION_EXPANSIONS.entries.find { it.value == norm }?.let {
        if (!variants.contains(it.key)) variants.add(it.key)
      }
    }

    // Hyphenated compound words: e.g. "heart-broken" -> ["heart-broken", "broken"]
    if (norm.contains('-')) {
      val parts = norm.split('-').filter { it.isNotBlank() }
      if (parts.isNotEmpty()) {
        val lastPart = parts.last()
        if (!variants.contains(lastPart)) variants.add(lastPart)
      }
    }

    return variants
  }

  /**
   * Extracts the word under or preceding the cursor in a text body.
   * If a range is highlighted (selection), extracts the selected text.
   * If cursor is at an index, scans left and right to find word boundaries.
   */
  fun extractWordAtCursor(text: String, selectionStart: Int, selectionEnd: Int): String {
    if (text.isEmpty()) return ""

    // If range selected, extract selected text
    if (selectionStart != selectionEnd) {
      val min = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
      val max = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
      val selected = text.substring(min, max)
      return normalize(selected)
    }

    // Cursor position
    val cursor = selectionStart.coerceIn(0, text.length)
    if (cursor == 0 && text.isEmpty()) return ""

    // If cursor is at the end of a word or space, walk backward to find the nearest word
    var probe = cursor
    if (probe > 0 && (probe == text.length || !text[probe].isLetterOrDigit())) {
      probe--
    }

    // If probe is on whitespace or punctuation, walk backward to find last word character
    while (probe > 0 && !text[probe].isLetterOrDigit() && text[probe] != '\'' && text[probe] != '-') {
      probe--
    }

    if (!text[probe].isLetterOrDigit() && text[probe] != '\'' && text[probe] != '-') {
      return ""
    }

    // Find start of word
    var start = probe
    while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '\'' || text[start - 1] == '-')) {
      start--
    }

    // Find end of word
    var end = probe
    while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '\'' || text[end] == '-')) {
      end++
    }

    val extracted = text.substring(start, end)
    return normalize(extracted)
  }

  /**
   * Fast syllable counter for English words (used for multi-syllable analysis and meter/rhythm matching).
   */
  fun estimateSyllables(word: String): Int {
    val clean = normalize(word).replace("'", "")
    if (clean.length <= 3) return 1

    var count = 0
    var prevIsVowel = false
    val vowels = "aeiouy"

    for (i in clean.indices) {
      val isVowel = vowels.contains(clean[i])
      if (isVowel && !prevIsVowel) {
        count++
      }
      prevIsVowel = isVowel
    }

    // Handle silent 'e' at end (e.g. "make", "late", "game"), unless ending in "le" preceded by consonant
    if (clean.endsWith("e") && !clean.endsWith("le") && count > 1) {
      count--
    }

    // Handle common endings: "ed" is often silent unless preceded by t or d
    if (clean.endsWith("ed") && count > 1) {
      val beforeEd = clean.substring(0, clean.length - 2)
      if (!beforeEd.endsWith("t") && !beforeEd.endsWith("d")) {
        count--
      }
    }

    return count.coerceAtLeast(1)
  }
}
