package com.lkboard.app.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

/**
 * Высокоскоростной генератор матриц QR-кодов для мгновенного подключения планшетов со смартфонов.
 */
object QrCodeGenerator {

    fun generateQrMatrix(content: String): Array<BooleanArray> {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val w = bitMatrix.width
        val h = bitMatrix.height
        return Array(h) { y ->
            BooleanArray(w) { x -> bitMatrix.get(x, y) }
        }
    }
}
