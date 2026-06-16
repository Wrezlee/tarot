package putra.yanuar.tarot

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrHelper {

    /**
     * Generate QR code bitmap dengan warna brand #7469B6
     * @param content  string yang di-encode
     * @param sizePx   ukuran bitmap px (default 400)
     */
    fun generateQr(content: String, sizePx: Int = 400): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) Color.parseColor("#7469B6") else Color.WHITE
                )
            }
        }
        return bitmap
    }

    /**
     * Format isi QR:
     * "TAROT-MEOW|bookingId|customerId|customerName|packageName|date|time"
     */
    fun buildQrContent(
        bookingId: String,
        customerId: String,
        customerName: String,
        packageName: String,
        date: String,
        time: String
    ): String = "TAROT-MEOW|$bookingId|$customerId|$customerName|$packageName|$date|$time"

    data class QrData(
        val bookingId: String,
        val customerId: String,
        val customerName: String,
        val packageName: String,
        val date: String,
        val time: String
    )

    /**
     * Parse hasil scan kamera. Return null jika format tidak valid.
     */
    fun parseQrContent(raw: String): QrData? {
        return try {
            val parts = raw.split("|")
            if (parts.size == 7 && parts[0] == "TAROT-MEOW") {
                QrData(
                    bookingId    = parts[1],
                    customerId   = parts[2],
                    customerName = parts[3],
                    packageName  = parts[4],
                    date         = parts[5],
                    time         = parts[6]
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}