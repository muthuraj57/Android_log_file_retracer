import proguard.obfuscate.MappingProcessor
import proguard.obfuscate.MappingReader
import proguard.retrace.ReTrace
import java.io.File
import java.io.PrintStream

class Processor : MappingProcessor {
    val classNameMap = mutableMapOf<String, String>()
    override fun processClassMapping(originalName: String?, obfuscatedName: String?): Boolean {
        if (obfuscatedName != null && originalName != null) {
            classNameMap[obfuscatedName] = originalName
        }
        return true
    }

    override fun processFieldMapping(p0: String?, p1: String?, p2: String?, p3: String?) {

    }

    override fun processMethodMapping(
        p0: String?,
        p1: Int,
        p2: Int,
        p3: String?,
        p4: String?,
        p5: String?,
        p6: String?
    ) {

    }
}

fun retrace(mappingFile: File, logFile: File) {
    println("retrace() called with: mappingFile = $mappingFile, logFile = $logFile")
    if (logFile.isFile) {
        //Retrace files starts with log_ and ends with .txt
        if (logFile.name.startsWith("log_") && logFile.extension == "txt") {
            retraceFile(mappingFile, logFile)
        }
        return
    }
    if (logFile.isDirectory) {
        println("logFile.listFiles() -> ${logFile.listFiles().contentToString()}")
        logFile.listFiles().forEach {
            println("calling retrace on $it")
            retrace(mappingFile, it)
        }
    }
}

fun retraceFile(mappingFile: File, logFile: File) {
    println("retracing ${logFile.path}..")
    val processor = Processor()
    MappingReader(mappingFile).pump(processor)

    //Log tag class name extraction regex.
    val classNameRegEx =
        "(\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}.\\d{3}\\s+\\d+\\s+\\d+\\s+\\w\\s+([a-z\\d\$-._]+):\\d+)".toRegex()
    val objRefRegEx = "\\[([a-z\\d\$-._]+)@[a-z\\d+]*\\]".toRegex()
    val stackTraceRegEx = "(?:\\s*%c:.*)|(?:\\s*at\\s+%c.%m\\s*\\(.*?(?::%l)?\\)\\s*)"
    val exceptionPrefix = "E AndroidRuntime:"

    val lines = logFile.readLines()
//    val outputFile = File(logFile.parentFile, logFile.name + "_deobfuscated.txt")
    logFile.printWriter().use { out ->
        var index = 0

        val exceptionLines = mutableListOf<String>()

        while (index < lines.size) {
            val line = lines[index]

            val exceptionIndex = line.indexOf(exceptionPrefix)
            if (exceptionIndex != -1) {
                exceptionLines += line.substring(exceptionIndex + exceptionPrefix.length, line.length)
                index++
                continue
            } else {
                if (exceptionLines.isNotEmpty()) {

                    //Create a new file and write the stacktrace it in.
                    val obfuscatedStackTraceFile =
                        File(logFile.parent, logFile.name + "_retrace_obfuscated_temp.txt")
                    obfuscatedStackTraceFile.printWriter().use {
                        exceptionLines.forEach { exceptionLine ->
                            it.println(exceptionLine)
                        }
                    }

                    //Create another file and set that as System.out since Retrace prints deobfuscated stacktrace in System.out.
                    val deobfuscatedStackTraceFile =
                        File(logFile.parent, logFile.name + "_retrace_deobfuscated_temp.txt")
                    deobfuscatedStackTraceFile.createNewFile()
                    val outStream = System.out
                    System.setOut(PrintStream(deobfuscatedStackTraceFile.outputStream()))

                    //Retrace the obfuscated stacktrace.
                    ReTrace(stackTraceRegEx, false, mappingFile, obfuscatedStackTraceFile).execute()

                    //Reset System.out
                    System.setOut(outStream)

                    //Write the retraced stacktrace to the original output file.
                    deobfuscatedStackTraceFile.readLines().forEach { stacktraceLine ->
                        out.println(stacktraceLine)
                    }

                    //Delete the temperory files.
                    obfuscatedStackTraceFile.delete()
                    deobfuscatedStackTraceFile.delete()
                }
                exceptionLines.clear()
            }

            //To find and replace object references with actual class name.
            //For ex: [ih2@e43bd21] to [LocalMemberWrapper@e43bd21]
            val objRefGroup = objRefRegEx.find(line)?.groups?.last()
            val objRefValue = objRefGroup
                ?.value
                ?.let { processor.classNameMap[it] }
                ?.split(".")
                ?.last()
            val updatedLine = if (objRefValue != null) {
                line.replaceRange(objRefGroup.range, objRefValue)
            } else {
                line
            }

            //To deobfuscate class name in log tags.
            val group = classNameRegEx.find(updatedLine)?.groups?.last()
            val value = group
                ?.value
                ?.let { processor.classNameMap[it] }
                ?.split(".")
                ?.last()
            if (value != null) {
                out.println(updatedLine.replaceRange(group.range, value))
            } else {
                out.println(updatedLine)
            }

            index++
        }
    }
    println("retracing completed for ${logFile.path}..")
}