package com.example

import com.example.rhyme.PhoneticDictionary
import com.example.rhyme.RhymeCandidate
import com.example.rhyme.RhymeEngine
import com.example.rhyme.RhymeRanker
import com.example.rhyme.RhymeType
import com.example.rhyme.WordNormalizer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RhymeEngineTest {

  private lateinit var phoneticDictionary: PhoneticDictionary
  private lateinit var rhymeEngine: RhymeEngine

  @Before
  fun setUp() {
    phoneticDictionary = PhoneticDictionary()
    rhymeEngine = RhymeEngine(phoneticDictionary)
  }

  // --- 1. WordNormalizer Tests ---

  @Test
  fun testWordNormalizer_cleaningAndContractions() {
    // Basic punctuation removal and lowercasing
    assertEquals("night", WordNormalizer.normalize("Night!"))
    assertEquals("dream", WordNormalizer.normalize("...dream???"))

    // Contractions expansion / cleaning
    assertEquals("dont", WordNormalizer.normalize("don't"))
    assertEquals("aint", WordNormalizer.normalize("ain't"))
    assertEquals("cant", WordNormalizer.normalize("can't"))
    assertEquals("youre", WordNormalizer.normalize("you're"))

    // Empty and whitespace strings
    assertEquals("", WordNormalizer.normalize(""))
    assertEquals("", WordNormalizer.normalize("   "))
    assertEquals("", WordNormalizer.normalize("--- ,,, !!!"))
  }

  @Test
  fun testWordNormalizer_extractWordAtCursor() {
    val lyrics = "Walking in the cold rain\nWatching stars shine"

    // Cursor on selected range "rain"
    val selRain = WordNormalizer.extractWordAtCursor(lyrics, 20, 24)
    assertEquals("rain", selRain)

    // Single cursor in middle of "stars" (pos 36)
    val starPos = lyrics.indexOf("stars") + 2
    val cursorStar = WordNormalizer.extractWordAtCursor(lyrics, starPos, starPos)
    assertEquals("stars", cursorStar)

    // Cursor at word boundary / punctuation
    val lyricWithPunctuation = "Lost in the night, finding the light."
    val nightPos = lyricWithPunctuation.indexOf("night") + 2
    val cursorNight = WordNormalizer.extractWordAtCursor(lyricWithPunctuation, nightPos, nightPos)
    assertEquals("night", cursorNight)
  }

  @Test
  fun testWordNormalizer_syllableEstimation() {
    // Monosyllabic words
    assertEquals(1, WordNormalizer.estimateSyllables("night"))
    assertEquals(1, WordNormalizer.estimateSyllables("rain"))
    assertEquals(1, WordNormalizer.estimateSyllables("cold"))

    // Polysyllabic words
    assertEquals(2, WordNormalizer.estimateSyllables("music"))
    assertEquals(3, WordNormalizer.estimateSyllables("forever"))
  }

  // --- 2. PhoneticDictionary Tests ---

  @Test
  fun testPhoneticDictionary_lookupAndPronunciation() {
    val nightPron = phoneticDictionary.getPronunciation("night")
    assertNotNull("night should be in dictionary", nightPron)
    assertEquals("night", nightPron?.word)
    assertEquals("AY-T", nightPron?.rhymeKey)
    assertEquals("AY", nightPron?.vowelPhone)
    assertEquals(1, nightPron?.syllableCount)

    // Multi-syllable word
    val desirePron = phoneticDictionary.getPronunciation("desire")
    assertNotNull("desire should be in dictionary", desirePron)
    assertEquals(3, desirePron?.syllableCount)
    assertEquals("AY-ER", desirePron?.rhymeKey)

    // Capitalized lookup should work via normalization
    val brightPron = phoneticDictionary.getPronunciation("Bright")
    assertNotNull(brightPron)
    assertEquals("AY-T", brightPron?.rhymeKey)
  }

  // --- 3. RhymeRanker Tests ---

  @Test
  fun testRhymeRanker_codaSimilarity() {
    // Exact codas
    val exactScore = RhymeRanker.calculateCodaSimilarity(listOf("T"), listOf("T"))
    assertEquals(1.0f, exactScore, 0.001f)

    // Same natural class (Plosives: T and D)
    val plosiveScore = RhymeRanker.calculateCodaSimilarity(listOf("T"), listOf("D"))
    assertTrue("Plosives T and D should have high slant similarity", plosiveScore >= 0.70f)

    // Same natural class (Nasals: M and N)
    val nasalScore = RhymeRanker.calculateCodaSimilarity(listOf("M"), listOf("N"))
    assertTrue("Nasals M and N should have high slant similarity", nasalScore >= 0.70f)
  }

  @Test
  fun testRhymeRanker_ranksPerfectAboveNear() {
    val candidates = listOf(
      RhymeCandidate("shine", RhymeType.NEAR, 0.7f, 1),
      RhymeCandidate("light", RhymeType.PERFECT, 1.0f, 1),
      RhymeCandidate("flight", RhymeType.PERFECT, 1.0f, 1)
    )

    val ranked = RhymeRanker.rank("night", candidates, targetSyllables = 1)
    assertEquals("light", ranked[0].word)
    assertEquals(RhymeType.PERFECT, ranked[0].type)
    assertEquals(RhymeType.PERFECT, ranked[1].type)
    assertEquals("shine", ranked[2].word)
    assertEquals(RhymeType.NEAR, ranked[2].type)
  }

  // --- 4. RhymeEngine Full Workflow Tests ---

  @Test
  fun testRhymeEngine_perfectRhymesForNight() = runTest {
    val result = rhymeEngine.findRhymes("night")

    assertTrue(result.pronunciationFound)
    assertEquals(1, result.syllableCount)
    assertTrue("Should have perfect rhymes", result.perfectRhymes.isNotEmpty())

    val perfectWords = result.perfectRhymes.map { it.word }
    assertTrue("light should rhyme with night", perfectWords.contains("light"))
    assertTrue("bright should rhyme with night", perfectWords.contains("bright"))
    assertTrue("fight should rhyme with night", perfectWords.contains("fight"))
    assertFalse("Target word itself should be excluded", perfectWords.contains("night"))

    // Near rhymes should also be populated (assonance on AY)
    assertTrue("Should have near rhymes with same vowel AY", result.nearRhymes.isNotEmpty())
    val nearWords = result.nearRhymes.map { it.word }
    assertTrue("shine or mind should be in near rhymes", nearWords.contains("shine") || nearWords.contains("mind"))
  }

  @Test
  fun testRhymeEngine_multiSyllableRhyme() = runTest {
    val result = rhymeEngine.findRhymes("fire")

    assertTrue(result.pronunciationFound)
    val perfectWords = result.perfectRhymes.map { it.word }
    assertTrue("desire should rhyme with fire", perfectWords.contains("desire") || perfectWords.contains("wire"))
  }

  @Test
  fun testRhymeEngine_offlineSuffixFallback() = runTest {
    // Word deliberately not in standard dictionary
    val unknownWord = "glimplight"
    val result = rhymeEngine.findRhymes(unknownWord)

    // Verify fallback activated
    assertFalse("Unknown word should not have phonetic pronunciation", result.pronunciationFound)
    assertTrue("Fallback rhymes should be generated from suffix", result.allRanked.isNotEmpty())

    // All fallback items should match the -ight suffix
    val matches = result.allRanked.filter { it.word.endsWith("ight") }
    assertTrue("Suffix fallback should match words ending in ight", matches.isNotEmpty())
  }

  @Test
  fun testRhymeEngine_punctuationAndContractionInput() = runTest {
    val result = rhymeEngine.findRhymes("  \"NIGHT!\"  ")
    assertTrue(result.pronunciationFound)
    assertEquals("night", result.normalizedWord)
    assertTrue(result.perfectRhymes.any { it.word == "light" })
  }
}
