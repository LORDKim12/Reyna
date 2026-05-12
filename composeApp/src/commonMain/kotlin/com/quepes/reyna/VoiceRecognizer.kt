package com.quepes.reyna

expect class VoiceRecognizer() {
    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    )
    fun stopListening()
}