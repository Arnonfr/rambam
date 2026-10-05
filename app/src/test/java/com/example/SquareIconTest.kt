package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SquareIconTest {
    @Test fun vectorHasTheExactHomeColorsAndExportsStoreAndLegacyAssets() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = context.getDrawable(R.drawable.app_icon_square)!!
        val master = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 1080, 1080)
        drawable.draw(Canvas(master))
        assertEquals(Color.parseColor("#FF873F"), master.getPixel(380, 390))
        assertEquals(Color.parseColor("#F4F66A"), master.getPixel(700, 390))
        assertEquals(Color.parseColor("#F0A0DD"), master.getPixel(380, 670))
        assertEquals(Color.parseColor("#9FDEEF"), master.getPixel(700, 670))
        assertEquals(Color.parseColor("#FFFCF6"), master.getPixel(10, 10))
        // Opt-in asset export; ordinary test runs never rewrite source assets.
        val root = System.getenv("SQUARE_ICON_EXPORT_ROOT") ?: return
        fun export(path: String, size: Int) {
            val file = File(root, path)
            file.parentFile.mkdirs()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        export("artwork/app-icon-square.png", 1024)
        export("artwork/play-store-icon-512.png", 512)
        listOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192).forEach { (density, size) ->
            for (name in listOf("ic_launcher", "ic_launcher_round")) {
                export("app/src/main/res/mipmap-$density/$name.png", size)
            }
        }
    }
}
