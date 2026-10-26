package com.example.data.speech

import android.content.Context
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Pinned model weights. They never select or change Android's system TTS engine. */
class PiperVoiceFiles(context: Context) {
    private val analytics = com.example.data.analytics.AudioAnalytics.get(context)
    val directory = File(context.noBackupFilesDir,"piper-michael-v1").apply { mkdirs() }
    data class Asset(val name:String,val url:String,val sha256:String,val bytes:Long)
    companion object {
        const val VOICE = "Michael · עברית"
        val assets = listOf(
            Asset("voice.onnx","https://huggingface.co/Phonikud/phonikud-tts-checkpoints/resolve/ff64ba389a3833c49a85c58fc51a7060a6984b87/michael.onnx",
                "d2824d46ecd7ca8a206686d818d3178effe4661ce78bdb4e754eed32fe604320",63_516_050)
        )
        /** Files already hashed in this process (path, size, mtime): skipping a halacha does not re-read 64MB. */
        private val verified=java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    }
    suspend fun ensure(progress:(Int)->Unit): File = withContext(Dispatchers.IO) {
        val total = assets.sumOf { it.bytes };var completed=0L
        for(asset in assets) {
            currentCoroutineContext().ensureActive()
            val target=File(directory,asset.name)
            if(!valid(target,asset)) {
                val temp=File(directory,"${asset.name}-${java.util.UUID.randomUUID()}.download")
                val began = android.os.SystemClock.elapsedRealtime()
                fun report(stage: String) = analytics.event("audio_voice_download", mapOf("engine" to "michael", "stage" to stage, "duration_ms" to (android.os.SystemClock.elapsedRealtime()-began), "schema_version" to 1L))
                report("begin")
                try {
                    val connection=URL(asset.url).openConnection() as HttpURLConnection
                    connection.connectTimeout=20_000;connection.readTimeout=30_000
                    connection.setRequestProperty("User-Agent","Baderech-HebrewVoice/138")
                    try {
                        check(connection.responseCode in 200..299) { "לא ניתן להוריד כרגע את הקול העברי. בדקו חיבור ונסו שוב." }
                        connection.inputStream.use { input -> temp.outputStream().use { output ->
                            val buffer=ByteArray(64*1024);var read: Int;var copied=0L
                            while(input.read(buffer).also {read=it}!=-1) {
                                currentCoroutineContext().ensureActive()
                                copied+=read;check(copied<=asset.bytes) { "קובץ הקול אינו תקין." }
                                output.write(buffer,0,read);progress(((completed+copied)*100/total).toInt())
                            }
                        } }
                    } finally { connection.disconnect() }
                    currentCoroutineContext().ensureActive()
                    check(valid(temp,asset)) { "קובץ הקול אינו תקין. נסו להוריד שוב." }
                    check(temp.renameTo(target)) { "לא ניתן לשמור את הקול. בדקו מקום פנוי בטלפון." }
                    report("success")
                } catch(e: CancellationException) { report("cancelled"); throw e }
                catch(e: Exception) { report("failed"); throw e }
                finally { temp.delete() }
            }
            completed+=asset.bytes;progress((completed*100/total).toInt())
        }
        directory
    }
    fun ready(): Boolean = assets.all { valid(File(directory, it.name), it) }
    private fun valid(file:File,asset:Asset):Boolean {
        if(!file.isFile || file.length()!=asset.bytes) return false
        val stamp="${file.absolutePath}:${file.length()}:${file.lastModified()}"
        if(stamp in verified) return true
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val b=ByteArray(64*1024);var n:Int
            while(input.read(b).also {n=it}!=-1) digest.update(b,0,n) }
        return (digest.digest().joinToString("") { "%02x".format(it) }==asset.sha256).also { if(it) verified+=stamp }
    }
}
