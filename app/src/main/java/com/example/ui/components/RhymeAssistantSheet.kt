package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rhyme.RhymeCandidate
import com.example.rhyme.RhymeQueryResult
import com.example.rhyme.RhymeType
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioPurple

/**
 * Filter tab for rhyme categories.
 */
enum class RhymeFilterTab {
  ALL,
  PERFECT,
  NEAR
}

/**
 * Songwriter's Offline Rhyme Assistant Bottom Sheet.
 * Supports:
 * - Direct manual input & search
 * - Instant automatic lookup of selected words from lyrics
 * - Tabbed filtering: All Ranked, Perfect Rhymes, Near Rhymes
 * - Syllable count filtering chips
 * - One-tap Action: "Insert at Cursor"
 * - One-tap Action: "Copy to Clipboard"
 * - Multi-syllable explanations and phonetic meter tags
 * - Fallback indicator when word uses suffix/spelling patterns
 * - 100% offline (no internet needed)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RhymeAssistantSheet(
  queryWord: String,
  result: RhymeQueryResult?,
  isSearching: Boolean,
  onSearchWord: (String) -> Unit,
  onInsertWordAtCursor: (String) -> Unit,
  onCopyWord: (String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  var inputQuery by remember(queryWord) { mutableStateOf(queryWord) }
  var selectedTab by remember { mutableStateOf(RhymeFilterTab.ALL) }
  var selectedSyllableFilter by remember { mutableStateOf<Int?>(null) }
  val focusRequester = remember { FocusRequester() }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 360.dp, max = 620.dp)
        .padding(horizontal = 16.dp)
        .navigationBarsPadding()
        .imePadding()
    ) {
      // Header Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(StudioPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = null,
              tint = StudioPurple,
              modifier = Modifier.size(18.dp)
            )
          }
          Column {
            Text(
              text = "Rhyme Assistant",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Offline Phonetic & Slant Engine",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("rhyme_sheet_close_button")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close Rhyme Assistant")
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Manual Word Search Input Field
      OutlinedTextField(
        value = inputQuery,
        onValueChange = {
          inputQuery = it
          onSearchWord(it)
        },
        placeholder = { Text("Enter a word to rhyme...", fontSize = 14.sp) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = StudioPurple)
        },
        trailingIcon = {
          if (inputQuery.isNotEmpty()) {
            IconButton(
              onClick = {
                inputQuery = ""
                onSearchWord("")
              },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Clear, contentDescription = "Clear input", modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = StudioPurple,
          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester)
          .testTag("rhyme_input_field")
      )

      // Query Word Meta Card (syllables, pronunciation status, fallback badge)
      if (result != null && result.normalizedWord.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = borderModifier(result.pronunciationFound),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "\"${result.normalizedWord}\"",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = StudioBlue.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "${result.syllableCount} syl",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = StudioBlue,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            if (!result.pronunciationFound) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = StudioAmber.copy(alpha = 0.2f)
              ) {
                Text(
                  text = "Suffix Fallback",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  color = StudioAmber,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            } else {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = StudioMint.copy(alpha = 0.15f)
              ) {
                Text(
                  text = "Phonetic Match",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  color = StudioMint,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Category Tabs (All / Perfect / Near)
      val tabs = listOf(
        RhymeFilterTab.ALL to "All (${result?.allRanked?.size ?: 0})",
        RhymeFilterTab.PERFECT to "Perfect (${result?.perfectRhymes?.size ?: 0})",
        RhymeFilterTab.NEAR to "Near (${result?.nearRhymes?.size ?: 0})"
      )

      TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = StudioPurple,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
            color = StudioPurple
          )
        },
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
      ) {
        tabs.forEach { (tab, label) ->
          Tab(
            selected = selectedTab == tab,
            onClick = { selectedTab = tab },
            text = {
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            },
            modifier = Modifier.testTag("rhyme_tab_${tab.name.lowercase()}")
          )
        }
      }

      // Syllable Count Quick Filter Row
      val candidatesForCurrentTab = when (selectedTab) {
        RhymeFilterTab.ALL -> result?.allRanked ?: emptyList()
        RhymeFilterTab.PERFECT -> result?.perfectRhymes ?: emptyList()
        RhymeFilterTab.NEAR -> result?.nearRhymes ?: emptyList()
      }

      val distinctSyllables = candidatesForCurrentTab.map { it.syllableCount }.distinct().sorted()

      if (distinctSyllables.size > 1) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Syllables:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          FilterChip(
            selected = selectedSyllableFilter == null,
            onClick = { selectedSyllableFilter = null },
            label = { Text("Any", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = StudioPurple.copy(alpha = 0.2f),
              selectedLabelColor = StudioPurple
            ),
            modifier = Modifier.height(28.dp)
          )

          distinctSyllables.forEach { syl ->
            FilterChip(
              selected = selectedSyllableFilter == syl,
              onClick = {
                selectedSyllableFilter = if (selectedSyllableFilter == syl) null else syl
              },
              label = { Text("$syl syl", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = StudioPurple.copy(alpha = 0.2f),
                selectedLabelColor = StudioPurple
              ),
              modifier = Modifier.height(28.dp)
            )
          }
        }
      }

      // Results List
      val filteredCandidates = if (selectedSyllableFilter != null) {
        candidatesForCurrentTab.filter { it.syllableCount == selectedSyllableFilter }
      } else {
        candidatesForCurrentTab
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        if (isSearching) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = StudioPurple, modifier = Modifier.size(32.dp))
          }
        } else if (result == null || result.normalizedWord.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = StudioPurple.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Type a word above or tap any word in your lyrics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else if (filteredCandidates.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No rhymes found in this category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("rhyme_results_list"),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(filteredCandidates, key = { "${it.word}_${it.type}" }) { candidate ->
              RhymeCandidateItem(
                candidate = candidate,
                onInsert = { onInsertWordAtCursor(candidate.word) },
                onCopy = { onCopyWord(candidate.word) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun borderModifier(isFound: Boolean): androidx.compose.foundation.BorderStroke {
  val color = if (isFound) StudioMint.copy(alpha = 0.3f) else StudioAmber.copy(alpha = 0.3f)
  return androidx.compose.foundation.BorderStroke(1.dp, color)
}

/**
 * Individual ranked rhyme result row with copy, insert, syllable count, and phonetic category badges.
 */
