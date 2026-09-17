package com.example.rhyme

/**
 * Rhyme classification types.
 */
enum class RhymeType {
  PERFECT, // Exact phoneme match from stressed vowel through word ending (e.g. night / bright, fire / desire)
  NEAR,    // Assonance (same vowel nucleus) or slant rhyme (consonant coda similarity)
  SUFFIX   // Orthographic / suffix fallback for words missing from phonetic dictionary
}

/**
 * Represents a ranked rhyme candidate with metadata for display in the songwriter studio.
 */
data class RhymeCandidate(
  val word: String,
  val type: RhymeType,
  val score: Float, // 0.0 to 1.0 ranking score
  val syllableCount: Int,
  val phoneticKey: String = "",
  val explanation: String = ""
)

/**
 * Ranks and sorts rhyme candidates based on:
 * 1. Rhyme type (PERFECT > NEAR > SUFFIX)
 * 2. Phonological distance (consonant sonority, place/manner of articulation)
 * 3. Syllable count alignment to target word
 * 4. Word popularity / common songwriting vocabulary weighting
 */
object RhymeRanker {

  // Phonetic consonant natural classes for slant/near-rhyme similarity scoring
  private val PLOSIVES = setOf("P", "B", "T", "D", "K", "G")
  private val FRICATIVES = setOf("F", "V", "TH", "DH", "S", "Z", "SH", "ZH", "HH")
  private val NASALS = setOf("M", "N", "NG")
  private val LIQUIDS_GLIDES = setOf("L", "R", "W", "Y")

  // Common songwriting high-utility words boosted in rank
  private val LYRIC_FAVORITES = setOf(
    "night", "light", "fire", "desire", "heart", "start", "time", "rhyme", "shine", "mind",
    "life", "fly", "sky", "high", "cry", "die", "dream", "see", "free", "deep", "keep",
    "day", "say", "stay", "away", "play", "rain", "pain", "space", "place", "grace",
    "dark", "spark", "star", "soul", "control", "whole", "flow", "glow", "slow", "grow",
    "cold", "hold", "gold", "you", "true", "blue", "through", "burn", "turn", "learn",
    "shout", "out", "loud", "proud", "more", "door", "floor", "storm", "born", "head",
    "dead", "red", "best", "rest", "stand", "hand", "love", "above", "sun", "run"
  )

  /**
   * Evaluates and scores candidates against a query target pronunciation.
   */
  fun rank(
    targetWord: String,
    candidates: List<RhymeCandidate>,
    targetSyllables: Int = 1
  ): List<RhymeCandidate> {
    val normTarget = WordNormalizer.normalize(targetWord)

    return candidates
      .filter { it.word != normTarget } // Exclude the query word itself
      .distinctBy { it.word }
      .map { candidate ->
        val adjustedScore = calculateFinalScore(candidate, targetSyllables)
        candidate.copy(score = adjustedScore)
      }
      .sortedWith(
        compareByDescending<RhymeCandidate> { it.type == RhymeType.PERFECT }
          .thenByDescending { it.score }
          .thenBy { Math.abs(it.syllableCount - targetSyllables) }
          .thenBy { it.word }
      )
  }

  private fun calculateFinalScore(candidate: RhymeCandidate, targetSyllables: Int): Float {
    var baseScore = when (candidate.type) {
      RhymeType.PERFECT -> 0.90f
      RhymeType.NEAR -> 0.65f
      RhymeType.SUFFIX -> 0.40f
    }

    // Syllable meter closeness bonus: poetic rhythm often matches syllable counts
    val syllableDiff = Math.abs(candidate.syllableCount - targetSyllables)
    val syllableMultiplier = when (syllableDiff) {
      0 -> 1.10f
      1 -> 1.00f
      2 -> 0.92f
      else -> 0.85f
    }

    // Boost high-frequency poetic songwriting terms
    val lyricBoost = if (LYRIC_FAVORITES.contains(candidate.word)) 1.08f else 1.0f

    return (baseScore * syllableMultiplier * lyricBoost * candidate.score).coerceIn(0.1f, 1.0f)
  }

  /**
   * Calculates similarity between two consonant coda sequences for slant / near rhymes.
   * Returns a score between 0.2 and 0.95.
   */
  fun calculateCodaSimilarity(targetCoda: List<String>, candidateCoda: List<String>): Float {
    if (targetCoda == candidateCoda) return 1.0f
    if (targetCoda.isEmpty() && candidateCoda.isEmpty()) return 1.0f
    if (targetCoda.isEmpty() || candidateCoda.isEmpty()) return 0.35f

    // Compare final consonant sounds
    val tLast = targetCoda.last()
    val cLast = candidateCoda.last()

    if (tLast == cLast) return 0.85f

    // Natural class sharing (e.g. nasal to nasal: M and N; plosive to plosive: T and D)
    val sameClass = (PLOSIVES.contains(tLast) && PLOSIVES.contains(cLast)) ||
      (FRICATIVES.contains(tLast) && FRICATIVES.contains(cLast)) ||
      (NASALS.contains(tLast) && NASALS.contains(cLast)) ||
      (LIQUIDS_GLIDES.contains(tLast) && LIQUIDS_GLIDES.contains(cLast))

    return if (sameClass) 0.70f else 0.45f
  }
}
