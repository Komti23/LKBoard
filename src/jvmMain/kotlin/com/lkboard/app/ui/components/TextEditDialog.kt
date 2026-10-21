package com.lkboard.app.ui.components

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        mutableStateOf(if (element is BoardElement.Text) element.colorHex else 0xFF1E1E1EL)
    }

    val palette = listOf(
        0xFF0F172AL, // Dark slate
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier.width(460.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (element is BoardElement.StickyNote) "📝 Редактирование стикера" else "✏️ Редактирование текста",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                // Если это текстовый блок — панель настроек стиля
                if (isTextElement) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Размер шрифта и стили
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Размер:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))

                            listOf(14f, 18f, 24f, 32f, 48f).forEach { sz ->
                                val isSelected = (fontSize.toInt() == sz.toInt())
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF2563EB) else Color.White)
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                        .clickable { fontSize = sz },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sz.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF1E293B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Bold
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isBold) Color(0xFFDBEAFE) else Color.White)
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                    .clickable { isBold = !isBold },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isBold) Color(0xFF1D4ED8) else Color(0xFF334155))
                            }

                            // Italic
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isItalic) Color(0xFFDBEAFE) else Color.White)
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                    .clickable { isItalic = !isItalic },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isItalic) Color(0xFF1D4ED8) else Color(0xFF334155))
                            }
                        }

                        // Цвет
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Цвет:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
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
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Отмена", color = Color(0xFF64748B))
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
