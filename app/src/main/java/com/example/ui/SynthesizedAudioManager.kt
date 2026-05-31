package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin

object SynthesizedAudioManager {
    private var appContext: android.content.Context? = null

    fun init(context: android.content.Context) {
        this.appContext = context.applicationContext
    }

    private const val sampleRate = 22050
    private var musicJob: Job? = null
    
    private val clickSample: ShortArray = generateBeep(880.0, 0.04)
    private val cashSample: ShortArray = generateArpeggio(listOf(523.25, 659.25, 783.99, 1046.50), 0.12)
    
    var musicVolume: Float = 0.4f
    var sfxVolume: Float = 0.5f

    private fun generateBeep(frequency: Double, durationSec: Double): ShortArray {
        val numSamples = (durationSec * sampleRate).toInt()
        val sample = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = kotlin.math.exp(-22.0 * t)
            sample[i] = (sin(2.0 * Math.PI * frequency * t) * 12000.0 * envelope).toInt().toShort()
        }
        return sample
    }

    private fun generateArpeggio(freqs: List<Double>, durationSecPerNote: Double): ShortArray {
        val noteSamples = (durationSecPerNote * sampleRate).toInt()
        val totalSamples = noteSamples * freqs.size
        val sample = ShortArray(totalSamples)
        
        for (noteIdx in freqs.indices) {
            val freq = freqs[noteIdx]
            val offset = noteIdx * noteSamples
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = kotlin.math.exp(-12.0 * t)
                sample[offset + i] = (sin(2.0 * Math.PI * freq * t) * 12000.0 * envelope).toInt().toShort()
            }
        }
        return sample
    }

    private val clickLock = Any()
    private val cashLock = Any()
    private var clickTrack: AudioTrack? = null
    private var cashTrack: AudioTrack? = null

    private fun createStaticTrack(buffer: ShortArray): AudioTrack? {
        return try {
            val audioTrack = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val builder = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    appContext?.let { ctx: android.content.Context ->
                        try {
                            val audioContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                ctx.createAttributionContext("audio")
                            } else {
                                ctx
                            }
                            val getAttributionSourceMethod = android.content.Context::class.java.getMethod("getAttributionSource")
                            val attributionSource = getAttributionSourceMethod.invoke(audioContext)
                            if (attributionSource != null) {
                                val setAttributionSourceMethod = builder.javaClass.getMethod(
                                    "setAttributionSource",
                                    Class.forName("android.content.AttributionSource")
                                )
                                setAttributionSourceMethod.invoke(builder, attributionSource)
                            }
                        } catch (ignored: Throwable) {}
                    }
                }

                builder.build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )
            }
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    fun playClick() {
        synchronized(clickLock) {
            if (sfxVolume <= 0.01f) return
            try {
                var track = clickTrack
                if (track == null) {
                    track = createStaticTrack(clickSample)
                    clickTrack = track
                }
                track?.let {
                    it.setVolume(sfxVolume * 0.5f)
                    it.stop()
                    it.reloadStaticData()
                    it.play()
                }
            } catch (t: Throwable) {
                try { clickTrack?.release() } catch (ignored: Throwable) {}
                clickTrack = null
            }
        }
    }

    fun playPurchase() {
        synchronized(cashLock) {
            if (sfxVolume <= 0.01f) return
            try {
                var track = cashTrack
                if (track == null) {
                    track = createStaticTrack(cashSample)
                    cashTrack = track
                }
                track?.let {
                    it.setVolume(sfxVolume * 0.8f)
                    it.stop()
                    it.reloadStaticData()
                    it.play()
                }
            } catch (t: Throwable) {
                try { cashTrack?.release() } catch (ignored: Throwable) {}
                cashTrack = null
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun startBackgroundMusic() {
        musicJob?.cancel()
        musicJob = GlobalScope.launch(Dispatchers.Default) {
            val chords = listOf(
                listOf(261.63, 311.13, 392.00), // C Minor sweet chord
                listOf(349.23, 415.30, 523.25), // F Minor sweet chord
                listOf(293.66, 349.23, 440.00), // D minor chord
                listOf(392.00, 466.16, 587.33)  // G Minor sweet chord
            )
            var idx = 0
            while (isActive) {
                val vol = musicVolume
                if (vol > 0.01f) {
                    val currentChord = chords[idx]
                    val duration = 4.0
                    val numSamples = (duration * sampleRate).toInt()
                    val buffer = ShortArray(numSamples)
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val envelope = if (t < 0.6) t / 0.6 else if (t > 3.4) (4.0 - t) / 0.6 else 1.0
                        var valSum = 0.0
                        for (freq in currentChord) {
                            valSum += sin(2.0 * Math.PI * freq * t)
                        }
                        buffer[i] = (valSum / 3.0 * 6000.0 * envelope).toInt().toShort()
                    }
                    
                    try {
                        val audioTrack = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            val builder = AudioTrack.Builder()
                                .setAudioAttributes(
                                    AudioAttributes.Builder()
                                        .setUsage(AudioAttributes.USAGE_GAME)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .build()
                                )
                                .setAudioFormat(
                                    AudioFormat.Builder()
                                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                        .setSampleRate(sampleRate)
                                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                        .build()
                                )
                                .setBufferSizeInBytes(buffer.size * 2)
                                .setTransferMode(AudioTrack.MODE_STATIC)

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                appContext?.let { ctx: android.content.Context ->
                                    try {
                                        val audioContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                            ctx.createAttributionContext("audio")
                                        } else {
                                            ctx
                                        }
                                        val getAttributionSourceMethod = android.content.Context::class.java.getMethod("getAttributionSource")
                                        val attributionSource = getAttributionSourceMethod.invoke(audioContext)
                                        if (attributionSource != null) {
                                            val setAttributionSourceMethod = builder.javaClass.getMethod(
                                                "setAttributionSource",
                                                Class.forName("android.content.AttributionSource")
                                            )
                                            setAttributionSourceMethod.invoke(builder, attributionSource)
                                        }
                                    } catch (ignored: Throwable) {}
                                }
                            }

                            builder.build()
                        } else {
                            @Suppress("DEPRECATION")
                            AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                sampleRate,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                buffer.size * 2,
                                AudioTrack.MODE_STATIC
                            )
                        }
                        audioTrack.write(buffer, 0, buffer.size)
                        audioTrack.setVolume(vol * 0.25f)
                        audioTrack.play()
                        delay(3900)
                        try {
                            audioTrack.stop()
                        } catch (ignored: Throwable) {}
                        try {
                            audioTrack.release()
                        } catch (ignored: Throwable) {}
                    } catch (e: CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        delay(1000)
                    }
                    idx = (idx + 1) % chords.size
                } else {
                    delay(1000)
                }
            }
        }
    }

    fun stopBackgroundMusic() {
        musicJob?.cancel()
        musicJob = null
    }
}
