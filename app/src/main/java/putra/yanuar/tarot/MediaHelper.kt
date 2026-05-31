package putra.yanuar.tarot

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageView
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

class MediaHelper(val context: Context) {

    fun getRcGallery(): Int = REQ_CODE_GALLERY

    fun bitmapToString(bmp: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    fun getBitmapToString(uri: Uri, imv: ImageView): String {
        var bmp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val dim = 720
        bmp = if (bmp.height > bmp.width) {
            bmp.scale((bmp.width * dim) / bmp.height, dim)
        } else {
            bmp.scale(dim, (bmp.height * dim) / bmp.width)
        }
        imv.setImageBitmap(bmp)
        return bitmapToString(bmp)
    }

    companion object {
        const val REQ_CODE_GALLERY = 100
    }
}