package reference

import ic.org.util.runCommand
import java.io.File

object JasminAPI : ReferenceEmulatorAPI {
  override fun emulate(prog: String, filename: String, input: String): Pair<String, Int> {
    val newFile = filename.replace(".wacc", ".j")
    File(newFile).writeText(prog)
    val (jOut,_) = "jasmin $newFile".runCommand()
    return "java wacc".runCommand().also { (out, code) ->
      File("wacc.class").delete()
      if (code == 1)
        System.err.println("java returned exit code 1. Output:\n$out\n Jasmin output:$jOut")
    }
  }
}

