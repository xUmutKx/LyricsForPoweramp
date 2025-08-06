package io.github.abhishekabhi789.lyricsforpoweramp.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import io.github.abhishekabhi789.lyricsforpoweramp.model.Timestamp

fun transformLyrics(
    text: String,
    primaryContainerColor: Color,
    onPrimaryContainerColor: Color,
    textColor: Color,
    errorColor: Color,
    errorContainerColor: Color
): TransformedText {
    val timeStampRegex = Regex("(\\[\\d{2,3}:\\d{2}\\.\\d{2}])")
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in timeStampRegex.findAll(text)) {
            if (match.range.first > lastIndex) {
                withStyle(SpanStyle(color = textColor)) {
                    append(text.substring(lastIndex, match.range.first))
                }
            }
            val timestamp = match.value
            val isValid = isValidTimestamp(timestamp)
            val contentStart = match.range.last + 1
            val nextMatchStart =
                timeStampRegex.find(text, contentStart)?.range?.first ?: text.length
            val lyricsText = text.substring(contentStart, nextMatchStart)
            withStyle(
                ParagraphStyle(
                    textIndent = TextIndent(restLine = 95.sp),
                    lineHeight = TextUnit(1f, TextUnitType.Em)
                )
            ) {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        background = if (isValid) primaryContainerColor else errorContainerColor,
                        color = if (isValid) onPrimaryContainerColor else errorColor
                    )
                ) {
                    append(timestamp)
                }
                withStyle(SpanStyle(color = textColor)) {
                    append(lyricsText)
                }
            }
            lastIndex = nextMatchStart
        }
        if (lastIndex < text.length) {
            //any other remaining content
            withStyle(SpanStyle(color = textColor)) {
                append(text.substring(lastIndex))
            }
        }
    }
    return TransformedText(annotatedString, OffsetMapping.Identity)
}

fun isValidTimestamp(ts: String): Boolean {
    // Expecting [mm:ss.cc]
    val match = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})]").matchEntire(ts) ?: return false
    val (mm, ss, cc) = match.destructured

    val minutes = mm.toIntOrNull() ?: return false
    val seconds = ss.toIntOrNull() ?: return false
    val centis = cc.toIntOrNull() ?: return false
    return minutes >= 0 && seconds in 0..59 && centis in 0..99
}

fun getTimeStampFromRange(range: TextRange, lyrics: String): List<Timestamp> {
    val firstLine =
        (lyrics.substring(0, (range.start - 1).coerceAtLeast(0)).lines().size - 1).coerceAtLeast(0)
    val lastLine =
        (lyrics.substring(0, (range.end - 1).coerceAtLeast(0)).lines().size).coerceAtLeast(0)
    val lines = lyrics.lines().subList(firstLine, lastLine)
    val timeStampRegex = Regex("(\\[\\d{2,}:\\d{2}\\.\\d{2}])")
    return lines.flatMap { line ->
        timeStampRegex.findAll(line).mapNotNull { match ->
            Timestamp.fromString(match.value)
        }
    }
}
