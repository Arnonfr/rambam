package com.example.data.speech

import ai.onnxruntime.*
import android.content.Context
import com.example.domain.speech.DailySpeechText
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

interface HebrewSpeechSynthesizer {
    suspend fun initialize(): String
    suspend fun synthesize(text: String, file: File)
    fun close()
}

/** The only production narrator. No system TTS construction or fallback exists. */
class PiperHebrewSpeech(private val context: Context) : HebrewSpeechSynthesizer {
    private val closed=AtomicBoolean(false)
    private val lock=Any()
    private var voice:OrtSession?=null
    private lateinit var model:File
    private lateinit var pronunciation:MichaelPronunciation
    private lateinit var environment:OrtEnvironment
    private lateinit var config:JSONObject
    override suspend fun initialize():String {
        val dir=PiperVoiceFiles(context).ensure { percent ->
            DailySpeechStatus.mutable.value=DailySpeechStatus.mutable.value.copy(voiceStatus=
                if(percent<100) "מוריד את הקול העברי · $percent%" else "מכין את הקול העברי…")
        }
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                check(!closed.get())
                model=File(dir,"voice.onnx")
                config=JSONObject(context.assets.open("speech/michael.config.json").bufferedReader().use {it.readText()})
                check(config.getString("phoneme_type")=="raw")
                pronunciation=MichaelPronunciation(context)
            }
        }
        DailySpeechStatus.mutable.value=DailySpeechStatus.mutable.value.copy(voiceStatus=PiperVoiceFiles.VOICE)
        return "michael-prepared-g2p-v3-d2824d46"
    }
    /** Loaded on first synthesis only: resuming or skipping to an already prepared halacha never waits for the model. */
    private fun voice():OrtSession = voice ?: run {
        environment=OrtEnvironment.getEnvironment().also { it.setTelemetry(false) }
        OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2,4));options.setInterOpNumThreads(1)
            // ARM emulators can advertise SME while trapping RDSVL. Keep physical-device kernels unchanged.
            if (android.os.Build.HARDWARE in setOf("ranchu", "goldfish"))
                options.addConfigEntry("mlas.disable_kleidiai", "1")
            environment.createSession(model.absolutePath,options)
        }
    }.also { voice=it }
    override suspend fun synthesize(text:String,file:File) = withContext(Dispatchers.IO) {
        val job=coroutineContext.job
        val sampleRate=config.getJSONObject("audio").getInt("sample_rate")
        val sentences=synchronized(lock) {
            check(!closed.get())
            sentences(pronunciation.phonemes(speechWords(DailySpeechText.speak(text))))
        }
        check(sentences.isNotEmpty()) { "אין טקסט מתאים להקראה." }
        val audio=ArrayList<FloatArray>()
        audio+=FloatArray((LEAD*sampleRate).toInt())
        sentences.forEachIndexed { index, sentence ->
            job.ensureActive()
            // One sentence per inference, as Piper does: long inputs drift in rhythm and the model's own pauses vary widely.
            audio+=trim(synchronized(lock) { check(!closed.get());infer(sentence) })
            audio+=FloatArray((pauseAfter(sentence,index==sentences.lastIndex)*sampleRate).toInt())
        }
        job.ensureActive()
        check(!closed.get())
        writeWave(file,FloatArray(audio.sumOf { it.size }).also { out -> var at=0
            audio.forEach { it.copyInto(out,at);at+=it.size } },sampleRate)
    }
    private fun infer(ipa:String):FloatArray {
        val map=config.getJSONObject("phoneme_id_map")
        val ids=mutableListOf<Long>()
        fun append(symbol:String) { val a=map.optJSONArray(symbol) ?: error("סימן הגייה לא נתמך: $symbol")
            for(i in 0 until a.length()) ids+=a.getLong(i) }
        append("^");append("_")
        ipa.forEach { c -> append(c.toString());append("_") }
        append("$")
        val session=voice()
        return OnnxTensor.createTensor(environment,LongBuffer.wrap(ids.toLongArray()),longArrayOf(1,ids.size.toLong())).use { input ->
            OnnxTensor.createTensor(environment,LongBuffer.wrap(longArrayOf(ids.size.toLong())),longArrayOf(1)).use { lengths ->
                OnnxTensor.createTensor(environment,FloatBuffer.wrap(floatArrayOf(.667f,1f,.8f)),longArrayOf(3)).use { scales ->
                    session.run(mapOf("input" to input,"input_lengths" to lengths,"scales" to scales)).use { result ->
                        val buffer=(result[0] as OnnxTensor).floatBuffer
                        FloatArray(buffer.remaining()).also { buffer.get(it) }
                    }
                }
            }
        }
    }
    override fun close() {
        if(!closed.compareAndSet(false,true)) return
        CoroutineScope(Dispatchers.IO).launch { synchronized(lock) {
            voice?.close();voice=null
        } }
    }
    companion object {
        internal fun speechWords(source:String):String = Regex("\\d+").replace(source) { number ->
            val n=number.value.toIntOrNull()
            val units=listOf("אפס","אחד","שניים","שלושה","ארבעה","חמישה","שישה","שבעה","שמונה","תשעה","עשרה")
            when {
                n==null -> number.value.toCharArray().joinToString(" ") { units[it.digitToInt()] }
                n<=10 -> units[n]
                n<20 -> units[n-10]+" עשר"
                n<100 -> listOf("","","עשרים","שלושים","ארבעים","חמישים","שישים","שבעים","שמונים","תשעים")[n/10]+if(n%10==0) "" else " ו"+units[n%10]
                else -> number.value.toCharArray().joinToString(" ") {units[it.digitToInt()]}
            }
        }
        private const val LEAD=.08f
        /** Even pauses: sentence ends, semicolons, and a longer breath before the next halacha. */
        internal fun pauseAfter(sentence:String,last:Boolean):Float = when {
            last -> .7f
            sentence.endsWith(';') -> .32f
            sentence.last() in ".:?!" -> .45f
            else -> .25f
        }
        /** Splits phonemes at sentence punctuation; fragments such as "jˈad haʃˈem." join the next sentence. */
        internal fun sentences(ipa:String):List<String> {
            val result=mutableListOf<String>();var carry=""
            ipa.split(Regex("(?<=[.:;?!])\\s+")).map(String::trim).filter(String::isNotEmpty).forEach { part ->
                val sentence=if(carry.isEmpty()) part else "$carry $part"
                if(sentence.length<18) carry=sentence else {result+=sentence;carry=""}
            }
            if(carry.isNotEmpty()) { if(result.isEmpty()) result+=carry else result[result.lastIndex]+=" $carry" }
            return result
        }
        /** Drops the model's own leading/trailing silence so that only [pauseAfter] decides the gaps. */
        internal fun trim(samples:FloatArray):FloatArray {
            val first=samples.indexOfFirst { abs(it)>.012f }
            if(first<0) return FloatArray(0)
            val last=samples.indexOfLast { abs(it)>.012f }
            return samples.copyOfRange((first-220).coerceAtLeast(0),(last+880).coerceAtMost(samples.size))
        }
        /** Fixed gain, not per-file peak normalisation, so loudness does not jump between halachot. */
        internal fun writeWave(file:File,samples:FloatArray,sampleRate:Int) {
            require(samples.isNotEmpty() && samples.all {it.isFinite()})
            val bytes=ByteBuffer.allocate(44+samples.size*2).order(ByteOrder.LITTLE_ENDIAN)
            bytes.put("RIFF".toByteArray()).putInt(36+samples.size*2).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(sampleRate).putInt(sampleRate*2).putShort(2).putShort(16)
                .put("data".toByteArray()).putInt(samples.size*2)
            samples.forEach {bytes.putShort((it*31128).toInt().coerceIn(-32768,32767).toShort())}
            file.writeBytes(bytes.array())
        }
    }
}
