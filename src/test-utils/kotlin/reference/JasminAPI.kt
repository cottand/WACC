package reference

import ic.org.JVM
import ic.org.util.runCommand
import java.io.File

object JasminAPI : ReferenceEmulatorAPI {
  override fun emulate(prog: String, filePath: String, input: String): Pair<String, Int> {
    val newFile = File(filePath.replace(".wacc", ".j")).apply { writeText(prog) }
    val folder = newFile.parentFile
    val (jOut,_) = "java -jar lib/jasmin.jar ${newFile.path}".runCommand()
    return "java -classpath .:${JVM.classpath.dropLast(1)}:${folder.path} wacc ${JVM.classes}".runCommand().also { (out, code) ->
      File("wacc.class").delete()
      if (code == 1)
        System.err.println("java returned exit code 1. Output:\n$out\n Jasmin output:$jOut")
    }
  }
}

