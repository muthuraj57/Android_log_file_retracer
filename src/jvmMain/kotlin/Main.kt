// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
@Preview
fun App() {
    var openMappingFileChooser by remember { mutableStateOf(false) }
    var openLogFileChooser by remember { mutableStateOf(false) }
    var mappingFile by remember { mutableStateOf<String?>(null) }
    var logFile by remember { mutableStateOf<String?>(null) }
    var isRetracing by remember { mutableStateOf(false) }

    MaterialTheme(darkColors()) {
        val scaffoldState = rememberScaffoldState()
        Scaffold(modifier = Modifier.fillMaxSize(), scaffoldState = scaffoldState) {
            Column(modifier = Modifier.padding(it).padding(16.dp)) {
                Row {
                    Button(onClick = {
                        openMappingFileChooser = true
                    }) {
                        if (mappingFile == null) {
                            Text("Choose mapping file")
                        } else {
                            Text("Change mapping file")
                        }
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        value = mappingFile?.split("/")?.last().orEmpty(),
                        onValueChange = {})
                }
                Row(modifier = Modifier.padding(top = 30.dp)) {
                    Button(onClick = {
                        openLogFileChooser = true
                    }) {
                        if (mappingFile == null) {
                            Text("Choose log file")
                        } else {
                            Text("Change log file")
                        }
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                        value = logFile?.split("/")?.last().orEmpty(),
                        onValueChange = {})
                }

                val scope = rememberCoroutineScope()
                Button(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 40.dp), onClick = {
                    scope.launch {
                        if (mappingFile == null) {
                            scaffoldState.snackbarHostState.showSnackbar("Select mapping file first.")
                            return@launch
                        }
                        if (logFile == null) {
                            scaffoldState.snackbarHostState.showSnackbar("Select log file first.")
                            return@launch
                        }

                        isRetracing = true
                        retrace(mappingFile = File(mappingFile), logFile = File(logFile))
                        logFile = null
                        isRetracing = false

                        scaffoldState.snackbarHostState.showSnackbar("Log file retraced.")
                    }
                }) {
                    Text("Retrace log file")
                }
            }

            if (isRetracing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f))) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Retracing..",
                        color = Color.Black,
                        fontSize = 30.sp
                    )
                }
            }
        }
        if (openMappingFileChooser) {
            FileDialog(title = "Select mapping file", onCloseRequest = { directory, file ->
                mappingFile = if (file != null) {
                    if (directory != null) {
                        "${directory}/${file}"
                    } else {
                        file
                    }
                } else {
                    null
                }
                println("Chosen mapping file path: $mappingFile")
                openMappingFileChooser = false
            })
        }
        if (openLogFileChooser) {
            FileDialog(title = "Select log file", onCloseRequest = { directory, file ->
                logFile = if (file != null) {
                    if (directory != null) {
                        "${directory}/${file}"
                    } else {
                        file
                    }
                } else {
                    null
                }
                println("Chosen log file path: $logFile")
                openLogFileChooser = false
            })
        }
    }
}

@Composable
private fun FileDialog(
    parent: Frame? = null,
    title: String,
    onCloseRequest: (directory: String?, file: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, title, LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(directory, file)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

fun main() = application {
    Window(title = "Log file Retracer", onCloseRequest = ::exitApplication) {
        App()
    }
}
