package com.example.rhyme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-level result container from the RhymeEngine.
 */
data class RhymeQueryResult(
  val queryWord: String,
  val normalizedWord: String,
  val pronunciationFound: Boolean,
  val syllableCount: Int,
  val perfectRhymes: List<RhymeCandidate>,
  val nearRhymes: List<RhymeCandidate>,
  val allRanked: List<RhymeCandidate>
)

/**
 * Comprehensive offline rhyme engine supporting:
 * - Direct manual query of any word (e.g. typing into assistant search)
 * - Selection/cursor extraction from song lyrics
 * - Perfect rhymes (full phonological coda match)
 * - Near rhymes (assonance / slant rhyme with natural consonant classes)
 * - Multi-syllable rhymes (feminine / dactylic rhymes like desire / inspire, melody / destiny)
 * - Robust fallback using orthographic suffix and spelling patterns if word is absent from dictionary
 * - Works 100% offline with zero internet dependency
 */
class RhymeEngine(
  private val phoneticDictionary: PhoneticDictionary = PhoneticDictionary()
) {

  /**
   * Finds rhymes for a given word or phrase.
   */
  suspend fun findRhymes(rawWord: String): RhymeQueryResult = withContext(Dispatchers.Default) {
    val norm = WordNormalizer.normalize(rawWord)
    if (norm.isEmpty()) {
      return@withContext RhymeQueryResult(
        queryWord = rawWord,
        normalizedWord = "",
        pronunciationFound = false,
        syllableCount = 0,
        perfectRhymes = emptyList(),
        nearRhymes = emptyList(),
        allRanked = emptyList()
      )
    }

    val pronunciation = phoneticDictionary.getPronunciation(norm)

    if (pronunciation != null) {
      // Word found in phonetic dictionary
      val targetSyllables = pronunciation.syllableCount
      val perfectCandidates = mutableListOf<RhymeCandidate>()
      val nearCandidates = mutableListOf<RhymeCandidate>()

      // 1. Perfect Rhymes (exact match on rhymeKey = nucleus + coda)
      val exactMatches = phoneticDictionary.getExactRhymes(pronunciation.rhymeKey)
      for (p in exactMatches) {
        if (p.word != norm) {
          perfectCandidates.add(
            RhymeCandidate(
              word = p.word,
              type = RhymeType.PERFECT,
              score = 1.0f,
              syllableCount = p.syllableCount,
              phoneticKey = p.rhymeKey,
              explanation = "Perfect match (${p.syllableCount} syl)"
            )
          )
        }
      }

      // 2. Near Rhymes (Assonance & Slant rhymes: shared vowel phone with compatible codas)
      val sameVowelWords = phoneticDictionary.getWordsByVowel(pronunciation.vowelPhone)
      val exactWordsSet = perfectCandidates.map { it.word }.toSet()

      for (p in sameVowelWords) {
        if (p.word != norm && !exactWordsSet.contains(p.word)) {
          val codaScore = RhymeRanker.calculateCodaSimilarity(
            pronunciation.codaConsonants,
            p.codaConsonants
          )
          nearCandidates.add(
            RhymeCandidate(
              word = p.word,
              type = RhymeType.NEAR,
              score = codaScore,
              syllableCount = p.syllableCount,
              phoneticKey = "${p.vowelPhone}-${p.codaConsonants.joinToString("")}",
              explanation = "Near rhyme (${p.syllableCount} syl)"
            )
          )
        }
      }

      val rankedPerfect = RhymeRanker.rank(norm, perfectCandidates, targetSyllables)
      val rankedNear = RhymeRanker.rank(norm, nearCandidates, targetSyllables)
      val allRanked = RhymeRanker.rank(norm, perfectCandidates + nearCandidates, targetSyllables)

      RhymeQueryResult(
        queryWord = rawWord,
        normalizedWord = norm,
        pronunciationFound = true,
        syllableCount = targetSyllables,
        perfectRhymes = rankedPerfect,
        nearRhymes = rankedNear,
        allRanked = allRanked
      )
    } else {
      // 3. Fallback: Word is missing from phonetic dictionary.
      // Use orthographic / suffix heuristic matching.
      val fallbackResult = generateSuffixFallbackRhymes(norm)
      fallbackResult
    }
  }

  /**
   * Generates orthographic and suffix-based rhyme candidates when word is absent from dictionary.
   */
  private fun generateSuffixFallbackRhymes(normWord: String): RhymeQueryResult {
    val estimatedSyllables = WordNormalizer.estimateSyllables(normWord)
    val allKnownWords = phoneticDictionary.getAllPronunciations()
    val suffixCandidates = mutableListOf<RhymeCandidate>()

    // Determine candidate suffixes to match (longest matching suffix first)
    val suffixesToTry = listOf(
      normWord.takeLast(4),
      normWord.takeLast(3),
      normWord.takeLast(2)
    ).filter { it.length >= 2 }

    for (p in allKnownWords) {
      if (p.word == normWord) continue

      for (suffix in suffixesToTry) {
        if (p.word.endsWith(suffix)) {
          val score = (suffix.length.toFloat() / normWord.length.coerceAtLeast(3)).coerceIn(0.4f, 0.85f)
          suffixCandidates.add(
            RhymeCandidate(
              word = p.word,
              type = RhymeType.SUFFIX,
              score = score,
              syllableCount = p.syllableCount,
              phoneticKey = "-$suffix",
              explanation = "Suffix match (-$suffix)"
            )
          )
          break
        }
      }
    }

    // Split suffix candidates into high-affinity and near-affinity
    val rankedSuffix = RhymeRanker.rank(normWord, suffixCandidates, estimatedSyllables)
    val topHalf = rankedSuffix.take(rankedSuffix.size / 2)
    val bottomHalf = rankedSuffix.drop(rankedSuffix.size / 2)

    return RhymeQueryResult(
      queryWord = normWord,
      normalizedWord = normWord,
      pronunciationFound = false,
      syllableCount = estimatedSyllables,
      perfectRhymes = topHalf,
      nearRhymes = bottomHalf,
      allRanked = rankedSuffix
    )
  }
}
