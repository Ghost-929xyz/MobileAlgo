package dev.algoforge.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.algoforge.core.Language

@Composable
fun CodeEditor(
    code: String,
    language: Language,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val lineCount = remember(code) { code.lineSequence().count().coerceAtLeast(1) }
    val transformation = remember(language, colors.primary, colors.tertiary, colors.secondary, colors.outline) {
        SyntaxHighlightTransformation(
            language = language,
            keywordColor = colors.primary,
            stringColor = colors.tertiary,
            numberColor = colors.secondary,
            commentColor = colors.outline,
        )
    }
    var value by remember { mutableStateOf(TextFieldValue(code, TextRange(code.length))) }
    LaunchedEffect(code) {
        if (value.text != code) value = value.copy(text = code, selection = TextRange(code.length))
    }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(vertical),
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            repeat(lineCount) { line ->
                Text(
                    text = (line + 1).toString(),
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 21.sp,
                )
            }
        }
        BasicTextField(
            value = value,
            onValueChange = {
                value = it
                onCodeChange(it.text)
            },
            modifier = Modifier
                .width(900.dp)
                .horizontalScroll(horizontal)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            ),
            visualTransformation = transformation,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            minLines = lineCount,
        )
    }
}

private class SyntaxHighlightTransformation(
    private val language: Language,
    private val keywordColor: Color,
    private val stringColor: Color,
    private val numberColor: Color,
    private val commentColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)
        val protectedRanges = ArrayList<IntRange>()

        commentRegex(language).findAll(text.text).forEach { match ->
            builder.addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
            protectedRanges += match.range
        }
        STRING.findAll(text.text).forEach { match ->
            builder.addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            protectedRanges += match.range
        }
        keywordRegex(language).findAll(text.text).forEach { match ->
            if (protectedRanges.none { match.range.first in it }) {
                builder.addStyle(SpanStyle(color = keywordColor), match.range.first, match.range.last + 1)
            }
        }
        NUMBER.findAll(text.text).forEach { match ->
            if (protectedRanges.none { match.range.first in it }) {
                builder.addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun commentRegex(language: Language) = when (language) {
        Language.PYTHON -> Regex("(?m)#[^\n]*")
        else -> Regex("(?s)/\\*.*?\\*/|(?m)//[^\\n]*")
    }

    private fun keywordRegex(language: Language) = Regex(
        "\\b(" + when (language) {
            Language.PYTHON -> "and|as|assert|async|await|break|case|class|continue|def|del|elif|else|except|False|finally|for|from|global|if|import|in|is|lambda|None|nonlocal|not|or|pass|raise|return|True|try|while|with|yield"
            Language.CPP -> "alignas|auto|bool|break|case|catch|char|class|const|constexpr|continue|default|delete|do|double|else|enum|explicit|extern|false|float|for|friend|if|inline|int|long|namespace|new|nullptr|operator|private|protected|public|return|short|signed|sizeof|static|struct|switch|template|this|throw|true|try|typedef|typename|union|unsigned|using|virtual|void|volatile|while"
            Language.C -> "auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|inline|int|long|register|restrict|return|short|signed|sizeof|static|struct|switch|typedef|union|unsigned|void|volatile|while"
            Language.JAVA -> "abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while"
        } + ")\\b",
    )

    private companion object {
        val STRING = Regex("\"(?:\\.|[^\"\\\\])*\"|'(?:\\.|[^'\\\\])*'")
        val NUMBER = Regex("\\b(?:0[xX][0-9a-fA-F]+|\\d+(?:\\.\\d+)?)\\b")
    }
}
