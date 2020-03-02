package reference

import java.io.File

object JasminAPI : ReferenceEmulatorAPI {
  override fun emulate(prog: String, filename: String, input: String): Pair<String, Int> {
    val newFile = filename.replace(".wacc", ".j")
    val classFile = filename.replace(".wacc", ".class")
    val asmFile = File(newFile).apply { writeText(prog) }
    val jOut = "jasmin $newFile -d $classFile".runCommand()
    return "java $classFile".runCommand()
  }
}

fun String.runCommand(workingDir: File = File(".")) =
  ProcessBuilder(*split("\\s".toRegex()).toTypedArray())
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .let { proc ->
      val code = proc.waitFor()
      proc.inputStream.bufferedReader().readText() to code
    }

