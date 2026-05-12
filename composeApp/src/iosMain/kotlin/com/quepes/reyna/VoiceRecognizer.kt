package com.quepes.reyna

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Foundation.NSLocale

actual class VoiceRecognizer actual constructor() {

    // Inicializamos el motor de reconocimiento en español (México)
    private val speechRecognizer = SFSpeechRecognizer(NSLocale("es-MX"))
    private val audioEngine = AVAudioEngine()
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: platform.Speech.SFSpeechRecognitionTask? = null

    @OptIn(ExperimentalForeignApi::class)
    actual fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Purgamos cualquier proceso de escucha anterior que se haya quedado colgado
        recognitionTask?.cancel()
        recognitionTask = null

        // Secuestramos la sesión de audio del iPhone para grabar
        val audioSession = AVAudioSession.sharedInstance()
        try {
            audioSession.setCategory(AVAudioSessionCategoryRecord, null)
            audioSession.setMode(AVAudioSessionModeMeasurement, null)
            audioSession.setActive(true, null)
        } catch (e: Exception) {
            onError("Falla crítica en el sistema de audio: ${e.message}")
            return
        }

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        val request = recognitionRequest ?: return

        // Queremos que nos devuelva las palabras en tiempo real, no hasta que terminemos de hablar
        request.shouldReportPartialResults = true

        val inputNode = audioEngine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)

        // Interceptamos el flujo de audio del hardware
        inputNode.installTapOnBus(0u, 1024u, recordingFormat) { buffer, _ ->
            if (buffer != null) {
                request.appendAudioPCMBuffer(buffer)
            }
        }

        audioEngine.prepare()
        try {
            audioEngine.startAndReturnError(null)
        } catch (e: Exception) {
            onError("Motor de audio bloqueado: ${e.message}")
            return
        }

        // Iniciamos la cacería de palabras
        recognitionTask = speechRecognizer?.recognitionTaskWithRequest(request) { result, error ->
            if (error != null) {
                stopListening()
                onError("Reconocimiento colapsado: ${error.localizedDescription}")
            }
            if (result != null) {
                // Pasamos el texto capturado a nuestra interfaz en commonMain
                onResult(result.bestTranscription.formattedString)
                if (result.isFinal()) {
                    stopListening()
                }
            }
        }
    }

    actual fun stopListening() {
        audioEngine.stop()
        audioEngine.inputNode.removeTapOnBus(0u)
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        recognitionRequest = null
        recognitionTask = null
    }
}