@Composable
fun RhymeCandidateItem(
  candidate: RhymeCandidate,
  onInsert: () -> Unit,
  onCopy: () -> Unit,
  modifier: Modifier = Modifier
) {
  val badgeColor = when (candidate.type) {
    RhymeType.PERFECT -> StudioMint
    RhymeType.NEAR -> StudioBlue
    RhymeType.SUFFIX -> StudioAmber
  }

  val badgeText = when (candidate.type) {
    RhymeType.PERFECT -> "Perfect"
    RhymeType.NEAR -> "Near"
    RhymeType.SUFFIX -> "Suffix"
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    modifier = modifier
      .fillMaxWidth()
      .testTag("rhyme_candidate_${candidate.word}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Word + Syllable + Badge
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = candidate.word,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeColor.copy(alpha = 0.15f)
          ) {
            Text(
              text = badgeText,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = badgeColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
          }

          Text(
            text = "${candidate.syllableCount} syl",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (candidate.explanation.isNotEmpty()) {
          Text(
            text = candidate.explanation,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Right: Action Buttons (Insert at Cursor, Copy)
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Copy to clipboard
        IconButton(
          onClick = onCopy,
          modifier = Modifier
            .size(36.dp)
            .testTag("copy_rhyme_${candidate.word}")
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy ${candidate.word}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }

        // Insert at cursor
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = StudioPurple.copy(alpha = 0.15f),
          onClick = onInsert,
          modifier = Modifier.testTag("insert_rhyme_${candidate.word}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Input,
              contentDescription = "Insert at cursor",
              tint = StudioPurple,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "Insert",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = StudioPurple
            )
          }
        }
      }
    }
  }
}
