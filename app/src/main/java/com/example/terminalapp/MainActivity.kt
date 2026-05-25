package com.example.terminalapp

import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            // Права получены
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Shizuku.addRequestPermissionResultListener(permissionListener)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TerminalScreen(
                        onRequestShizukuPermission = { requestShizukuPermission() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }

    private fun requestShizukuPermission() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(101)
            }
        }
    }
}

@Composable
fun TerminalScreen(onRequestShizukuPermission: () -> Unit) {
    var commandInput by remember { mutableStateOf("") }
    var terminalOutput by remember { mutableStateOf("Здесь будет вывод команд...") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = commandInput,
            onValueChange = { commandInput = it },
            label = { Text("Введите команду (например: pm list packages)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    val cmd = commandInput
                    if (cmd.isNotBlank()) {
                        scope.launch {
                            terminalOutput = "Выполнение..."
                            terminalOutput = SmartTerminal.execute(cmd)
                        }
                    }
                }
            ) {
                Text("Выполнить")
            }

            Button(onClick = onRequestShizukuPermission) {
                Text("Запросить Shizuku")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Статус среды:\n" +
                   "• Root: ${if (SmartTerminal.isRootAvailable()) "ДОСТУПЕН ✅" else "Нет ❌"}\n" +
                   "• Shizuku: ${if (Shizuku.pingBinder()) "ЗАПУЩЕНА 🟢" else "Не запущена 🔴"}\n" +
                   "• Права Shizuku: ${if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) "ЕСТЬ 🛡️" else "НЕТ 🔓"}\n" +
                   "-----------------------------------\n\n" + terminalOutput,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        )
    }
}

object SmartTerminal {

    fun isRootAvailable(): Boolean {
        return try {
