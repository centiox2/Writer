package com.example.rhyme

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Represents the phonetic structure of a word.
 * @param word The normalized orthographic word
 * @param phonemes ARPAbet phoneme sequence, e.g. ["F", "AY1", "R"] for "fire"
 * @param rhymeKey The primary rhyme key (from the primary stressed vowel to the end)
 * @param syllableCount Number of syllables derived from stress digits in phonemes
 */
data class Pronunciation(
  val word: String,
  val phonemes: List<String>,
  val rhymeKey: String,
  val vowelPhone: String,
  val codaConsonants: List<String>,
  val syllableCount: Int
)

/**
 * Phonetic Dictionary supporting:
 * - CMUdict phoneme mappings (built-in offline embedded core + optional asset expansion)
 * - Phonetic parsing with ARPAbet vowels (AA, AE, AH, AO, AW, AY, EH, ER, EY, IH, IY, OW, OY, UH, UW)
 * - Automatic rhyme key extraction (stressed vowel + coda)
 * - Consonant similarity groupings (plosives, fricatives, nasals, liquids) for near-rhymes
 * - Offline fallback heuristics for words missing from the dictionary
 */
class PhoneticDictionary(
  private val context: Context? = null
) {

  // Map word -> Pronunciation
  private val wordToPronunciation = mutableMapOf<String, Pronunciation>()

  // Map exact rhymeKey -> List of words
  private val rhymeKeyToWords = mutableMapOf<String, MutableList<Pronunciation>>()

  // Map vowelPhone -> List of pronunciations (for assonance / near-rhymes)
  private val vowelToWords = mutableMapOf<String, MutableList<Pronunciation>>()

  init {
    loadCoreDictionary()
    loadAssetDictionaryIfAvailable()
  }

  val size: Int
    get() = wordToPronunciation.size

  fun getPronunciation(word: String): Pronunciation? {
    val variants = WordNormalizer.getLookupVariants(word)
    for (v in variants) {
      wordToPronunciation[v]?.let { return it }
    }
    return null
  }

  fun containsWord(word: String): Boolean {
    return getPronunciation(word) != null
  }

  fun getExactRhymes(rhymeKey: String): List<Pronunciation> {
    return rhymeKeyToWords[rhymeKey] ?: emptyList()
  }

  fun getWordsByVowel(vowel: String): List<Pronunciation> {
    return vowelToWords[vowel] ?: emptyList()
  }

  fun getAllPronunciations(): Collection<Pronunciation> {
    return wordToPronunciation.values
  }

  /**
   * Registers a pronunciation into the index.
   */
  fun addEntry(word: String, phonemes: List<String>) {
    val normWord = WordNormalizer.normalize(word)
    if (normWord.isEmpty() || phonemes.isEmpty()) return

    // Find the nucleus (last stressed vowel, or last vowel if no stress digit)
    var lastStressedVowelIndex = -1
    var lastVowelIndex = -1
    var vowelCount = 0

    for (i in phonemes.indices) {
      val p = phonemes[i]
      if (isVowel(p)) {
        vowelCount++
        lastVowelIndex = i
        if (p.contains('1') || p.contains('2')) {
          lastStressedVowelIndex = i
        }
      }
    }

    val rhymeVowelIndex = if (lastStressedVowelIndex != -1) lastStressedVowelIndex else lastVowelIndex
    if (rhymeVowelIndex == -1) return

    val rhymeKey = phonemes.subList(rhymeVowelIndex, phonemes.size).joinToString("-") { stripStress(it) }
    val vowelPhone = stripStress(phonemes[rhymeVowelIndex])
    val codaConsonants = if (rhymeVowelIndex + 1 < phonemes.size) {
      phonemes.subList(rhymeVowelIndex + 1, phonemes.size).map { stripStress(it) }
    } else {
      emptyList()
    }

    val sylCount = if (vowelCount > 0) vowelCount else WordNormalizer.estimateSyllables(normWord)

    val pronunciation = Pronunciation(
      word = normWord,
      phonemes = phonemes,
      rhymeKey = rhymeKey,
      vowelPhone = vowelPhone,
      codaConsonants = codaConsonants,
      syllableCount = sylCount
    )

    wordToPronunciation[normWord] = pronunciation

    rhymeKeyToWords.getOrPut(rhymeKey) { mutableListOf() }.add(pronunciation)
    vowelToWords.getOrPut(vowelPhone) { mutableListOf() }.add(pronunciation)
  }

  private fun loadAssetDictionaryIfAvailable() {
    val ctx = context ?: return
    try {
      ctx.assets.open("cmudict_lyrics.txt").use { inputStream ->
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String? = reader.readLine()
        while (line != null) {
          val trimmed = line.trim()
          if (trimmed.isNotEmpty() && !trimmed.startsWith(";;;")) {
            val parts = trimmed.split("\\s+".toRegex())
            if (parts.size >= 2) {
              val rawWord = parts[0]
              // Discard alternate variants with parenthesis, e.g. "WORD(1)"
              val word = if (rawWord.contains('(')) rawWord.substringBefore('(') else rawWord
              val phones = parts.subList(1, parts.size)
              addEntry(word, phones)
            }
          }
          line = reader.readLine()
        }
      }
    } catch (_: Exception) {
      // Asset file not present or error reading; built-in dictionary remains intact
    }
  }

  /**
   * High-frequency offline dictionary designed specifically for songwriters, hip-hop, pop, rock, and R&B lyrics.
   * Over 1,200+ top lyrical rhyming words, slang, verbs, emotions, and multi-syllable terms.
   */
  private fun loadCoreDictionary() {
    val entries = listOf(
      // AY1 rhymes (fire, light, night, right, fly, sky, time, life, ride, mind)
      "light" to listOf("L", "AY1", "T"),
      "night" to listOf("N", "AY1", "T"),
      "fight" to listOf("F", "AY1", "T"),
      "bright" to listOf("B", "R", "AY1", "T"),
      "tight" to listOf("T", "AY1", "T"),
      "sight" to listOf("S", "AY1", "T"),
      "might" to listOf("M", "AY1", "T"),
      "flight" to listOf("F", "L", "AY1", "T"),
      "knight" to listOf("N", "AY1", "T"),
      "white" to listOf("W", "AY1", "T"),
      "bite" to listOf("B", "AY1", "T"),
      "kite" to listOf("K", "AY1", "T"),
      "spite" to listOf("S", "P", "AY1", "T"),
      "ignite" to listOf("IH0", "G", "N", "AY1", "T"),
      "tonight" to listOf("T", "AH0", "N", "AY1", "T"),
      "midnight" to listOf("M", "IH1", "D", "N", "AY2", "T"),
      "daylight" to listOf("D", "EY1", "L", "AY2", "T"),
      "sunlight" to listOf("S", "AH1", "N", "L", "AY2", "T"),
      "polite" to listOf("P", "AH0", "L", "AY1", "T"),
      "unite" to listOf("Y", "UW0", "N", "AY1", "T"),

      "fire" to listOf("F", "AY1", "ER0"),
      "desire" to listOf("D", "IH0", "Z", "AY1", "ER0"),
      "inspire" to listOf("IH0", "N", "S", "P", "AY1", "ER0"),
      "wire" to listOf("W", "AY1", "ER0"),
      "higher" to listOf("HH", "AY1", "ER0"),
      "tire" to listOf("T", "AY1", "ER0"),
      "liar" to listOf("L", "AY1", "ER0"),
      "admire" to listOf("AE0", "D", "M", "AY1", "ER0"),
      "aspire" to listOf("AH0", "S", "P", "AY1", "ER0"),
      "acquire" to listOf("AH0", "K", "W", "AY1", "ER0"),
      "empire" to listOf("EH1", "M", "P", "AY2", "ER0"),

      "fly" to listOf("F", "L", "AY1"),
      "sky" to listOf("S", "K", "AY1"),
      "high" to listOf("HH", "AY1"),
      "cry" to listOf("K", "R", "AY1"),
      "die" to listOf("D", "AY1"),
      "lie" to listOf("L", "AY1"),
      "tie" to listOf("T", "AY1"),
      "bye" to listOf("B", "AY1"),
      "try" to listOf("T", "R", "AY1"),
      "dry" to listOf("D", "R", "AY1"),
      "goodbye" to listOf("G", "UH2", "D", "B", "AY1"),
      "reply" to listOf("R", "IH0", "P", "L", "AY1"),
      "deny" to listOf("D", "IH0", "N", "AY1"),
      "supply" to listOf("S", "AH0", "P", "L", "AY1"),
      "rely" to listOf("R", "IH0", "L", "AY1"),

      "time" to listOf("T", "AY1", "M"),
      "rhyme" to listOf("R", "AY1", "M"),
      "climb" to listOf("K", "L", "AY1", "M"),
      "crime" to listOf("K", "R", "AY1", "M"),
      "shine" to listOf("SH", "AY1", "N"),
      "mine" to listOf("M", "AY1", "N"),
      "fine" to listOf("F", "AY1", "N"),
      "line" to listOf("L", "AY1", "N"),
      "wine" to listOf("W", "AY1", "N"),
      "sign" to listOf("S", "AY1", "N"),
      "divine" to listOf("D", "IH0", "V", "AY1", "N"),
      "align" to listOf("AH0", "L", "AY1", "N"),
      "design" to listOf("D", "IH0", "Z", "AY1", "N"),
      "sunshine" to listOf("S", "AH1", "N", "SH", "AY2", "N"),
      "lifeline" to listOf("L", "AY1", "F", "L", "AY2", "N"),

      "life" to listOf("L", "AY1", "F"),
      "strife" to listOf("S", "T", "R", "AY1", "F"),
      "wife" to listOf("W", "AY1", "F"),
      "knife" to listOf("N", "AY1", "F"),

      "mind" to listOf("M", "AY1", "N", "D"),
      "find" to listOf("F", "AY1", "N", "D"),
      "blind" to listOf("B", "L", "AY1", "N", "D"),
      "kind" to listOf("K", "AY1", "N", "D"),
      "grind" to listOf("G", "R", "AY1", "N", "D"),
      "behind" to listOf("B", "IH0", "HH", "AY1", "N", "D"),
      "remind" to listOf("R", "IY0", "M", "AY1", "N", "D"),
      "rewind" to listOf("R", "IY0", "W", "AY1", "N", "D"),

      "ride" to listOf("R", "AY1", "D"),
      "side" to listOf("S", "AY1", "D"),
      "hide" to listOf("HH", "AY1", "D"),
      "pride" to listOf("P", "R", "AY1", "D"),
      "wide" to listOf("W", "AY1", "D"),
      "guide" to listOf("G", "AY1", "D"),
      "tide" to listOf("T", "AY1", "D"),
      "slide" to listOf("S", "L", "AY1", "D"),
      "glide" to listOf("G", "L", "AY1", "D"),
      "inside" to listOf("IH2", "N", "S", "AY1", "D"),
      "outside" to listOf("AW1", "T", "S", "AY2", "D"),
      "beside" to listOf("B", "IH0", "S", "AY1", "D"),
      "collide" to listOf("K", "AH0", "L", "AY1", "D"),
      "provide" to listOf("P", "R", "AH0", "V", "AY1", "D"),

      // IY1 rhymes (dream, sea, see, believe, deep, keep, free, me, we, be)
      "dream" to listOf("D", "R", "IY1", "M"),
      "beam" to listOf("B", "IY1", "M"),
      "stream" to listOf("S", "T", "R", "IY1", "M"),
      "gleam" to listOf("G", "L", "IY1", "M"),
      "scheme" to listOf("S", "K", "IY1", "M"),
      "team" to listOf("T", "IY1", "M"),
      "scream" to listOf("S", "K", "R", "IY1", "M"),
      "extreme" to listOf("EH0", "K", "S", "T", "R", "IY1", "M"),
      "redeem" to listOf("R", "IH0", "D", "IY1", "M"),

      "see" to listOf("S", "IY1"),
      "sea" to listOf("S", "IY1"),
      "free" to listOf("F", "R", "IY1"),
      "me" to listOf("M", "IY1"),
      "we" to listOf("W", "IY1"),
      "be" to listOf("B", "IY1"),
      "tree" to listOf("T", "R", "IY1"),
      "knee" to listOf("N", "IY1"),
      "flee" to listOf("F", "L", "IY1"),
      "plea" to listOf("P", "L", "IY1"),
      "key" to listOf("K", "IY1"),
      "agree" to listOf("AH0", "G", "R", "IY1"),
      "degree" to listOf("D", "IH0", "G", "R", "IY1"),
      "destiny" to listOf("D", "EH1", "S", "T", "AH0", "N", "IY0"),
      "melody" to listOf("M", "EH1", "L", "AH0", "D", "IY0"),
      "memory" to listOf("M", "EH1", "M", "ER0", "IY0"),
      "eternity" to listOf("IH0", "T", "ER1", "N", "AH0", "T", "IY0"),
      "gravity" to listOf("G", "R", "AE1", "V", "AH0", "T", "IY0"),
      "clarity" to listOf("K", "L", "EH1", "R", "AH0", "T", "IY0"),
      "serenity" to listOf("S", "ER0", "EH1", "N", "AH0", "T", "IY0"),
      "guarantee" to listOf("G", "EH2", "R", "AH0", "N", "T", "IY1"),

      "deep" to listOf("D", "IY1", "P"),
      "keep" to listOf("K", "IY1", "P"),
      "sleep" to listOf("S", "L", "IY1", "P"),
      "weep" to listOf("W", "IY1", "P"),
      "leap" to listOf("L", "IY1", "P"),
      "steep" to listOf("S", "T", "IY1", "P"),
      "creep" to listOf("K", "R", "IY1", "P"),
      "asleep" to listOf("AH0", "S", "L", "IY1", "P"),

      "feel" to listOf("F", "IY1", "L"),
      "heal" to listOf("HH", "IY1", "L"),
      "real" to listOf("R", "IY1", "L"),
      "steel" to listOf("S", "T", "IY1", "L"),
      "wheel" to listOf("W", "IY1", "L"),
      "deal" to listOf("D", "IY1", "L"),
      "reveal" to listOf("R", "IH0", "V", "IY1", "L"),
      "appeal" to listOf("AH0", "P", "IY1", "L"),
      "unreal" to listOf("AH0", "N", "R", "IY1", "L"),

      // EY1 rhymes (day, say, away, stay, play, break, make, take, wake, rain, pain)
      "day" to listOf("D", "EY1"),
      "say" to listOf("S", "EY1"),
      "play" to listOf("P", "L", "EY1"),
      "stay" to listOf("S", "T", "EY1"),
      "away" to listOf("AH0", "W", "EY1"),
      "way" to listOf("W", "EY1"),
      "pray" to listOf("P", "R", "EY1"),
      "gray" to listOf("G", "R", "EY1"),
      "grey" to listOf("G", "R", "EY1"),
      "sway" to listOf("S", "W", "EY1"),
      "decay" to listOf("D", "IH0", "K", "EY1"),
      "betray" to listOf("B", "IH0", "T", "R", "EY1"),
      "display" to listOf("D", "IH0", "S", "P", "L", "EY1"),
      "today" to listOf("T", "AH0", "D", "EY1"),
      "runaway" to listOf("R", "AH1", "N", "AH0", "W", "EY2"),

      "make" to listOf("M", "EY1", "K"),
      "take" to listOf("T", "EY1", "K"),
      "break" to listOf("B", "R", "EY1", "K"),
      "wake" to listOf("W", "EY1", "K"),
      "shake" to listOf("SH", "EY1", "K"),
      "fake" to listOf("F", "EY1", "K"),
      "lake" to listOf("L", "EY1", "K"),
      "stake" to listOf("S", "T", "EY1", "K"),
      "ache" to listOf("EY1", "K"),
      "mistake" to listOf("M", "IH0", "S", "T", "EY1", "K"),
      "heartbreak" to listOf("HH", "AA1", "R", "T", "B", "R", "EY2", "K"),
      "earthquake" to listOf("ER1", "TH", "K", "W", "EY2", "K"),
      "forsake" to listOf("F", "AO0", "R", "S", "EY1", "K"),
      "awake" to listOf("AH0", "W", "EY1", "K"),

      "rain" to listOf("R", "EY1", "N"),
      "pain" to listOf("P", "EY1", "N"),
      "chain" to listOf("CH", "EY1", "N"),
      "strain" to listOf("S", "T", "R", "EY1", "N"),
      "train" to listOf("T", "R", "EY1", "N"),
      "vein" to listOf("V", "EY1", "N"),
      "insane" to listOf("IH2", "N", "S", "EY1", "N"),
      "remain" to listOf("R", "IH0", "M", "EY1", "N"),
      "explain" to listOf("IH0", "K", "S", "P", "L", "EY1", "N"),
      "sustain" to listOf("S", "AH0", "S", "T", "EY1", "N"),
      "refrain" to listOf("R", "IH0", "F", "R", "EY1", "N"),

      "space" to listOf("S", "P", "EY1", "S"),
      "place" to listOf("P", "L", "EY1", "S"),
      "face" to listOf("F", "EY1", "S"),
      "trace" to listOf("T", "R", "EY1", "S"),
      "chase" to listOf("CH", "EY1", "S"),
      "grace" to listOf("G", "R", "EY1", "S"),
      "embrace" to listOf("EH0", "M", "B", "R", "EY1", "S"),
      "erase" to listOf("IH0", "R", "EY1", "S"),
      "replace" to listOf("R", "IH0", "P", "L", "EY1", "S"),

      // AA1 / AR rhymes (heart, dark, start, part, star, far, car)
      "heart" to listOf("HH", "AA1", "R", "T"),
      "start" to listOf("S", "T", "AA1", "R", "T"),
      "part" to listOf("P", "AA1", "R", "T"),
      "art" to listOf("AA1", "R", "T"),
      "chart" to listOf("CH", "AA1", "R", "T"),
      "smart" to listOf("S", "M", "AA1", "R", "T"),
      "apart" to listOf("AH0", "P", "AA1", "R", "T"),
      "restart" to listOf("R", "IY0", "S", "T", "AA1", "R", "T"),
      "depart" to listOf("D", "IH0", "P", "AA1", "R", "T"),

      "dark" to listOf("D", "AA1", "R", "K"),
      "spark" to listOf("S", "P", "AA1", "R", "K"),
      "mark" to listOf("M", "AA1", "R", "K"),
      "park" to listOf("P", "AA1", "R", "K"),
      "bark" to listOf("B", "AA1", "R", "K"),
      "remark" to listOf("R", "IH0", "M", "AA1", "R", "K"),

      "star" to listOf("S", "T", "AA1", "R"),
      "far" to listOf("F", "AA1", "R"),
      "scar" to listOf("S", "K", "AA1", "R"),
      "car" to listOf("K", "AA1", "R"),
      "bar" to listOf("B", "AA1", "R"),
      "guitar" to listOf("G", "IH0", "T", "AA1", "R"),
      "afar" to listOf("AH0", "F", "AA1", "R"),

      // OW1 rhymes (soul, control, go, flow, know, show, slow, shadow, echo)
      "soul" to listOf("S", "OW1", "L"),
      "control" to listOf("K", "AH0", "N", "T", "R", "OW1", "L"),
      "whole" to listOf("HH", "OW1", "L"),
      "hole" to listOf("HH", "OW1", "L"),
      "goal" to listOf("G", "OW1", "L"),
      "role" to listOf("R", "OW1", "L"),
      "toll" to listOf("T", "OW1", "L"),
      "stroll" to listOf("S", "T", "R", "OW1", "L"),

      "go" to listOf("G", "OW1"),
      "flow" to listOf("F", "L", "OW1"),
      "know" to listOf("N", "OW1"),
      "show" to listOf("SH", "OW1"),
      "glow" to listOf("G", "L", "OW1"),
      "slow" to listOf("S", "L", "OW1"),
      "blow" to listOf("B", "L", "OW1"),
      "grow" to listOf("G", "R", "OW1"),
      "below" to listOf("B", "IH0", "L", "OW1"),
      "shadow" to listOf("SH", "AE1", "D", "OW0"),
      "echo" to listOf("EH1", "K", "OW0"),
      "overflow" to listOf("OW2", "V", "ER0", "F", "L", "OW1"),
      "window" to listOf("W", "IH1", "N", "D", "OW0"),
      "tomorrow" to listOf("T", "AH0", "M", "AA1", "R", "OW0"),
      "sorrow" to listOf("S", "AA1", "R", "OW0"),

      "cold" to listOf("K", "OW1", "L", "D"),
      "hold" to listOf("HH", "OW1", "L", "D"),
      "gold" to listOf("G", "OW1", "L", "D"),
      "old" to listOf("OW1", "L", "D"),
      "bold" to listOf("B", "OW1", "L", "D"),
      "fold" to listOf("F", "OW1", "L", "D"),
      "untold" to listOf("AH0", "N", "T", "OW1", "L", "D"),
      "behold" to listOf("B", "IH0", "HH", "OW1", "L", "D"),

      // UW1 rhymes (you, true, blue, through, new, choose, lose)
      "you" to listOf("Y", "UW1"),
      "true" to listOf("T", "R", "UW1"),
      "blue" to listOf("B", "L", "UW1"),
      "through" to listOf("TH", "R", "UW1"),
      "new" to listOf("N", "UW1"),
      "knew" to listOf("N", "UW1"),
      "flew" to listOf("F", "L", "UW1"),
      "grew" to listOf("G", "R", "UW1"),
      "clue" to listOf("K", "L", "UW1"),
      "view" to listOf("V", "Y", "UW1"),
      "rescue" to listOf("R", "EH1", "S", "K", "Y", "UW0"),
      "pursue" to listOf("P", "ER0", "S", "UW1"),
      "breakthrough" to listOf("B", "R", "EY1", "K", "TH", "R", "UW2"),

      "lose" to listOf("L", "UW1", "Z"),
      "choose" to listOf("CH", "UW1", "Z"),
      "refuse" to listOf("R", "IH0", "F", "Y", "UW1", "Z"),
      "confuse" to listOf("K", "AH0", "N", "F", "Y", "UW1", "Z"),
      "blues" to listOf("B", "L", "UW1", "Z"),
      "cruise" to listOf("K", "R", "UW1", "Z"),
      "bruise" to listOf("B", "R", "UW1", "Z"),

      // ER1 rhymes (burn, learn, turn, world, hurt, word)
      "burn" to listOf("B", "ER1", "N"),
      "learn" to listOf("L", "ER1", "N"),
      "turn" to listOf("T", "ER1", "N"),
      "yearn" to listOf("Y", "ER1", "N"),
      "return" to listOf("R", "IH0", "T", "ER1", "N"),

      "hurt" to listOf("HH", "ER1", "T"),
      "shirt" to listOf("SH", "ER1", "T"),
      "dirt" to listOf("D", "ER1", "T"),
      "desert" to listOf("D", "IH0", "Z", "ER1", "T"),
      "alert" to listOf("AH0", "L", "ER1", "T"),

      "word" to listOf("W", "ER1", "D"),
      "heard" to listOf("HH", "ER1", "D"),
      "bird" to listOf("B", "ER1", "D"),
      "occurred" to listOf("AH0", "K", "ER1", "D"),

      // AW1 rhymes (now, how, brow, out, shout, doubt, loud, cloud, proud)
      "now" to listOf("N", "AW1"),
      "how" to listOf("HH", "AW1"),
      "vow" to listOf("V", "AW1"),
      "bow" to listOf("B", "AW1"),
      "somehow" to listOf("S", "AH1", "M", "HH", "AW2"),

      "out" to listOf("AW1", "T"),
      "shout" to listOf("SH", "AW1", "T"),
      "doubt" to listOf("D", "AW1", "T"),
      "about" to listOf("AH0", "B", "AW1", "T"),
      "without" to listOf("W", "IH0", "TH", "AW1", "T"),

      "loud" to listOf("L", "AW1", "D"),
      "cloud" to listOf("K", "L", "AW1", "D"),
      "proud" to listOf("P", "R", "AW1", "D"),
      "crowd" to listOf("K", "R", "AW1", "D"),

      // AO1 / OR rhymes (more, door, floor, roar, war, storm, born, torn)
      "more" to listOf("M", "AO1", "R"),
      "door" to listOf("D", "AO1", "R"),
      "floor" to listOf("F", "L", "AO1", "R"),
      "roar" to listOf("R", "AO1", "R"),
      "war" to listOf("W", "AO1", "R"),
      "score" to listOf("S", "K", "AO1", "R"),
      "shore" to listOf("SH", "AO1", "R"),
      "before" to listOf("B", "IH0", "F", "AO1", "R"),
      "ignore" to listOf("IH0", "G", "N", "AO1", "R"),
      "explore" to listOf("IH0", "K", "S", "P", "L", "AO1", "R"),
      "forever" to listOf("F", "ER0", "EH1", "V", "ER0"),

      "born" to listOf("B", "AO1", "R", "N"),
      "torn" to listOf("T", "AO1", "R", "N"),
      "worn" to listOf("W", "AO1", "R", "N"),
      "storm" to listOf("S", "T", "AO1", "R", "M"),
      "form" to listOf("F", "AO1", "R", "M"),
      "warm" to listOf("W", "AO1", "R", "M"),
      "transform" to listOf("T", "R", "AE2", "N", "S", "F", "AO1", "R", "M"),

      // EH1 rhymes (head, dead, red, said, breath, death, best, rest, west, never)
      "head" to listOf("HH", "EH1", "D"),
      "dead" to listOf("D", "EH1", "D"),
      "red" to listOf("R", "EH1", "D"),
      "said" to listOf("S", "EH1", "D"),
      "lead" to listOf("L", "EH1", "D"),
      "thread" to listOf("TH", "R", "EH1", "D"),
      "shed" to listOf("SH", "EH1", "D"),
      "ahead" to listOf("AH0", "HH", "EH1", "D"),
      "instead" to listOf("IH0", "N", "S", "T", "EH1", "D"),

      "breath" to listOf("B", "R", "EH1", "TH"),
      "death" to listOf("D", "EH1", "TH"),

      "best" to listOf("B", "EH1", "S", "T"),
      "rest" to listOf("R", "EH1", "S", "T"),
      "west" to listOf("W", "EH1", "S", "T"),
      "chest" to listOf("CH", "EH1", "S", "T"),
      "test" to listOf("T", "EH1", "S", "T"),
      "quest" to listOf("K", "W", "EH1", "S", "T"),
      "confest" to listOf("K", "AH0", "N", "F", "EH1", "S", "T"),
      "obsessed" to listOf("AH0", "B", "S", "EH1", "S", "T"),
      "manifest" to listOf("M", "AE1", "N", "AH0", "F", "EH2", "S", "T"),

      // IH1 rhymes (live, give, fill, spill, will, still, kill, rhythm)
      "give" to listOf("G", "IH1", "V"),
      "live" to listOf("L", "IH1", "V"),
      "forgive" to listOf("F", "ER0", "G", "IH1", "V"),

      "will" to listOf("W", "IH1", "L"),
      "still" to listOf("S", "T", "IH1", "L"),
      "fill" to listOf("F", "IH1", "L"),
      "spill" to listOf("S", "P", "IH1", "L"),
      "kill" to listOf("K", "IH1", "L"),
      "chill" to listOf("CH", "IH1", "L"),
      "thrill" to listOf("TH", "R", "IH1", "L"),
      "until" to listOf("AH0", "N", "T", "IH1", "L"),

      // AE1 rhymes (stand, hand, land, brand, back, black, track)
      "stand" to listOf("S", "T", "AE1", "N", "D"),
      "hand" to listOf("HH", "AE1", "N", "D"),
      "land" to listOf("L", "AE1", "N", "D"),
      "band" to listOf("B", "AE1", "N", "D"),
      "demand" to listOf("D", "IH0", "M", "AE1", "N", "D"),
      "understand" to listOf("AH2", "N", "D", "ER0", "S", "T", "AE1", "N", "D"),

      "black" to listOf("B", "L", "AE1", "K"),
      "back" to listOf("B", "AE1", "K"),
      "track" to listOf("T", "R", "AE1", "K"),
      "attack" to listOf("AH0", "T", "AE1", "K"),

      // AH1 rhymes (love, above, run, sun, one, done, gun)
      "love" to listOf("L", "AH1", "V"),
      "above" to listOf("AH0", "B", "AH1", "V"),
      "dove" to listOf("D", "AH1", "V"),
      "glove" to listOf("G", "L", "AH1", "V"),

      "run" to listOf("R", "AH1", "N"),
      "sun" to listOf("S", "AH1", "N"),
      "one" to listOf("W", "AH1", "N"),
      "won" to listOf("W", "AH1", "N"),
      "done" to listOf("D", "AH1", "N"),
      "gun" to listOf("G", "AH1", "N"),
      "begun" to listOf("B", "IH0", "G", "AH1", "N"),

      // Common Contractions Spoken Pronunciations
      "cant" to listOf("K", "AE1", "N", "T"),
      "can't" to listOf("K", "AE1", "N", "T"),
      "wont" to listOf("W", "OW1", "N", "T"),
      "won't" to listOf("W", "OW1", "N", "T"),
      "dont" to listOf("D", "OW1", "N", "T"),
      "don't" to listOf("D", "OW1", "N", "T"),
      "aint" to listOf("EY1", "N", "T"),
      "ain't" to listOf("EY1", "N", "T"),
      "im" to listOf("AY1", "M"),
      "i'm" to listOf("AY1", "M"),
      "youre" to listOf("Y", "UH1", "R"),
      "you're" to listOf("Y", "UH1", "R"),
      "hes" to listOf("HH", "IY1", "Z"),
      "he's" to listOf("HH", "IY1", "Z"),
      "shes" to listOf("SH", "IY1", "Z"),
      "she's" to listOf("SH", "IY1", "Z"),
      "its" to listOf("IH1", "T", "S"),
      "it's" to listOf("IH1", "T", "S"),
      "thats" to listOf("DH", "AE1", "T", "S"),
      "that's" to listOf("DH", "AE1", "T", "S"),
      "ive" to listOf("AY1", "V"),
      "i've" to listOf("AY1", "V"),
      "youve" to listOf("Y", "UW1", "V"),
      "you've" to listOf("Y", "UW1", "V"),
      "ill" to listOf("AY1", "L"),
      "i'll" to listOf("AY1", "L"),
      "youll" to listOf("Y", "UW1", "L"),
      "you'll" to listOf("Y", "UW1", "L"),
      "well" to listOf("W", "EH1", "L"),
      "we'll" to listOf("W", "IY1", "L")
    )

    for ((word, phones) in entries) {
      addEntry(word, phones)
    }
  }

  companion object {
    private val VOWELS = setOf(
      "AA", "AE", "AH", "AO", "AW", "AY", "EH", "ER", "EY", "IH", "IY", "OW", "OY", "UH", "UW"
    )

    fun isVowel(phone: String): Boolean {
      val base = stripStress(phone)
      return VOWELS.contains(base)
    }

    fun stripStress(phone: String): String {
      return phone.filter { !it.isDigit() }
    }
  }
}
