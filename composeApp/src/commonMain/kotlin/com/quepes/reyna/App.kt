package com.quepes.reyna

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Importaciones para la comunicación TCP
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFD32F2F),
            background = Color(0xFF121212)
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
    val scope = rememberCoroutineScope() // Necesario para disparar la red en segundo plano

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("Esperando objetivo...") }
    var errorMessage by remember { mutableStateOf("") }

    // Dirección IP de tu PC (la que sacamos con ipconfig)
    val serverIp = "192.168.1.71"
    val serverPort = 8080

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

                            // DISPARO TÁCTICO AL SERVIDOR C++
                            // Usamos Dispatchers.IO para no congelar la pantalla mientras enviamos datos
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val selectorManager = SelectorManager(Dispatchers.IO)
                                    val socket = aSocket(selectorManager).tcp().connect(serverIp, serverPort)

                                    val writeChannel = socket.openWriteChannel(autoFlush = true)
                                    writeChannel.writeStringUtf8(textoCapturado)

                                    // Cerramos para liberar el puerto en Windows
                                    socket.close()
                                    selectorManager.close()
                                } catch (e: Exception) {
                                    // Volvemos al hilo principal para mostrar el error en la UI
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "Error de enlace: ${e.message}"
                                    }
                                }
                            }
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