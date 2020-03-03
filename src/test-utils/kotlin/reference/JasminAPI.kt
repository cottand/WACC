package reference

import ic.org.util.runCommand
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

