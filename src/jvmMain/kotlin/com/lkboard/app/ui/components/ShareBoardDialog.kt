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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.collaboration.CollabSessionState
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Стеклянный диалог совместной работы и шеринга доски в стиле Apple Liquid Glass.
 */
@Composable
fun ShareBoardDialog(
    collabState: CollabSessionState,
    boardTitle: String,
    onStartHosting: (port: Int, userName: String) -> Unit,
    onStopHosting: () -> Unit,
    onConnectAsClient: (hostUrl: String, userName: String) -> Unit,
    onDisconnectClient: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (collabState.isConnectedAsClient) 1 else 0) } // 0: Хост (Раздать), 1: Подключиться
    var hostUserName by remember { mutableStateOf(collabState.userName.ifBlank { "Хост" }) }
    var clientUserName by remember { mutableStateOf(collabState.userName.ifBlank { "Участник" }) }
    var joinUrlInput by remember { mutableStateOf(collabState.hostUrl.ifBlank { "http://192.168.1." }) }
    var copiedWebFeedback by remember { mutableStateOf(false) }
    var copiedPublicFeedback by remember { mutableStateOf(false) }
    var copiedAppFeedback by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String, type: Int) {
        try {
            val sel = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
            when (type) {
                0 -> copiedPublicFeedback = true
                1 -> copiedWebFeedback = true
                else -> copiedAppFeedback = true
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(580.dp)
                .clip(RoundedCornerShape(24.dp))
                .shadow(28.dp, RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.94f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.90f)), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(26.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // Шапка диалога
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Совместная работа",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = boardTitle,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Вкладки выбора режима (Хост / Подключиться)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) Color.White else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color(0xFF0284C7) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Поделиться ссылкой (Хост)",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) Color.White else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Laptop,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color(0xFF0284C7) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Подключиться к доске",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color(0xFF0F172A) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedTab == 0) {
                    // --- ВКЛАДКА 1: ХОСТ (РАЗДАТЬ ССЫЛКУ) ---

                    // Статусный баннер сессии
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (collabState.isHosting) Color(0xFFF0FDF4) else Color(0xFFF8FAFC))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (collabState.isHosting) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (collabState.isHosting) Color(0xFF10B981) else Color(0xFF94A3B8))
                                )
                                Column {
                                    Text(
                                        text = if (collabState.isHosting) "Трансляция активна" else "Совместный доступ выключен",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (collabState.isHosting) Color(0xFF15803D) else Color(0xFF475569)
                                    )
                                    Text(
                                        text = if (collabState.isHosting) "Подключено участников: ${collabState.connectedPeers.size}" else "Включите трансляцию, чтобы сгенерировать ссылку",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (collabState.isHosting) onStopHosting() else onStartHosting(collabState.serverPort, hostUserName)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (collabState.isHosting) Color(0xFFEF4444) else Color(0xFF0284C7)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (collabState.isHosting) "Остановить" else "Включить доступ", fontSize = 13.sp)
                            }
                        }
                    }

                    if (collabState.isHosting) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Общедоступная облачная интернет-ссылка (через сервер 164.37.224.219)
                        if (collabState.publicWebShareLink.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("🌐", fontSize = 14.sp)
                                        Text("Интернет-ссылка (для участников в любой точке мира)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    }
                                    if (copiedPublicFeedback) {
                                        Text("Скопировано!", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .border(BorderStroke(1.2.dp, Color(0xFF93C5FD)), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = collabState.publicWebShareLink,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF1D4ED8),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = { copyToClipboard(collabState.publicWebShareLink, type = 0) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Копировать", fontSize = 12.sp)
                                    }
                                }
                                Text(
                                    text = "Открывается на любом телефоне, планшете или компьютере через интернет (работает без общего Wi-Fi).",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 2. Прямая локальная веб-ссылка для локальной сети (Wi-Fi)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("🏠", fontSize = 14.sp)
                                    Text("Локальная ссылка (для одной Wi-Fi сети)", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = Color(0xFF334155))
                                }
                                if (copiedWebFeedback) {
                                    Text("Скопировано!", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = collabState.webShareLink,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedButton(
                                    onClick = { copyToClipboard(collabState.webShareLink, type = 1) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Копировать", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Код комнаты и приложение LKBoard
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                        Text("Код комнаты", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Text(collabState.roomCode, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { copyToClipboard(collabState.appShareLink, type = 2) }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Laptop, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            Text("Для приложения LKBoard", fontSize = 11.sp, color = Color(0xFF64748B))
                                        }
                                        Text(if (copiedAppFeedback) "Скопировано!" else "Копировать код", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                                    }
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Подсказка об облачном сервере
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0FDF4))
                                .border(BorderStroke(1.dp, Color(0xFFBBF7D0)), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "🚀 Облачный сервер LKBoard (164.37.224.219) активен: участники могут мгновенно подключиться по интернет-ссылке из любого браузера или смартфона без дополнительных настроек!",
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                } else {
                    // --- ВКЛАДКА 2: ПОДКЛЮЧИТЬСЯ К ДОСКЕ ---

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = joinUrlInput,
                            onValueChange = { joinUrlInput = it },
                            label = { Text("Ссылка или адрес хоста (http://ip:порт или облако)") },
                            placeholder = { Text("например, http://164.37.224.219:8765/?room=LK-1234") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        OutlinedTextField(
                            value = clientUserName,
                            onValueChange = { clientUserName = it },
                            label = { Text("Ваше отображаемое имя") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        // Индикатор статуса подключения
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (collabState.isConnectedAsClient) Color(0xFF10B981) else Color(0xFF94A3B8))
                            )
                            Text(
                                text = collabState.statusMessage,
                                fontSize = 12.sp,
                                color = if (collabState.isConnectedAsClient) Color(0xFF15803D) else Color(0xFF64748B)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (collabState.isConnectedAsClient) {
                                Button(
                                    onClick = onDisconnectClient,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Отключиться")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (joinUrlInput.isNotBlank()) {
                                            onConnectAsClient(joinUrlInput.trim(), clientUserName.ifBlank { "Участник" })
                                        }
                                    },
                                    enabled = joinUrlInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Подключиться к доске")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Нижняя панель действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Готово", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
