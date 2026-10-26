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
    @Test fun adaptiveForegroundKeepsAllColoredArtworkInsideTheSafeCircle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = context.getDrawable(R.drawable.ic_launcher_foreground)!!
        val bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 1080, 1080)
        drawable.draw(Canvas(bitmap))
        var coloredPixels = 0
        for (y in 0 until 1080) for (x in 0 until 1080) {
            val pixel = bitmap.getPixel(x, y)
            val orange = Color.red(pixel) > 200 && Color.green(pixel) < 160 && Color.blue(pixel) < 80
            val blue = Color.blue(pixel) > 100 && Color.red(pixel) < 90 && Color.blue(pixel) > Color.green(pixel)
            if (Color.alpha(pixel) > 200 && (orange || blue)) {
                coloredPixels++
                val radiusSquared = (x - 540) * (x - 540) + (y - 540) * (y - 540)
                org.junit.Assert.assertTrue("Logo must fit the adaptive-icon safe circle", radiusSquared <= 330 * 330)
            }
        }
        org.junit.Assert.assertTrue("The complete artwork must remain visible", coloredPixels > 20000)
        val output = File("build/review/launcher-safe-zone.png")
        output.parentFile.mkdirs()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun suppliedLogoRetainsItsColorsAndExportsStoreAndLegacyAssets() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = context.getDrawable(R.drawable.app_icon_square)!!
        val master = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 1080, 1080)
        drawable.draw(Canvas(master))
        // The supplied artwork has an orange figure, blue book and white background.
        org.junit.Assert.assertTrue(Color.red(master.getPixel(10, 10)) > 245)
        val orange = master.getPixel(550, 650)
        org.junit.Assert.assertTrue(Color.red(orange) > 220 && Color.green(orange) < 130 && Color.blue(orange) < 50)
        val blue = master.getPixel(520, 470)
        org.junit.Assert.assertTrue(Color.blue(blue) > Color.red(blue) && Color.blue(blue) > Color.green(blue))
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
