package com.lkboard.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lkboard.app.model.BoardElement
import com.lkboard.app.ui.theme.LocalAppTheme

/**
 * Диалог редактирования текста стикера или текстового блока с настройкой шрифта, стиля и цвета.
 */
@Composable
fun TextEditDialog(
    element: BoardElement,
    onSave: (String) -> Unit = {},
    onSaveDetailed: (text: String, fontSize: Float, isBold: Boolean, isItalic: Boolean, colorHex: Long) -> Unit = { t, _, _, _, _ -> onSave(t) },
    onDismiss: () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    val initialText = when (element) {
        is BoardElement.StickyNote -> element.text
        is BoardElement.Text -> element.text
        else -> ""
    }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }

    val isTextElement = element is BoardElement.Text

    var fontSize by remember {
        mutableStateOf(if (element is BoardElement.Text) element.fontSize else 19f)
    }
    var isBold by remember {
        mutableStateOf(if (element is BoardElement.Text) element.isBold else false)
    }
    var isItalic by remember {
        mutableStateOf(if (element is BoardElement.Text) element.isItalic else false)
    }
    var colorHex by remember {
        mutableStateOf(if (element is BoardElement.Text) element.colorHex else (if (isDark) 0xFFF4F4F5L else 0xFF1E1E1EL))
    }

    val palette = listOf(
        if (isDark) 0xFFF4F4F5L else 0xFF0F172AL,
        0xFF2563EBL, // Blue
        0xFF16A34AL, // Green
        0xFFDC2626L, // Red
        0xFF9333EAL, // Purple
        0xFFD97706L  // Amber
    )

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun submit() {
        if (isTextElement) {
            onSaveDetailed(textFieldValue.text, fontSize, isBold, isItalic, colorHex)
        } else {
            onSave(textFieldValue.text)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        unfocusedTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
        focusedContainerColor = if (isDark) Color(0xFF242830) else Color.White,
        unfocusedContainerColor = if (isDark) Color(0xFF242830) else Color.White,
        cursorColor = Color(0xFF2563EB),
        focusedBorderColor = Color(0xFF2563EB),
        unfocusedBorderColor = if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1),
        focusedPlaceholderColor = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8),
        unfocusedPlaceholderColor = if (isDark) Color(0xFF71717A) else Color(0xFF94A3B8)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF181B20) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)),
            shadowElevation = 16.dp,
            modifier = Modifier.width(460.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides (if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A))) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                Text(
                    text = if (element is BoardElement.StickyNote) "📝 Редактирование стикера" else "✏️ Редактирование текста",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF1E293B)
                )

                // Если это текстовый блок — панель настроек стиля
                if (isTextElement) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF242830) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Размер шрифта и стили
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Размер:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))

                            listOf(14f, 18f, 24f, 32f, 48f).forEach { sz ->
                                val isSelected = (fontSize.toInt() == sz.toInt())
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF1E222A) else Color.White))
                                        .border(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                        .clickable { fontSize = sz },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sz.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Bold
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isBold) (if (isDark) Color(0xFF1E2638) else Color(0xFFDBEAFE)) else (if (isDark) Color(0xFF1E222A) else Color.White))
                                    .border(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                    .clickable { isBold = !isBold },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isBold) (if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)))
                            }

                            // Italic
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isItalic) (if (isDark) Color(0xFF1E2638) else Color(0xFFDBEAFE)) else (if (isDark) Color(0xFF1E222A) else Color.White))
                                    .border(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                    .clickable { isItalic = !isItalic },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isItalic) (if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)))
                            }
                        }

                        // Цвет
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Цвет:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                            palette.forEach { cHex ->
                                val isSelected = colorHex == cHex
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(cHex))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { colorHex = cHex }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Enter && event.isCtrlPressed) {
                                submit()
                                true
                            } else if (event.key == Key.Escape) {
                                onDismiss()
                                true
                            } else {
                                false
                            }
                        },
                    minLines = 4,
                    maxLines = 8,
                    placeholder = { Text("Введите текст... (Ctrl+Enter для сохранения)") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1))
                    ) {
                        Text("Отмена", color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                    }

                    Button(
                        onClick = { submit() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}
}
