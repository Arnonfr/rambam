package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.speech.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Explicit opt-in: real model inference, not a mocked Android system voice. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class PiperModelIntegrationTest {
    @Test fun realHebrewWeightsProducePointedAndUnpointedRambamAudio() = runBlocking {
        val modelPath=System.getenv("PIPER_TEST_MODELS")
        assumeTrue("Set PIPER_TEST_MODELS to the verified local weights",modelPath!=null)
        val context=ApplicationProvider.getApplicationContext<Context>()
        val files=PiperVoiceFiles(context)
        PiperVoiceFiles.assets.forEach { File(modelPath!!,it.name).copyTo(File(files.directory,it.name),overwrite=true) }
        val engine=PiperHebrewSpeech(context)
        try {
            val start=System.nanoTime()
            assertTrue(engine.initialize().startsWith("michael-prepared"))
            val output=File(System.getenv("PIPER_TEST_OUTPUT") ?: "build/review/piper-audio").apply {mkdirs()}
            val comparison=System.getenv("PIPER_COMPARE_TEXT_FILE")?.let { File(it).readText() }
            val samples=if(comparison!=null) listOf("current-app" to comparison) else listOf(
                "pointed" to checkNotNull(javaClass.getResource("/michael-reference-text.txt")).readText(),
                "unpointed" to "הלכות איסורי ביאה, פרק שלושה. קטן בן תשע שנים ויום אחד, הרי זה פטור.")
            samples.forEach { (name,text) ->
                val file=File(output,"piper-hebrew-$name.wav")
                engine.synthesize(text,file)
                val bytes=file.readBytes()
                assertEquals("RIFF",String(bytes.copyOfRange(0,4)))
                assertEquals("WAVE",String(bytes.copyOfRange(8,12)))
                assertTrue("Neural model must produce actual audio",bytes.size>22050*2)
                assertTrue(bytes.drop(44).any {it.toInt()!=0})
                println("Piper sample $name: ${bytes.size} bytes, ${"%.2f".format((bytes.size-44)/44100.0)} seconds")
            }
            println("Real Piper integration elapsed seconds: ${(System.nanoTime()-start)/1e9}")
        } finally {engine.close()}
    }
}
