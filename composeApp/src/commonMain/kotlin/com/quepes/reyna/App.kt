package com.projecto.reyna // Revisa que este sea tu paquete exacto

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFD32F2F), background = Color(0xFF121212))) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ReynaVoiceInterface()
        }
    }
}

@Composable
fun ReynaVoiceInterface() {
    val voiceRecognizer = remember { VoiceRecognizer() }
    val scope = rememberCoroutineScope()
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("Esperando objetivo...") }
    var errorMessage by remember { mutableStateOf("") }

    var sendJob by remember { mutableStateOf<Job?>(null) }

    val serverIp = "192.168.1.71" // Tu IP en Windows
    val serverPort = 8080

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("LA EMPERATRIZ", color = Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        Text(recognizedText, color = Color.White, fontSize = 24.sp, modifier = Modifier.fillMaxWidth().weight(1f))
        if (errorMessage.isNotEmpty()) Text("ALERTA: $errorMessage", color = Color.Yellow, modifier = Modifier.padding(bottom = 16.dp))

        Button(
            onClick = {
                if (isListening) {
                    voiceRecognizer.stopListening()
                    isListening = false
                    sendJob?.cancel() // Si apagas manual, cancelamos el envío
                    recognizedText = "Cacería abortada manualmente."
                } else {
                    errorMessage = ""
                    recognizedText = "Escuchando..."
                    isListening = true

                    voiceRecognizer.startListening(
                        onResult = { textoCapturado ->
                            recognizedText = textoCapturado

                            // 1. Cancelamos el temporizador anterior si sigues hablando
                            sendJob?.cancel()

                            // 2. Iniciamos el temporizador de silencio
                            sendJob = scope.launch(Dispatchers.IO) {
                                delay(1500) // ESPERA 1.5 SEGUNDOS DE SILENCIO TOTAL

                                // 3. ¡SILENCIO DETECTADO! APAGAMOS EL MICRÓFONO PRIMERO
                                withContext(Dispatchers.Main) {
                                    voiceRecognizer.stopListening()
                                    isListening = false
                                    recognizedText = "$textoCapturado\n[ENVIANDO...]"
                                }

                                // 4. AHORA SÍ, ATRAVESAMOS LA RED
                                try {
                                    val selectorManager = SelectorManager(Dispatchers.IO)
                                    val socket = aSocket(selectorManager).tcp().connect(serverIp, serverPort)
                                    val writeChannel = socket.openWriteChannel(autoFlush = true)

                                    writeChannel.writeStringUtf8(textoCapturado)

                                    socket.close()
                                    selectorManager.close()

                                    // Confirmación visual
                                    withContext(Dispatchers.Main) {
                                        recognizedText = "$textoCapturado\n[IMPACTO CONFIRMADO]"
                                    }
                                } catch (e: Exception) {
                                    // Si la red falla, el micrófono ya está apagado, por lo que no se congela
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "Fallo de conexión TCP: ${e.message}"
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
            colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.DarkGray else Color.Red),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text(if (isListening) "DETENER ESCUCHA" else "INICIAR CACERÍA", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}