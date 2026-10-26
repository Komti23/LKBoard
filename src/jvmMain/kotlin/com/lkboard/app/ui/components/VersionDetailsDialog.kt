package com.lkboard.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.ui.animation.bouncyClickable
import com.lkboard.app.ui.theme.LocalAppTheme
import com.lkboard.app.update.AppVersion
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.launch

/**
 * Выдвижная вкладка с подробными данными о версии приложения LKBoard и новыми функциями (What's New).
 * Выдвигается сверху с плавной slide-анимацией без артефактов и искажения шрифтов.
 */
@Composable
fun VersionDetailsDialog(
    onDismiss: () -> Unit,
    onCheckForUpdates: () -> Unit = {}
) {
    val isDark = LocalAppTheme.current.isDark
    var selectedTab by remember { mutableStateOf(0) } // 0 = Что нового, 1 = О системе

    val coroutineScope = rememberCoroutineScope()
    val animProgress = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }

    fun collapseAndDismiss() {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            animProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 160,
                    easing = FastOutLinearInEasing
                )
            )
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        )
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                    collapseAndDismiss()
                    true
                } else false
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Затемняющий фон (scrim), плавно закрывающийся при клике
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * animProgress.value.coerceIn(0f, 1f)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    collapseAndDismiss()
                }
        )

        // Выдвигающаяся карточка-шторка (чистый вертикальный slide без растягивания текста и без артефактов)
        val progress = animProgress.value.coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .padding(top = 28.dp, bottom = 28.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    // Плавный вертикальный сдвиг без растяжения шрифтов
                    translationY = (1f - progress) * -42.dp.toPx()
                    scaleX = 0.98f + 0.02f * progress
                    scaleY = 0.98f + 0.02f * progress
                    alpha = progress
                }
                .widthIn(max = 660.dp)
                .heightIn(max = 680.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0xFF16181D) else Color.White)
                .border(
                    BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                // Аккуратный индикатор-язычок шторки в стиле Apple
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isDark) Color(0xFF333A48) else Color(0xFFCBD5E1))
                )

                Spacer(Modifier.height(14.dp))

                // Заголовок с логотипом приложения и кнопкой закрытия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AppLogo.LogoIcon(size = 48.dp)

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "LKBoard Pro",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A),
                                    letterSpacing = (-0.5).sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))
                                        .border(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "v${AppVersion.VERSION_NAME}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                                    )
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Сборка ${AppVersion.VERSION_CODE} • ${AppVersion.RELEASE_DATE} • Актуальная стабильная версия",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = { collapseAndDismiss() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                            .bouncyClickable { collapseAndDismiss() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Свернуть вкладку",
                            tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Вкладки переключения: Что нового / О системе
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabButton(
                        title = "✨ Новые функции",
                        icon = Icons.Default.Stars,
                        isSelected = selectedTab == 0,
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    TabButton(
                        title = "ℹ️ О системе",
                        icon = Icons.Default.Info,
                        isSelected = selectedTab == 1,
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Содержимое выбранной вкладки (со скроллом)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (selectedTab == 0) {
                        // Список новых функций релиза
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppVersion.WHAT_IS_NEW.forEach { item ->
                                FeatureCard(item = item, isDark = isDark)
                            }
                        }
                    } else {
                        // Подробные данные системы и окружения
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SystemInfoCard(
                                title = "Окружение и движок",
                                isDark = isDark,
                                rows = listOf(
                                    "Версия приложения" to "v${AppVersion.VERSION_NAME} (Build ${AppVersion.VERSION_CODE})",
                                    "Среда выполнения Java" to "${System.getProperty("java.version")} (${System.getProperty("java.vendor")})",
                                    "Операционная система" to "${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})",
                                    "Графический стек" to "Compose Multiplatform Desktop + Skia",
                                    "Поддержка стилуса" to "Windows Pointer Events & Pressure API",
                                    "Сервер синхронизации" to AppVersion.UPDATE_URL
                                )
                            )

                            SystemInfoCard(
                                title = "Ресурсы и ссылки",
                                isDark = isDark,
                                rows = listOf(
                                    "Официальный сайт" to AppVersion.WEBSITE_URL,
                                    "Страница загрузок" to AppVersion.DOWNLOAD_PAGE_URL,
                                    "Разработчик" to "LKBoard Engineering Team"
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Нижняя панель действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                Desktop.getDesktop().browse(URI.create(AppVersion.WEBSITE_URL))
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFCBD5E1)),
                        modifier = Modifier.bouncyClickable {
                            try {
                                Desktop.getDesktop().browse(URI.create(AppVersion.WEBSITE_URL))
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Сайт LKBoard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                collapseAndDismiss()
                                onCheckForUpdates()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF242830) else Color(0xFFF1F5F9),
                                contentColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1)),
                            modifier = Modifier.bouncyClickable {
                                collapseAndDismiss()
                                onCheckForUpdates()
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Проверить обновления", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = { collapseAndDismiss() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            modifier = Modifier.bouncyClickable { collapseAndDismiss() }
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Свернуть", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) {
                    if (isDark) Color(0xFF181B20) else Color.White
                } else Color.Transparent
            )
            .bouncyClickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) (if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
            )
        }
    }
}

@Composable
private fun FeatureCard(
    item: com.lkboard.app.update.FeatureHighlight,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF20242C) else Color(0xFFF8FAFC))
            .border(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Иконка бейдж
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF181B20) else Color(0xFFEFF6FF))
                    .border(1.dp, if (isDark) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFFBFDBFE), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.icon,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SystemInfoCard(
    title: String,
    isDark: Boolean,
    rows: List<Pair<String, String>>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF20242C) else Color(0xFFF8FAFC))
            .border(1.dp, if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
            )

            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    Text(
                        text = value,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                    )
                }
            }
        }
    }
}
