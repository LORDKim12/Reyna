package com.quepes.reyna

import androidx.compose.foundation.layout.*
import androidx.compose.material3.* import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Eliminamos la importación problemática de Preview

@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFD32F2F), // Rojo sangre
            background = Color(0xFF121212) // Fondo oscuro
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ReynaVoiceInterface()
        }
    }
}

@Composable
fun ReynaVoiceInterface() {
    val voiceRecognizer = remember { VoiceRecognizer() }

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("Esperando objetivo...") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LA EMPERATRIZ",
            color = Color.Red,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = recognizedText,
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = "ALERTA: $errorMessage",
                color = Color.Yellow,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (isListening) {
                    voiceRecognizer.stopListening()
                    isListening = false
                    recognizedText = "Cacería pausada."
                } else {
                    errorMessage = ""
                    recognizedText = "Escuchando el entorno..."
                    isListening = true

                    voiceRecognizer.startListening(
                        onResult = { textoCapturado ->
                            recognizedText = textoCapturado
                        },
                        onError = { error ->
                            errorMessage = error
                            isListening = false
                        }
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color.DarkGray else Color.Red,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text(
                text = if (isListening) "DETENER ESCUCHA" else "INICIAR CACERÍA",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}