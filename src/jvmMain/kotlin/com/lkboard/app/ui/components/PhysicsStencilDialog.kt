package com.lkboard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkboard.app.model.PhysicsStencilType
import com.lkboard.app.ui.theme.LocalAppTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Боковая панель физического конструктора и интерактивной мини-лаборатории (Side Drawer):
 * - Механика: динамический расчет сил на наклонной плоскости (Fтяж, N, Fтр, a), блоков и пружин.
 * - Электричество: расчет закона Ома (U, I, R, P), емкости конденсаторов и энергии.
 * - Оптика: расчет фокусного расстояния, оптической силы линзы и линейного увеличения.
 */
@Composable
fun PhysicsLabSidePanel(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSelectStencil: (type: PhysicsStencilType, label: String) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    var selectedCategory by remember { mutableStateOf("Механика") }
    var selectedType by remember { mutableStateOf(PhysicsStencilType.INCLINED_PLANE) }

    // --- Механика: параметры ---
    var massKg by remember { mutableStateOf(2.0f) }
    var angleDeg by remember { mutableStateOf(30f) }
    var frictionCoeff by remember { mutableStateOf(0.15f) }
    var springConstant by remember { mutableStateOf(100f) } // Н/м
    var pulleyMass1 by remember { mutableStateOf(3.0f) }
    var pulleyMass2 by remember { mutableStateOf(1.0f) }

    // --- Электричество: параметры ---
    var voltageVolts by remember { mutableStateOf(12.0f) }
    var resistanceOhms by remember { mutableStateOf(6.0f) }
    var capacitanceMicroFarad by remember { mutableStateOf(47.0f) }
    var inductanceMilliHenry by remember { mutableStateOf(10.0f) }

    // --- Оптика: параметры ---
    var focalLengthCm by remember { mutableStateOf(20.0f) }
    var objectDistanceCm by remember { mutableStateOf(30.0f) }

    // Вычисления в реальном времени (g = 9.8 м/с2)
    val g = 9.8f
    val angleRad = (angleDeg * PI / 180.0).toFloat()
    val gravityForce = massKg * g
    val normalForce = gravityForce * cos(angleRad)
    val slideForce = gravityForce * sin(angleRad)
    val maxFrictionForce = frictionCoeff * normalForce
    val netForce = maxOf(0f, slideForce - maxFrictionForce)
    val acceleration = netForce / massKg

    // Блок с нитью (Атвуд)
    val atwoodAcc = if (pulleyMass1 + pulleyMass2 > 0f) abs(pulleyMass1 - pulleyMass2) * g / (pulleyMass1 + pulleyMass2) else 0f
    val atwoodTension = if (pulleyMass1 + pulleyMass2 > 0f) (2f * pulleyMass1 * pulleyMass2 * g) / (pulleyMass1 + pulleyMass2) else 0f

    // Пружина
    val springDeltaCm = if (springConstant > 0f) (massKg * g / springConstant) * 100f else 0f
    val springPeriod = if (springConstant > 0f) 2f * PI.toFloat() * sqrt(massKg / springConstant) else 0f

    // Электричество
    val currentAmperes = if (resistanceOhms > 0f) voltageVolts / resistanceOhms else 0f
    val powerWatts = voltageVolts * currentAmperes
    val capEnergyMilliJoules = 0.5f * (capacitanceMicroFarad * 1e-6f) * voltageVolts * voltageVolts * 1000f

    // Оптика
    val lensPowerDiopters = if (focalLengthCm > 0f) 100f / focalLengthCm else 0f
    val imageDistanceCm = if (objectDistanceCm - focalLengthCm != 0f) (objectDistanceCm * focalLengthCm) / (objectDistanceCm - focalLengthCm) else 0f
    val magnification = if (objectDistanceCm > 0f) abs(imageDistanceCm / objectDistanceCm) else 1f

    // Автоматическая генерация информативной подписи для холста
    val computedLabel = when (selectedType) {
        PhysicsStencilType.INCLINED_PLANE -> "m=${"%.1f".format(massKg)}кг, α=${angleDeg.toInt()}°, a=${"%.1f".format(acceleration)}м/с²"
        PhysicsStencilType.PULLEY_BLOCK -> "m1=${"%.1f".format(pulleyMass1)}кг, m2=${"%.1f".format(pulleyMass2)}кг, a=${"%.1f".format(atwoodAcc)}м/с²"
        PhysicsStencilType.SPRING -> "k=${springConstant.toInt()}Н/м, m=${"%.1f".format(massKg)}кг, Δx=${"%.1f".format(springDeltaCm)}см"
        PhysicsStencilType.OPTICAL_LENS -> "F=${focalLengthCm.toInt()}см, D=${"%.1f".format(lensPowerDiopters)}дптр, Г=${"%.1f".format(magnification)}x"
        PhysicsStencilType.CIRCUIT_RESISTOR -> "R=${"%.1f".format(resistanceOhms)}Ом, I=${"%.2f".format(currentAmperes)}А, P=${"%.1f".format(powerWatts)}Вт"
        PhysicsStencilType.CIRCUIT_BATTERY -> "U=${"%.1f".format(voltageVolts)}В"
        PhysicsStencilType.CIRCUIT_CAPACITOR -> "C=${capacitanceMicroFarad.toInt()}мкФ, W=${"%.2f".format(capEnergyMilliJoules)}мДж"
        PhysicsStencilType.CIRCUIT_INDUCTOR -> "L=${inductanceMilliHenry.toInt()}мГн"
        PhysicsStencilType.CIRCUIT_SWITCH -> "Ключ К1"
        PhysicsStencilType.CIRCUIT_LAMP -> "Лампа ${"%.1f".format(powerWatts)}Вт (${voltageVolts.toInt()}В)"
    }

    val categories = listOf("Механика", "Электричество", "Оптика")
    val stencilsInCategory = PhysicsStencilType.entries.filter { it.category == selectedCategory }

    Box(modifier = Modifier.fillMaxSize()) {
        // Затемняющий оверлей
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Правая боковая панель с плавной анимацией выезда
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(500.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.28f)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(if (isDark) Color(0xFF14171D) else Color.White)
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0),
                        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Шапка
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFE2E8F0))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🔬 Физическая Лаборатория",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Интерактивный калькулятор формул и стенсилов",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF282D37) else Color(0xFFE2E8F0))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Скроллируемое тело лаборатории
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Вкладки разделов физики
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = cat == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) (if (isDark) Color(0xFF1E2638) else Color(0xFFEFF6FF))
                                            else (if (isDark) Color(0xFF242830) else Color(0xFFF8FAFC))
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                            else (if (isDark) Color(0xFF333842) else Color(0xFFCBD5E1)),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedCategory = cat
                                            selectedType = PhysicsStencilType.entries.first { it.category == cat }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when(cat) {
                                        "Механика" -> "⚙️"
                                        "Электричество" -> "⚡"
                                        else -> "🔍"
                                    }
                                    Text(
                                        text = "$icon $cat",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                    )
                                }
                            }
                        }

                        // 2. Выбор примитива в текущем разделе
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ПРИМИТИВ ДЛЯ ЭКСПЕРИМЕНТА:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                stencilsInCategory.take(3).forEach { stencil ->
                                    val isCurrent = stencil == selectedType
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isCurrent) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE))
                                                else (if (isDark) Color(0xFF1A1E26) else Color(0xFFF8FAFC))
                                            )
                                            .border(
                                                1.dp,
                                                if (isCurrent) Color(0xFF2563EB) else (if (isDark) Color(0xFF2D3545) else Color(0xFFE2E8F0)),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedType = stencil }
                                            .padding(vertical = 7.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stencil.displayName.substringBefore(" (").substringBefore(" с "),
                                            fontSize = 10.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) (if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (stencilsInCategory.size > 3) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    stencilsInCategory.drop(3).take(3).forEach { stencil ->
                                        val isCurrent = stencil == selectedType
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isCurrent) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE))
                                                    else (if (isDark) Color(0xFF1A1E26) else Color(0xFFF8FAFC))
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isCurrent) Color(0xFF2563EB) else (if (isDark) Color(0xFF2D3545) else Color(0xFFE2E8F0)),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedType = stencil }
                                                .padding(vertical = 7.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stencil.displayName.substringBefore(" (").substringBefore(" с "),
                                                fontSize = 10.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) (if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Интерактивные параметры лаборатории (ползунки и поля ввода)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF12161F) else Color(0xFFF0F7FF))
                                .border(1.dp, if (isDark) Color(0xFF1E2638) else Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🎛️ ПАРАМЕТРЫ ЛАБОРАТОРИИ:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                            )

                            when (selectedType) {
                                PhysicsStencilType.INCLINED_PLANE -> {
                                    LabSlider(label = "Масса груза m", valueText = "${"%.1f".format(massKg)} кг", value = massKg, range = 0.2f..20f) { massKg = it }
                                    LabSlider(label = "Угол наклона α", valueText = "${angleDeg.toInt()}°", value = angleDeg, range = 5f..80f) { angleDeg = it }
                                    LabSlider(label = "Коэффициент трения μ", valueText = "%.2f".format(frictionCoeff), value = frictionCoeff, range = 0.0f..0.8f) { frictionCoeff = it }
                                }
                                PhysicsStencilType.PULLEY_BLOCK -> {
                                    LabSlider(label = "Груз 1 (m1)", valueText = "${"%.1f".format(pulleyMass1)} кг", value = pulleyMass1, range = 0.5f..20f) { pulleyMass1 = it }
                                    LabSlider(label = "Груз 2 (m2)", valueText = "${"%.1f".format(pulleyMass2)} кг", value = pulleyMass2, range = 0.5f..20f) { pulleyMass2 = it }
                                }
                                PhysicsStencilType.SPRING -> {
                                    LabSlider(label = "Масса груза m", valueText = "${"%.1f".format(massKg)} кг", value = massKg, range = 0.1f..10f) { massKg = it }
                                    LabSlider(label = "Жесткость пружины k", valueText = "${springConstant.toInt()} Н/м", value = springConstant, range = 20f..500f) { springConstant = it }
                                }
                                PhysicsStencilType.CIRCUIT_RESISTOR, PhysicsStencilType.CIRCUIT_LAMP -> {
                                    LabSlider(label = "Напряжение U", valueText = "${voltageVolts.toInt()} В", value = voltageVolts, range = 1f..220f) { voltageVolts = it }
                                    LabSlider(label = "Сопротивление R", valueText = "${"%.1f".format(resistanceOhms)} Ом", value = resistanceOhms, range = 0.5f..100f) { resistanceOhms = it }
                                }
                                PhysicsStencilType.CIRCUIT_BATTERY -> {
                                    LabSlider(label = "Напряжение источника U", valueText = "${voltageVolts.toInt()} В", value = voltageVolts, range = 1.5f..220f) { voltageVolts = it }
                                }
                                PhysicsStencilType.CIRCUIT_CAPACITOR -> {
                                    LabSlider(label = "Емкость C", valueText = "${capacitanceMicroFarad.toInt()} мкФ", value = capacitanceMicroFarad, range = 1f..500f) { capacitanceMicroFarad = it }
                                    LabSlider(label = "Напряжение U", valueText = "${voltageVolts.toInt()} В", value = voltageVolts, range = 5f..250f) { voltageVolts = it }
                                }
                                PhysicsStencilType.CIRCUIT_INDUCTOR -> {
                                    LabSlider(label = "Индуктивность L", valueText = "${inductanceMilliHenry.toInt()} мГн", value = inductanceMilliHenry, range = 1f..200f) { inductanceMilliHenry = it }
                                }
                                PhysicsStencilType.OPTICAL_LENS -> {
                                    LabSlider(label = "Фокусное расстояние F", valueText = "${focalLengthCm.toInt()} см", value = focalLengthCm, range = 5f..80f) { focalLengthCm = it }
                                    LabSlider(label = "Расстояние до предмета d", valueText = "${objectDistanceCm.toInt()} см", value = objectDistanceCm, range = 5f..120f) { objectDistanceCm = it }
                                }
                                else -> {
                                    Text(
                                        text = "Готовый примитив электрической цепи",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                    )
                                }
                            }
                        }

                        // 4. Карточка живого физического расчета в реальном времени
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF0F1724) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isDark) Color(0xFF223554) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📊 РЕЗУЛЬТАТЫ ФИЗИЧЕСКИХ ВЫЧИСЛЕНИЙ:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                            )

                            when (selectedType) {
                                PhysicsStencilType.INCLINED_PLANE -> {
                                    MetricRow("Сила тяжести Fт = m·g", "${"%.1f".format(gravityForce)} Н")
                                    MetricRow("Реакция опоры N = m·g·cos(α)", "${"%.1f".format(normalForce)} Н")
                                    MetricRow("Скатывающая сила Fск = m·g·sin(α)", "${"%.1f".format(slideForce)} Н")
                                    MetricRow("Сила трения Fтр = μ·N", "${"%.1f".format(maxFrictionForce)} Н")
                                    MetricRow("Ускорение бруска a", "${"%.2f".format(acceleration)} м/с²", highlight = true)
                                }
                                PhysicsStencilType.PULLEY_BLOCK -> {
                                    MetricRow("Ускорение системы a = |m1-m2|·g/(m1+m2)", "${"%.2f".format(atwoodAcc)} м/с²", highlight = true)
                                    MetricRow("Сила натяжения нити T = 2·m1·m2·g/(m1+m2)", "${"%.1f".format(atwoodTension)} Н")
                                }
                                PhysicsStencilType.SPRING -> {
                                    MetricRow("Статическое удлинение Δx = m·g/k", "${"%.1f".format(springDeltaCm)} см")
                                    MetricRow("Период колебаний T = 2π√(m/k)", "${"%.2f".format(springPeriod)} с", highlight = true)
                                    val freq = if (springPeriod > 0f) 1f / springPeriod else 0f
                                    MetricRow("Частота колебаний ν = 1/T", "${"%.2f".format(freq)} Гц")
                                }
                                PhysicsStencilType.CIRCUIT_RESISTOR, PhysicsStencilType.CIRCUIT_LAMP -> {
                                    MetricRow("Сила тока в цепи I = U / R", "${"%.2f".format(currentAmperes)} А", highlight = true)
                                    MetricRow("Тепловая мощность P = U · I", "${"%.1f".format(powerWatts)} Вт")
                                    MetricRow("Теплота за 1 минуту Q = P · 60с", "${"%.1f".format(powerWatts * 60f)} Дж")
                                }
                                PhysicsStencilType.CIRCUIT_CAPACITOR -> {
                                    val chargeMicroC = capacitanceMicroFarad * voltageVolts
                                    MetricRow("Заряд конденсатора q = C · U", "${"%.1f".format(chargeMicroC)} мкКл")
                                    MetricRow("Энергия поля W = C·U²/2", "${"%.2f".format(capEnergyMilliJoules)} мДж", highlight = true)
                                }
                                PhysicsStencilType.OPTICAL_LENS -> {
                                    MetricRow("Оптическая сила D = 100 / F", "${"%.1f".format(lensPowerDiopters)} дптр")
                                    MetricRow("Положение изображения f = d·F/(d-F)", "${"%.1f".format(imageDistanceCm)} см", highlight = true)
                                    MetricRow("Линейное увеличение Г = |f/d|", "${"%.2f".format(magnification)}x")
                                }
                                else -> {
                                    MetricRow("Напряжение цепи", "${voltageVolts.toInt()} В")
                                }
                            }
                        }

                        // Текст подписи, которая появится на холсте
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "МАРКИРОВКА НА ХОЛСТЕ:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF0F141E) else Color.White)
                                    .border(1.dp, if (isDark) Color(0xFF283244) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = computedLabel,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }

                    // Нижняя панель действий
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFE2E8F0))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3F4554) else Color(0xFFCBD5E1))
                        ) {
                            Text("Отмена", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onSelectStencil(selectedType, computedLabel)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("✓ Добавить в лабораторию на доску", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)
            )
            Text(
                text = valueText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MetricRow(
    title: String,
    value: String,
    highlight: Boolean = false
) {
    val isDark = LocalAppTheme.current.isDark
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = if (highlight) (if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)) else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A))
        )
    }
}

/**
 * Обратная совместимость для вызова как Dialog.
 */
@Composable
fun PhysicsStencilDialog(
    onDismiss: () -> Unit,
    onSelectStencil: (type: PhysicsStencilType, label: String) -> Unit
) {
    PhysicsLabSidePanel(
        isOpen = true,
        onDismiss = onDismiss,
        onSelectStencil = onSelectStencil
    )
}
