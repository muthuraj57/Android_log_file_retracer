// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
@Preview
fun App() {
    var openMappingFileChooser by remember { mutableStateOf(false) }
    var openLogFileChooser by remember { mutableStateOf(LogFileChooser.None) }
    var mappingFile by remember { mutableStateOf<String?>(null) }
    var logFile by remember { mutableStateOf<String?>(null) }
    var isRetracing by remember { mutableStateOf(false) }

    MaterialTheme(darkColors()) {
        val scaffoldState = rememberScaffoldState()
        Scaffold(modifier = Modifier.fillMaxSize(), scaffoldState = scaffoldState) {
            if (isRetracing) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(it)
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.align(Alignment.Center)) {
                        Text(
                            text = "Retracing..",
                            fontSize = 30.sp
                        )
                        CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
                    }
                }
            } else {
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
                    if (logFile == null) {
                        Row(
                            modifier = Modifier.padding(top = 30.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = {
                                openLogFileChooser = LogFileChooser.ChooseFile
                            }) {
                                Text("Choose log File")
                            }
                            Button(onClick = {
                                openLogFileChooser = LogFileChooser.ChooseFolder
                            }) {
                                Text("Choose log Folder")
                            }
                        }
                    } else {
                        Row(modifier = Modifier.padding(top = 30.dp)) {
                            Button(onClick = {
                                logFile = null
                            }) {
                                Text("Clear log file selection")
                            }
                            val file = File(logFile)
                            val value = if (file.isDirectory) {
                                file.path
                            } else {
                                file.path.split("/").last()
                            }
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                value = value,
                                onValueChange = {})
                        }
                    }

                    val scope = rememberCoroutineScope()
                    Button(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 40.dp), onClick = {
                        scope.launch(Dispatchers.IO) {
                            if (mappingFile == null) {
                                scaffoldState.snackbarHostState.showSnackbar("Select mapping file first.")
                                return@launch
                            }
                            if (logFile == null) {
                                scaffoldState.snackbarHostState.showSnackbar("Select log file first.")
                                return@launch
                            }

                            isRetracing = true
                            val isDirectory = File(logFile).isDirectory
                            retrace(mappingFile = File(mappingFile), logFile = File(logFile))
                            println("from compose: retrace completed")
                            logFile = null
                            isRetracing = false

                            if (isDirectory) {
                                scaffoldState.snackbarHostState.showSnackbar("Log files retraced.")
                            } else {
                                scaffoldState.snackbarHostState.showSnackbar("Log file retraced.")
                            }
                        }
                    }) {
                        Text("Retrace log file")
                    }
                }
            }
        }
        if (openMappingFileChooser) {
            FileDialog(title = "Select mapping file", selectDirectory = false, onCloseRequest = { file ->
                mappingFile = file
                println("Chosen mapping file path: $mappingFile")
                openMappingFileChooser = false
            })
        }
        if (openLogFileChooser != LogFileChooser.None) {
            FileDialog(
                title = if (openLogFileChooser == LogFileChooser.ChooseFolder) "Select log folder" else "Select log file",
                selectDirectory = openLogFileChooser == LogFileChooser.ChooseFolder,
                onCloseRequest = { file ->
                    logFile = file
                    println("Chosen log file path: $logFile")
                    openLogFileChooser = LogFileChooser.None
                })
        }
    }
}

@Composable
private fun FileDialog(
    parent: Frame? = null,
    title: String,
    selectDirectory: Boolean,
    onCloseRequest: (file: String?) -> Unit
) = AwtWindow(
    create = {
        if (selectDirectory) {
            System.setProperty("apple.awt.fileDialogForDirectories", "true")
        }
        object : FileDialog(parent, title, LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    val filePath = if (file != null) {
                        if (directory != null) {
                            "${directory}/${file}"
                        } else {
                            file
                        }
                    } else {
                        null
                    }
                    if (selectDirectory) {
                        System.setProperty("apple.awt.fileDialogForDirectories", "false")
                    }
                    onCloseRequest(filePath)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

private enum class LogFileChooser {
    None, ChooseFile, ChooseFolder
}

fun main() = application {
    Window(title = "Log file Retracer", onCloseRequest = ::exitApplication) {
        App()
    }
}
