package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

fun transformLyrics(
    text: AnnotatedString,
    vararg currentPlayingLines: Int,
    selectionLineIndexes: IntRange,
    textColor: Color,
    selectionContainerColor: Color,
    onSelectionContainerColor: Color,
    timestampContainerColor: Color,
    onTimestampContainerColor: Color,
    errorContainer: Color,
    onErrorContainer: Color
): TransformedText {
    val potentialTimestampRegex = Regex("(^\\[\\d.*])")
    val validTimestampStyle = SpanStyle(
        color = onTimestampContainerColor,
        background = timestampContainerColor
    )
    val invalidTimestampStyle = SpanStyle(color = onErrorContainer, background = errorContainer)

    val annotatedString = buildAnnotatedString {
        text.text.lines().forEachIndexed { lineIndex, line ->
            val (color, bgColor) = when (lineIndex) {
                in currentPlayingLines -> onSelectionContainerColor to selectionContainerColor
                in selectionLineIndexes ->
                    onSelectionContainerColor to selectionContainerColor.copy(0.3f)

                else -> textColor to Color.Unspecified
            }

            val lineStyle = SpanStyle(
                color = color,
                background = bgColor,
                fontWeight = if (lineIndex in currentPlayingLines) FontWeight.Bold else FontWeight.Normal
            )
            withStyle(
                ParagraphStyle(
                    textIndent = TextIndent(restLine = 6.5.em),
                    lineHeight = 1.4.em
                )
            ) {
                var currentIndex = 0
                potentialTimestampRegex.findAll(line).forEach { match ->
                    if (match.range.first > currentIndex) {
                        withStyle(lineStyle) {
                            append(line.substring(currentIndex, match.range.first))
                        }
                    }

                    val tsText = match.value
                    val tsStyle =
                        if (isValidTimestamp(tsText)) validTimestampStyle else invalidTimestampStyle
                    withStyle(tsStyle) {
                        append(tsText)
                    }

                    currentIndex = match.range.last + 1
                }
                if (currentIndex < line.length) {
                    withStyle(lineStyle) {
                        append(line.substring(currentIndex))
                    }
                }

                if (lineIndex != text.text.lines().lastIndex) {
                    append("\n")
                }
            }
        }
    }
    return TransformedText(annotatedString, OffsetMapping.Identity)
}

fun isValidTimestamp(ts: String): Boolean {
    // Expecting [mm:ss.cc]
    val match = Regex("\\[(\\d{2,3}):(\\d{2})\\.(\\d{2})]").matchEntire(ts) ?: return false
    val (mm, ss, cc) = match.destructured
    val minutes = mm.toIntOrNull() ?: return false
    val seconds = ss.toIntOrNull() ?: return false
    val centiseconds = cc.toIntOrNull() ?: return false
    return minutes >= 0 && seconds in 0..59 && centiseconds in 0..99
}

fun getLineIndexesForSelection(textFieldValue: TextFieldValue): IntRange {
    val lyricsContent = textFieldValue.text
    val range = textFieldValue.selection
    val startIndex = minOf(range.start, range.end)
    val endIndex = maxOf(range.start, range.end)

    val firstLine =
        (lyricsContent.substring(0, (startIndex).coerceAtLeast(0)).lines().size - 1)
            .coerceAtLeast(0)
    val lastLine =
        (lyricsContent.substring(0, (endIndex).coerceAtLeast(0)).lines().size - 1)
            .coerceAtLeast(0)
    return IntRange(firstLine, lastLine)
}
