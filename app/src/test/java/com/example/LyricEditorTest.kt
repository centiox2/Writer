package com.example

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.SongDatabase
import com.example.data.repositories.SongRepository
import com.example.ui.viewmodels.FontFamilyChoice
import com.example.ui.viewmodels.LyricEditorViewModel
import com.example.ui.viewmodels.SaveStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LyricEditorTest {

  private lateinit var database: SongDatabase
  private lateinit var repository: SongRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, SongDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = SongRepository(database.songDao())
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun testEditingAndMetricsCalculation() = runTest(testDispatcher) {
    val created = repository.createSong(title = "Midnight Melody", lyrics = "Hello world\nSecond line")
    val viewModel = LyricEditorViewModel(created.id, repository, created)

    assertEquals("Hello world\nSecond line", viewModel.uiState.value.textFieldValue.text)
    assertEquals(4, viewModel.uiState.value.wordCount)
    assertEquals(2, viewModel.uiState.value.lineCount)

    // Edit text
    val updated = TextFieldValue("A brand new song verse with seven words", TextRange(39))
    viewModel.onTextChanged(updated)

    assertEquals(8, viewModel.uiState.value.wordCount)
    assertEquals(1, viewModel.uiState.value.lineCount)
    assertEquals(39, viewModel.uiState.value.charCount)
    assertEquals(SaveStatus.UNSAVED, viewModel.uiState.value.saveStatus)
  }

  @Test
  fun testUndoAndRedo() = runTest(testDispatcher) {
    val created = repository.createSong(title = "Undo Test", lyrics = "Initial draft")
    val viewModel = LyricEditorViewModel(created.id, repository, created)

    assertFalse("Initially canUndo should be false", viewModel.uiState.value.canUndo)
    assertFalse("Initially canRedo should be false", viewModel.uiState.value.canRedo)

    // Step 1: Add verse
    viewModel.onTextChanged(TextFieldValue("Initial draft\n[Verse 1]\nWoke up early today", TextRange(46)))
    assertTrue("canUndo should be true after change", viewModel.uiState.value.canUndo)

    // Step 2: Insert section tag
    viewModel.insertSection("Chorus")
    val chorusText = viewModel.uiState.value.textFieldValue.text
    assertTrue(chorusText.contains("[Chorus]"))

    // Undo section insertion
    viewModel.undo()
    assertFalse(viewModel.uiState.value.textFieldValue.text.contains("[Chorus]"))
    assertTrue("canRedo should be true after undo", viewModel.uiState.value.canRedo)

    // Redo section insertion
    viewModel.redo()
    assertTrue(viewModel.uiState.value.textFieldValue.text.contains("[Chorus]"))

    // Undo twice
    viewModel.undo() // reverts Chorus
    viewModel.undo() // reverts to Initial draft
    assertEquals("Initial draft", viewModel.uiState.value.textFieldValue.text)
  }

  @Test
  fun testSelectionCutCopyPaste() = runTest(testDispatcher) {
    val created = repository.createSong(title = "Clipboard Test", lyrics = "One two three four five")
    val viewModel = LyricEditorViewModel(created.id, repository, created)

    // Select "three" (indices 8 to 13)
    val selectionValue = TextFieldValue("One two three four five", TextRange(8, 13))
    viewModel.onTextChanged(selectionValue)

    // Cut selected text
    val cutText = viewModel.cutSelectedText()
    assertEquals("three", cutText)
    assertEquals("One two  four five", viewModel.uiState.value.textFieldValue.text)

    // Paste text at cursor (cursor is at index 8)
    viewModel.insertTextAtCursor("SEVEN")
    assertEquals("One two SEVEN four five", viewModel.uiState.value.textFieldValue.text)

    // Select all
    viewModel.selectAll()
    assertEquals(0, viewModel.uiState.value.textFieldValue.selection.min)
    assertEquals(viewModel.uiState.value.textFieldValue.text.length, viewModel.uiState.value.textFieldValue.selection.max)

    // Clear selection
    viewModel.clearSelection()
    assertTrue(viewModel.uiState.value.textFieldValue.selection.collapsed)
  }

  @Test
  fun testLyricSectionsOutlineAndJump() = runTest(testDispatcher) {
    val created = repository.createSong(
      title = "Section Song",
      lyrics = "[Intro]\nAcoustic guitar\n\n[Verse]\nWalking down the lane\n\n[Chorus]\nSinging in the rain"
    )
    val viewModel = LyricEditorViewModel(created.id, repository, created)

    val sections = viewModel.uiState.value.detectedSections
    assertEquals(3, sections.size)
    assertEquals("Intro", sections[0].title)
    assertEquals(1, sections[0].lineNumber)
    assertEquals("Verse", sections[1].title)
    assertEquals(4, sections[1].lineNumber)
    assertEquals("Chorus", sections[2].title)
    assertEquals(7, sections[2].lineNumber)

    // Test Jump to Chorus
    viewModel.jumpToSection(sections[2].startIndex)
    val selection = viewModel.uiState.value.textFieldValue.selection
    val selectedString = viewModel.uiState.value.textFieldValue.text.substring(selection.min, selection.max)
    assertEquals("[Chorus]", selectedString)
  }

  @Test
  fun testSearchWithinLyrics() = runTest(testDispatcher) {
    val created = repository.createSong(
      title = "Search Test",
      lyrics = "Love is all, love is bright, remember the love in the night"
    )
    val viewModel = LyricEditorViewModel(created.id, repository, created)

    viewModel.toggleSearch()
    assertTrue(viewModel.uiState.value.isSearchActive)

    viewModel.onSearchQueryChanged("love")
    assertEquals(3, viewModel.uiState.value.searchMatchCount)
    assertEquals(0, viewModel.uiState.value.currentMatchIndex)

    // First match selected
    var sel = viewModel.uiState.value.textFieldValue.selection
    assertEquals("Love", viewModel.uiState.value.textFieldValue.text.substring(sel.min, sel.max))

    // Next match
    viewModel.findNextMatch()
    assertEquals(1, viewModel.uiState.value.currentMatchIndex)
    sel = viewModel.uiState.value.textFieldValue.selection
    assertEquals("love", viewModel.uiState.value.textFieldValue.text.substring(sel.min, sel.max))

    // Previous match loops around to last match
    viewModel.findPreviousMatch()
    viewModel.findPreviousMatch()
    assertEquals(2, viewModel.uiState.value.currentMatchIndex)
  }

  @Test
  fun testAutosaveAndInstantSave() = runTest(testDispatcher) {
    val created = repository.createSong(title = "Save Test", lyrics = "Start")
    val viewModel = LyricEditorViewModel(created.id, repository, created)

    viewModel.onTextChanged(TextFieldValue("Draft text autosave test", TextRange(24)))
    assertEquals(SaveStatus.UNSAVED, viewModel.uiState.value.saveStatus)

    // Trigger instant save (simulating leaving or backgrounding app)
    viewModel.saveNowSync()

    assertEquals(SaveStatus.SAVED, viewModel.uiState.value.saveStatus)

    // Verify persisted directly in database
    val persisted = repository.getSongByIdSync(created.id)
    assertNotNull(persisted)
    assertEquals("Draft text autosave test", persisted!!.lyrics)
  }

  @Test
  fun testAppRestartAndProcessRecreation() = runTest(testDispatcher) {
    val created = repository.createSong(
      title = "Process Death Test",
      lyrics = "[Intro]\nOriginal lyrics before restart"
    )

    // Step 1: User writes lyrics in editor
    val viewModel1 = LyricEditorViewModel(created.id, repository, created)
    viewModel1.insertSection("Chorus")
    viewModel1.insertTextAtCursor("Never gonna give you up\nNever gonna let you down")
    viewModel1.saveNowSync()

    val savedLyrics = viewModel1.uiState.value.textFieldValue.text

    // Step 2: Simulate process recreation / app restart
    // Fetch latest persisted song from Room database
    val persistedSong = repository.getSongByIdSync(created.id)
    assertNotNull(persistedSong)
    assertEquals(savedLyrics, persistedSong!!.lyrics)

    // Create completely fresh ViewModel instance with state restored from Room
    val viewModelRecreated = LyricEditorViewModel(created.id, repository, persistedSong)

    val restoredState = viewModelRecreated.uiState.value
    assertEquals(savedLyrics, restoredState.textFieldValue.text)
    assertEquals(SaveStatus.SAVED, restoredState.saveStatus)
    assertTrue("Should detect [Intro] and [Chorus]", restoredState.detectedSections.size >= 2)
    assertEquals("Intro", restoredState.detectedSections[0].title)
    assertEquals("Chorus", restoredState.detectedSections[1].title)
    assertEquals(viewModel1.uiState.value.wordCount, restoredState.wordCount)
    assertEquals(viewModel1.uiState.value.lineCount, restoredState.lineCount)
  }

  @Test
  fun testTypographyAndFormattingPreferences() = runTest(testDispatcher) {
    val created = repository.createSong(title = "Formatting Test", lyrics = "Typography check")
    val viewModel = LyricEditorViewModel(created.id, repository, created)

    viewModel.setFontSize(24f)
    assertEquals(24f, viewModel.uiState.value.fontSizeSp)

    viewModel.setLineSpacing(1.8f)
    assertEquals(1.8f, viewModel.uiState.value.lineSpacingMultiplier)

    viewModel.setFontFamily(FontFamilyChoice.SERIF)
    assertEquals(FontFamilyChoice.SERIF, viewModel.uiState.value.fontFamilyChoice)

    viewModel.toggleKeepScreenOn()
    assertTrue(viewModel.uiState.value.keepScreenOn)
    viewModel.toggleKeepScreenOn()
    assertFalse(viewModel.uiState.value.keepScreenOn)
  }
}
