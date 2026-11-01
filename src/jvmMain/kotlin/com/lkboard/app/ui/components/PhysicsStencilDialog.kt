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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Боковая панель физического конструктора и интерактивной мини-лаборатории (Side Drawer):
 * - Механика: динамический расчет сил на наклонной плоскости (Fтяж, N, Fтр, a), блоков Атвуда, пружин, нитяного маятника и правила моментов рычага.
 * - Термодинамика: изопроцессы в цилиндре под поршнем, работа газа A = p·ΔV, температура T, замкнутый цикл p-V.
 * - Электричество: расчет закона Ома (U, I, R, P), конденсаторов, катушек, амперметров, вольтметров и реостатов.
 * - Магнетизм: сила Лоренца (Fл = qvB) и сила Ампера (Fa = IBL) в однородном магнитном поле.
 * - Оптика: тонкая собирающая линза (F, d, D, Г) и оптическая призма с углом наименьшего отклонения луча.
 */
@Composable
fun PhysicsLabSidePanel(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSelectStencil: (type: PhysicsStencilType, label: String, initialParams: Map<String, Float>) -> Unit
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
    var pendulumLengthM by remember { mutableStateOf(1.0f) }
    var pendulumGravity by remember { mutableStateOf(9.8f) } // Земля 9.8, Луна 1.6, Марс 3.7
    var leverArm1Cm by remember { mutableStateOf(40f) }
    var leverArm2Cm by remember { mutableStateOf(20f) }
    var leverMass1Kg by remember { mutableStateOf(2.0f) }

    // --- Термодинамика: параметры ---
    var thermoPressureKPa by remember { mutableStateOf(100f) } // кПа
    var thermoVol1L by remember { mutableStateOf(2.0f) } // литры
    var thermoVol2L by remember { mutableStateOf(5.0f) }
    var thermoMoles by remember { mutableStateOf(1.0f) } // моль
    var pvP1KPa by remember { mutableStateOf(100f) }
    var pvP2KPa by remember { mutableStateOf(300f) }
    var pvV1L by remember { mutableStateOf(2.0f) }
    var pvV2L by remember { mutableStateOf(6.0f) }

    // --- Электричество: параметры ---
    var voltageVolts by remember { mutableStateOf(12.0f) }
    var resistanceOhms by remember { mutableStateOf(6.0f) }
    var capacitanceMicroFarad by remember { mutableStateOf(47.0f) }
    var inductanceMilliHenry by remember { mutableStateOf(10.0f) }
    var rheostatMaxR by remember { mutableStateOf(50.0f) }
    var rheostatCurR by remember { mutableStateOf(20.0f) }

    // --- Магнетизм: параметры ---
    var magnetFieldTesla by remember { mutableStateOf(0.5f) } // Тл
    var particleChargeMicroC by remember { mutableStateOf(10.0f) } // мкКл
    var particleSpeedMps by remember { mutableStateOf(1000.0f) } // м/с
    var wireCurrentA by remember { mutableStateOf(3.0f) } // А
    var wireLengthCm by remember { mutableStateOf(20.0f) } // см

    // --- Оптика: параметры ---
    var focalLengthCm by remember { mutableStateOf(20.0f) }
    var objectDistanceCm by remember { mutableStateOf(30.0f) }
    var prismApexAngleDeg by remember { mutableStateOf(60.0f) }
    var prismRefractiveIndex by remember { mutableStateOf(1.50f) }

    // --- Вычисления в реальном времени ---
    val g = pendulumGravity
    val angleRad = (angleDeg * PI / 180.0).toFloat()
    val gravityForce = massKg * g
    val normalForce = gravityForce * cos(angleRad)
    val slideForce = gravityForce * sin(angleRad)
    val maxFrictionForce = frictionCoeff * normalForce
    val netForce = maxOf(0f, slideForce - maxFrictionForce)
    val acceleration = netForce / massKg

    // Атвуд
    val atwoodAcc = if (pulleyMass1 + pulleyMass2 > 0f) abs(pulleyMass1 - pulleyMass2) * g / (pulleyMass1 + pulleyMass2) else 0f
    val atwoodTension = if (pulleyMass1 + pulleyMass2 > 0f) (2f * pulleyMass1 * pulleyMass2 * g) / (pulleyMass1 + pulleyMass2) else 0f

    // Пружина
    val springDeltaCm = if (springConstant > 0f) (massKg * g / springConstant) * 100f else 0f
    val springPeriod = if (springConstant > 0f) 2f * PI.toFloat() * sqrt(massKg / springConstant) else 0f

    // Нитяной маятник
    val pendulumPeriod = if (pendulumGravity > 0f && pendulumLengthM > 0f) 2f * PI.toFloat() * sqrt(pendulumLengthM / pendulumGravity) else 0f
    val pendulumFreq = if (pendulumPeriod > 0f) 1f / pendulumPeriod else 0f

    // Рычаг
    val leverMass2Kg = if (leverArm2Cm > 0f) (leverMass1Kg * leverArm1Cm) / leverArm2Cm else 0f
    val leverTorque1 = (leverMass1Kg * g * (leverArm1Cm / 100f))

    // Термодинамика: работа и температура
    val rConst = 8.314f // Дж/(моль·К)
    val deltaVolL = thermoVol2L - thermoVol1L
    val thermoWorkJoules = thermoPressureKPa * 1000f * (deltaVolL * 1e-3f) // p · ΔV
    val temp1K = if (thermoMoles > 0f) (thermoPressureKPa * 1000f * (thermoVol1L * 1e-3f)) / (thermoMoles * rConst) else 0f
    val temp2K = if (thermoMoles > 0f) (thermoPressureKPa * 1000f * (thermoVol2L * 1e-3f)) / (thermoMoles * rConst) else 0f

    // Цикл p-V
    val cycleWorkJoules = abs(pvP2KPa - pvP1KPa) * 1000f * (abs(pvV2L - pvV1L) * 1e-3f)

    // Электричество
    val currentAmperes = if (resistanceOhms > 0f) voltageVolts / resistanceOhms else 0f
    val powerWatts = voltageVolts * currentAmperes
    val capEnergyMilliJoules = 0.5f * (capacitanceMicroFarad * 1e-6f) * voltageVolts * voltageVolts * 1000f
    val rheostatCurrent = if (rheostatCurR > 0f) voltageVolts / rheostatCurR else 0f

    // Магнетизм
    val lorentzForceN = (particleChargeMicroC * 1e-6f) * particleSpeedMps * magnetFieldTesla
    val ampereForceN = wireCurrentA * magnetFieldTesla * (wireLengthCm / 100f)

    // Оптика
    val lensPowerDiopters = if (focalLengthCm > 0f) 100f / focalLengthCm else 0f
    val imageDistanceCm = if (objectDistanceCm - focalLengthCm != 0f) (objectDistanceCm * focalLengthCm) / (objectDistanceCm - focalLengthCm) else 0f
    val magnification = if (objectDistanceCm > 0f) abs(imageDistanceCm / objectDistanceCm) else 1f

    // Призма: угол наименьшего отклонения δmin = 2·arcsin(n·sin(A/2)) - A
    val apexRad = (prismApexAngleDeg * PI / 180.0).toFloat()
    val sinHalfApex = sin(apexRad / 2f)
    val devMinDeg = try {
        val arg = prismRefractiveIndex * sinHalfApex
        if (arg in -1f..1f) {
            val devRad = 2f * asin(arg) - apexRad
            (devRad * 180f / PI).toFloat()
        } else 0f
    } catch (_: Exception) { 0f }

    // Автоматическая генерация информативной подписи для холста
    val computedLabel = when (selectedType) {
        PhysicsStencilType.INCLINED_PLANE -> "m=${"%.1f".format(massKg)}кг, α=${angleDeg.toInt()}°, a=${"%.1f".format(acceleration)}м/с²"
        PhysicsStencilType.PULLEY_BLOCK -> "m1=${"%.1f".format(pulleyMass1)}кг, m2=${"%.1f".format(pulleyMass2)}кг, a=${"%.1f".format(atwoodAcc)}м/с²"
        PhysicsStencilType.SPRING -> "k=${springConstant.toInt()}Н/м, m=${"%.1f".format(massKg)}кг, T=${"%.2f".format(springPeriod)}с"
        PhysicsStencilType.MATHEMATICAL_PENDULUM -> "L=${"%.2f".format(pendulumLengthM)}м, T=${"%.2f".format(pendulumPeriod)}с, ν=${"%.2f".format(pendulumFreq)}Гц"
        PhysicsStencilType.LEVER_BALANCE -> "d1=${leverArm1Cm.toInt()}см, d2=${leverArm2Cm.toInt()}см, m2=${"%.1f".format(leverMass2Kg)}кг"

        PhysicsStencilType.THERMO_CYLINDER -> "p=${thermoPressureKPa.toInt()}кПа, A=${"%.1f".format(thermoWorkJoules)}Дж, T=${temp1K.toInt()}..${temp2K.toInt()}K"
        PhysicsStencilType.THERMO_PV_CYCLE -> "A(цикл)=${"%.1f".format(cycleWorkJoules)}Дж, p=${pvP1KPa.toInt()}..${pvP2KPa.toInt()}кПа"

        PhysicsStencilType.OPTICAL_LENS -> "F=${focalLengthCm.toInt()}см, D=${"%.1f".format(lensPowerDiopters)}дптр, Г=${"%.1f".format(magnification)}x"
        PhysicsStencilType.OPTICAL_PRISM -> "A=${prismApexAngleDeg.toInt()}°, n=${"%.2f".format(prismRefractiveIndex)}, δ(мин)=${"%.1f".format(devMinDeg)}°"

        PhysicsStencilType.CIRCUIT_RESISTOR -> "R=${"%.1f".format(resistanceOhms)}Ом, I=${"%.2f".format(currentAmperes)}А, P=${"%.1f".format(powerWatts)}Вт"
        PhysicsStencilType.CIRCUIT_BATTERY -> "U=${"%.1f".format(voltageVolts)}В"
        PhysicsStencilType.CIRCUIT_CAPACITOR -> "C=${capacitanceMicroFarad.toInt()}мкФ, W=${"%.2f".format(capEnergyMilliJoules)}мДж"
        PhysicsStencilType.CIRCUIT_INDUCTOR -> "L=${inductanceMilliHenry.toInt()}мГн"
        PhysicsStencilType.CIRCUIT_SWITCH -> "Ключ К1"
        PhysicsStencilType.CIRCUIT_LAMP -> "Лампа ${"%.1f".format(powerWatts)}Вт (${voltageVolts.toInt()}В)"
        PhysicsStencilType.CIRCUIT_AMMETER -> "Амперметр: I=${"%.2f".format(currentAmperes)}А"
        PhysicsStencilType.CIRCUIT_VOLTMETER -> "Вольтметр: U=${voltageVolts.toInt()}В"
        PhysicsStencilType.CIRCUIT_RHEOSTAT -> "Реостат: R=${rheostatCurR.toInt()}/${rheostatMaxR.toInt()}Ом, I=${"%.2f".format(rheostatCurrent)}А"

        PhysicsStencilType.MAGNET_FIELD -> "B=${"%.2f".format(magnetFieldTesla)}Тл, F(Лоренца)=${"%.3f".format(lorentzForceN * 1000f)}мН, F(Ампера)=${"%.2f".format(ampereForceN)}Н"
    }

    val categories = listOf("Механика", "Термодинамика", "Электричество", "Магнетизм", "Оптика")
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
                    .width(520.dp)
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
                    // 1. Премиальная шапка без эмодзи
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF181B22) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isDark) Color(0xFF232730) else Color(0xFFE2E8F0))
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF1E2638) else Color(0xFFEFF6FF))
                                    .border(1.dp, if (isDark) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFFBFDBFE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PrecisionManufacturing,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Физическая лаборатория",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFF4F4F5) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Интерактивный симулятор физических законов и стенсилов",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            }
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

                    // 2. Скроллируемое тело лаборатории
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Вкладки разделов физики с векторными иконками
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = cat == selectedCategory
                                val catIcon = when (cat) {
                                    "Механика" -> Icons.Default.PrecisionManufacturing
                                    "Термодинамика" -> Icons.Default.Whatshot
                                    "Электричество" -> Icons.Default.ElectricBolt
                                    "Магнетизм" -> Icons.Default.Explore
                                    else -> Icons.Default.Visibility
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) (if (isDark) Color(0xFF1E2638) else Color(0xFFEFF6FF))
                                            else (if (isDark) Color(0xFF1E222A) else Color(0xFFF8FAFC))
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB))
                                            else (if (isDark) Color(0xFF2E333D) else Color(0xFFE2E8F0)),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedCategory = cat
                                            selectedType = PhysicsStencilType.entries.first { it.category == cat }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = cat,
                                            tint = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = cat,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) (if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Выбор примитива в текущем разделе
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ПРИМИТИВ ДЛЯ ЭКСПЕРИМЕНТА:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val chunks = stencilsInCategory.chunked(3)
                                chunks.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { stencil ->
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
                                        // Заполнение пустых мест в строке
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Интерактивные параметры лаборатории
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF12161F) else Color(0xFFF0F7FF))
                                .border(1.dp, if (isDark) Color(0xFF1E2638) else Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "ПАРАМЕТРЫ ЛАБОРАТОРИИ:",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                )
                            }

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
                                PhysicsStencilType.MATHEMATICAL_PENDULUM -> {
                                    LabSlider(label = "Длина подвеса L", valueText = "${"%.2f".format(pendulumLengthM)} м", value = pendulumLengthM, range = 0.1f..4.0f) { pendulumLengthM = it }
                                    LabSlider(label = "Ускорение g (Земля/Луна/Марс)", valueText = "${"%.1f".format(pendulumGravity)} м/с²", value = pendulumGravity, range = 1.6f..12.0f) { pendulumGravity = it }
                                }
                                PhysicsStencilType.LEVER_BALANCE -> {
                                    LabSlider(label = "Плечо 1 (d1)", valueText = "${leverArm1Cm.toInt()} см", value = leverArm1Cm, range = 10f..100f) { leverArm1Cm = it }
                                    LabSlider(label = "Плечо 2 (d2)", valueText = "${leverArm2Cm.toInt()} см", value = leverArm2Cm, range = 10f..100f) { leverArm2Cm = it }
                                    LabSlider(label = "Груз 1 (m1)", valueText = "${"%.1f".format(leverMass1Kg)} кг", value = leverMass1Kg, range = 0.5f..20f) { leverMass1Kg = it }
                                }

                                PhysicsStencilType.THERMO_CYLINDER -> {
                                    LabSlider(label = "Давление газа p", valueText = "${thermoPressureKPa.toInt()} кПа", value = thermoPressureKPa, range = 20f..500f) { thermoPressureKPa = it }
                                    LabSlider(label = "Начальный объем V1", valueText = "${"%.1f".format(thermoVol1L)} л", value = thermoVol1L, range = 1f..10f) { thermoVol1L = it }
                                    LabSlider(label = "Конечный объем V2", valueText = "${"%.1f".format(thermoVol2L)} л", value = thermoVol2L, range = 1f..10f) { thermoVol2L = it }
                                    LabSlider(label = "Количество вещества ν", valueText = "${"%.1f".format(thermoMoles)} моль", value = thermoMoles, range = 0.2f..5f) { thermoMoles = it }
                                }
                                PhysicsStencilType.THERMO_PV_CYCLE -> {
                                    LabSlider(label = "Давление p1", valueText = "${pvP1KPa.toInt()} кПа", value = pvP1KPa, range = 50f..300f) { pvP1KPa = it }
                                    LabSlider(label = "Давление p2", valueText = "${pvP2KPa.toInt()} кПа", value = pvP2KPa, range = 100f..600f) { pvP2KPa = it }
                                    LabSlider(label = "Объем V1", valueText = "${"%.1f".format(pvV1L)} л", value = pvV1L, range = 1f..6f) { pvV1L = it }
                                    LabSlider(label = "Объем V2", valueText = "${"%.1f".format(pvV2L)} л", value = pvV2L, range = 2f..12f) { pvV2L = it }
                                }

                                PhysicsStencilType.CIRCUIT_RESISTOR, PhysicsStencilType.CIRCUIT_LAMP, PhysicsStencilType.CIRCUIT_AMMETER -> {
                                    LabSlider(label = "Напряжение цепи U", valueText = "${voltageVolts.toInt()} В", value = voltageVolts, range = 1f..220f) { voltageVolts = it }
                                    LabSlider(label = "Сопротивление нагрузки R", valueText = "${"%.1f".format(resistanceOhms)} Ом", value = resistanceOhms, range = 0.5f..100f) { resistanceOhms = it }
                                }
                                PhysicsStencilType.CIRCUIT_BATTERY, PhysicsStencilType.CIRCUIT_VOLTMETER -> {
                                    LabSlider(label = "ЭДС источника U", valueText = "${voltageVolts.toInt()} В", value = voltageVolts, range = 1.5f..220f) { voltageVolts = it }
                                }
                                PhysicsStencilType.CIRCUIT_CAPACITOR -> {
                                    LabSlider(label = "Емкость C", valueText = "${capacitanceMicroFarad.toInt()} мкФ", value = capacitanceMicroFarad, range = 1f..500f) { capacitanceMicroFarad = it }
                                    LabSlider(label = "Напряжение U", valueText = "${voltageVolts.toInt()} В", value = voltageVolts, range = 5f..250f) { voltageVolts = it }
                                }
                                PhysicsStencilType.CIRCUIT_INDUCTOR -> {
                                    LabSlider(label = "Индуктивность L", valueText = "${inductanceMilliHenry.toInt()} мГн", value = inductanceMilliHenry, range = 1f..200f) { inductanceMilliHenry = it }
                                }
                                PhysicsStencilType.CIRCUIT_RHEOSTAT -> {
                                    LabSlider(label = "Текущее положение R", valueText = "${rheostatCurR.toInt()} Ом", value = rheostatCurR, range = 0f..rheostatMaxR) { rheostatCurR = it }
                                    LabSlider(label = "Максимальное Rmax", valueText = "${rheostatMaxR.toInt()} Ом", value = rheostatMaxR, range = 10f..200f) { rheostatMaxR = it }
                                }

                                PhysicsStencilType.MAGNET_FIELD -> {
                                    LabSlider(label = "Магнитная индукция B", valueText = "${"%.2f".format(magnetFieldTesla)} Тл", value = magnetFieldTesla, range = 0.05f..2.5f) { magnetFieldTesla = it }
                                    LabSlider(label = "Заряд частицы q", valueText = "${particleChargeMicroC.toInt()} мкКл", value = particleChargeMicroC, range = 1f..100f) { particleChargeMicroC = it }
                                    LabSlider(label = "Скорость частицы v", valueText = "${particleSpeedMps.toInt()} м/с", value = particleSpeedMps, range = 100f..5000f) { particleSpeedMps = it }
                                    LabSlider(label = "Ток в проводнике I", valueText = "${wireCurrentA.toInt()} А", value = wireCurrentA, range = 0.5f..15f) { wireCurrentA = it }
                                }

                                PhysicsStencilType.OPTICAL_LENS -> {
                                    LabSlider(label = "Фокусное расстояние F", valueText = "${focalLengthCm.toInt()} см", value = focalLengthCm, range = 5f..80f) { focalLengthCm = it }
                                    LabSlider(label = "Расстояние до предмета d", valueText = "${objectDistanceCm.toInt()} см", value = objectDistanceCm, range = 5f..120f) { objectDistanceCm = it }
                                }
                                PhysicsStencilType.OPTICAL_PRISM -> {
                                    LabSlider(label = "Преломляющий угол A", valueText = "${prismApexAngleDeg.toInt()}°", value = prismApexAngleDeg, range = 30f..75f) { prismApexAngleDeg = it }
                                    LabSlider(label = "Показатель преломления n", valueText = "%.2f".format(prismRefractiveIndex), value = prismRefractiveIndex, range = 1.33f..1.95f) { prismRefractiveIndex = it }
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

                        // Карточка живого физического расчета в реальном времени
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF0F1724) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isDark) Color(0xFF223554) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "РЕЗУЛЬТАТЫ ФИЗИЧЕСКИХ ВЫЧИСЛЕНИЙ:",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB)
                                )
                            }

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
                                PhysicsStencilType.MATHEMATICAL_PENDULUM -> {
                                    MetricRow("Период колебаний T = 2π√(L/g)", "${"%.2f".format(pendulumPeriod)} с", highlight = true)
                                    MetricRow("Циклическая частота ω = √(g/L)", "${"%.2f".format(if (pendulumLengthM > 0f) sqrt(pendulumGravity / pendulumLengthM) else 0f)} рад/с")
                                    MetricRow("Частота колебаний ν = 1/T", "${"%.2f".format(pendulumFreq)} Гц")
                                }
                                PhysicsStencilType.LEVER_BALANCE -> {
                                    MetricRow("Уравновешивающая масса m2 = m1·d1/d2", "${"%.2f".format(leverMass2Kg)} кг", highlight = true)
                                    MetricRow("Вращающий момент сил M = F1·d1", "${"%.2f".format(leverTorque1)} Н·м")
                                    MetricRow("Выигрыш в силе d1/d2", "${"%.2f".format(if (leverArm2Cm > 0f) leverArm1Cm / leverArm2Cm else 1f)}x")
                                }

                                PhysicsStencilType.THERMO_CYLINDER -> {
                                    MetricRow("Изобарная работа A = p·ΔV", "${"%.1f".format(thermoWorkJoules)} Дж", highlight = true)
                                    MetricRow("Начальная температура T1 = pV1/(νR)", "${temp1K.toInt()} K (${(temp1K - 273.15f).toInt()} °C)")
                                    MetricRow("Конечная температура T2 = pV2/(νR)", "${temp2K.toInt()} K (${(temp2K - 273.15f).toInt()} °C)")
                                }
                                PhysicsStencilType.THERMO_PV_CYCLE -> {
                                    MetricRow("Работа газа за полный цикл A = Δp·ΔV", "${"%.1f".format(cycleWorkJoules)} Дж", highlight = true)
                                    MetricRow("Амплитуда давления Δp = p2 - p1", "${(pvP2KPa - pvP1KPa).toInt()} кПа")
                                    MetricRow("Изменение объема ΔV = V2 - V1", "${"%.1f".format(pvV2L - pvV1L)} л")
                                }

                                PhysicsStencilType.CIRCUIT_RESISTOR, PhysicsStencilType.CIRCUIT_LAMP, PhysicsStencilType.CIRCUIT_AMMETER -> {
                                    MetricRow("Сила тока в цепи I = U / R", "${"%.2f".format(currentAmperes)} А", highlight = true)
                                    MetricRow("Тепловая мощность P = U · I", "${"%.1f".format(powerWatts)} Вт")
                                    MetricRow("Теплота за 1 минуту Q = P · 60с", "${"%.1f".format(powerWatts * 60f)} Дж")
                                }
                                PhysicsStencilType.CIRCUIT_BATTERY, PhysicsStencilType.CIRCUIT_VOLTMETER -> {
                                    MetricRow("Падение напряжения U", "${voltageVolts.toInt()} В", highlight = true)
                                }
                                PhysicsStencilType.CIRCUIT_CAPACITOR -> {
                                    val chargeMicroC = capacitanceMicroFarad * voltageVolts
                                    MetricRow("Заряд конденсатора q = C · U", "${"%.1f".format(chargeMicroC)} мкКл")
                                    MetricRow("Энергия поля W = C·U²/2", "${"%.2f".format(capEnergyMilliJoules)} мДж", highlight = true)
                                }
                                PhysicsStencilType.CIRCUIT_RHEOSTAT -> {
                                    MetricRow("Сила тока при R=${rheostatCurR.toInt()}Ом", "${"%.2f".format(rheostatCurrent)} А", highlight = true)
                                    MetricRow("Диапазон тока", "${"%.2f".format(if (rheostatMaxR > 0f) voltageVolts / rheostatMaxR else 0f)}..${"%.1f".format(voltageVolts)} А")
                                }

                                PhysicsStencilType.MAGNET_FIELD -> {
                                    MetricRow("Сила Лоренца Fл = q·v·B", "${"%.4f".format(lorentzForceN)} Н", highlight = true)
                                    MetricRow("Сила Ампера Fa = I·B·L", "${"%.3f".format(ampereForceN)} Н", highlight = true)
                                    val cyclotronRadiusMm = if (magnetFieldTesla > 0f) ((1.67e-27f * particleSpeedMps) / (1.6e-19f * magnetFieldTesla)) * 1000f else 0f
                                    MetricRow("Радиус траектории протона R", "${"%.2f".format(cyclotronRadiusMm)} мм")
                                }

                                PhysicsStencilType.OPTICAL_LENS -> {
                                    MetricRow("Оптическая сила D = 100 / F", "${"%.1f".format(lensPowerDiopters)} дптр")
                                    MetricRow("Положение изображения f = d·F/(d-F)", "${"%.1f".format(imageDistanceCm)} см", highlight = true)
                                    MetricRow("Линейное увеличение Г = |f/d|", "${"%.2f".format(magnification)}x")
                                }
                                PhysicsStencilType.OPTICAL_PRISM -> {
                                    MetricRow("Угол наим. отклонения δmin", "${"%.1f".format(devMinDeg)}°", highlight = true)
                                    MetricRow("Скорость света в призме v = c/n", "${"%.1f".format(300.0f / prismRefractiveIndex)} тыс. км/с")
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

                    // 3. Нижняя панель действий без эмодзи
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
                                val currentParams = when (selectedType) {
                                    PhysicsStencilType.INCLINED_PLANE -> mapOf("m" to massKg, "angle" to angleDeg, "mu" to frictionCoeff)
                                    PhysicsStencilType.PULLEY_BLOCK -> mapOf("m1" to pulleyMass1, "m2" to pulleyMass2)
                                    PhysicsStencilType.SPRING -> mapOf("m" to massKg, "k" to springConstant)
                                    PhysicsStencilType.MATHEMATICAL_PENDULUM -> mapOf("L" to pendulumLengthM, "g" to pendulumGravity)
                                    PhysicsStencilType.LEVER_BALANCE -> mapOf("d1" to leverArm1Cm, "d2" to leverArm2Cm, "m1" to leverMass1Kg)
                                    PhysicsStencilType.THERMO_CYLINDER -> mapOf("p" to thermoPressureKPa, "V" to thermoVol2L, "nu" to thermoMoles)
                                    PhysicsStencilType.THERMO_PV_CYCLE -> mapOf("p1" to pvP1KPa, "p2" to pvP2KPa, "V1" to pvV1L, "V2" to pvV2L)
                                    PhysicsStencilType.CIRCUIT_RESISTOR -> mapOf("R" to resistanceOhms, "U" to voltageVolts)
                                    PhysicsStencilType.CIRCUIT_BATTERY -> mapOf("U" to voltageVolts)
                                    PhysicsStencilType.CIRCUIT_CAPACITOR -> mapOf("C" to capacitanceMicroFarad, "U" to voltageVolts)
                                    PhysicsStencilType.CIRCUIT_INDUCTOR -> mapOf("L" to inductanceMilliHenry)
                                    PhysicsStencilType.CIRCUIT_SWITCH -> emptyMap()
                                    PhysicsStencilType.CIRCUIT_LAMP -> mapOf("R" to resistanceOhms, "U" to voltageVolts)
                                    PhysicsStencilType.CIRCUIT_AMMETER -> mapOf("I" to 2.4f, "Imax" to 5.0f)
                                    PhysicsStencilType.CIRCUIT_VOLTMETER -> mapOf("U" to voltageVolts, "Umax" to 24.0f)
                                    PhysicsStencilType.CIRCUIT_RHEOSTAT -> mapOf("Rcur" to rheostatCurR, "Rmax" to rheostatMaxR, "U" to voltageVolts)
                                    PhysicsStencilType.MAGNET_FIELD -> mapOf("B" to magnetFieldTesla, "q" to 10f, "v" to 1000f, "I" to 3f)
                                    PhysicsStencilType.OPTICAL_LENS -> mapOf("F" to 20f, "d" to 30f)
                                    PhysicsStencilType.OPTICAL_PRISM -> mapOf("A" to 60f, "n" to 1.50f)
                                }
                                onSelectStencil(selectedType, computedLabel, currentParams)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Добавить на доску", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
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
    onSelectStencil: (type: PhysicsStencilType, label: String, initialParams: Map<String, Float>) -> Unit
) {
    PhysicsLabSidePanel(
        isOpen = true,
        onDismiss = onDismiss,
        onSelectStencil = onSelectStencil
    )
}
