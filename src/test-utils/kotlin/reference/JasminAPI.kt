package reference

import ic.org.util.print
import java.io.File

object JasminAPI : ReferenceEmulatorAPI {
  override fun emulate(prog: String, filename: String, input: String): Pair<String, Int> {
    val newFile = filename.replace(".wacc", ".j")
    File(newFile).writeText(prog)
    val jOut = "jasmin $newFile".runCommand()
    return "java wacc".runCommand().also { File("wacc.class").delete() }
    // TODO execute files in the right directory
  }
}

fun String.runCommand(workingDir: File = File(".")) =
  ProcessBuilder(*split("\\s".toRegex()).toTypedArray())
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .let { proc ->
      proc.waitFor()
      proc.inputStream.bufferedReader().readText() to proc.exitValue()
    }

