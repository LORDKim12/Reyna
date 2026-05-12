package com.quepes.reyna

actual class VoiceRecognizer actual constructor() {

    actual fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Por ahora, si ejecutamos en Android, Reyna simplemente reportará que no está lista.
        onError("Sistemas auditivos de Android en espera. Arquitectura actual enfocada en iOS.")
    }

    actual fun stopListening() {
        // Sin acción requerida por ahora en Android
    }
}