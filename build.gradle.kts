@file:Suppress("SpellCheckingInspection")

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.3.61"
  // Serialization
  kotlin("plugin.serialization") version "1.3.61"
  // Gradle Shadow plugin for building a fat jar
  id("com.github.johnrengelman.shadow") version "5.2.0"
  antlr
  application
}

group = "ic.wacc"
version = "1.0"

application {
  mainClassName = "ic.org.MainKt"
}
repositories {
  jcenter()
}

val arrowVer = "0.10.4"
val antlrVer = "4.7"
val ktorVer = "1.3.1"
dependencies {

  // Kotlin standard library
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
  implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3")

  // Kotlin functional programming library, Arrow
  implementation("io.arrow-kt:arrow-optics:$arrowVer")
  implementation("io.arrow-kt:arrow-syntax:$arrowVer")

  // Ktor http client to query the reference compiler
  testImplementation("io.ktor:ktor-client-core:$ktorVer")
  testImplementation("io.ktor:ktor-client-cio:$ktorVer")
  testImplementation("io.ktor:ktor-client-serialization-jvm:$ktorVer")
  testImplementation("io.ktor:ktor-gson:$ktorVer")
  testImplementation("io.ktor:ktor-client-gson:$ktorVer")

  val fuelVer = "2.2.1"

  testImplementation("com.github.kittinunf.fuel:fuel:$fuelVer")
  testImplementation("com.github.kittinunf.fuel:fuel-gson:$fuelVer")

  // TODO can it be removed
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")

  antlr("org.antlr:antlr4:$antlrVer")

  // JUnit5
  testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}
// Antlr compile grammar task
val antlrOut = "build/generated/source/antlr/"
tasks.generateGrammarSource {
  val generatedPackage = "antlr"
  arguments =
    arguments + listOf("-package", generatedPackage, "-visitor", "-no-listener", "-Werror")
  this.outputDirectory = file(antlrOut + generatedPackage)
}

// Add the antlr compiled grammar output to the compiled java sourcesets
sourceSets["main"].java.srcDir(antlrOut)

// Add a test-utils sourceset that will not get added to the final jar
sourceSets["test"].withConvention(KotlinSourceSet::class) {
  kotlin.srcDir("src/test-utils/kotlin")
}

tasks {
  test {
    useJUnitPlatform()
  }

  withType<KotlinCompile>().configureEach {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xinline-classes")
      jvmTarget = "1.8"
    }
    dependsOn(generateGrammarSource)
  }
}

val generateRefAss by tasks.registering {
  val waccExamples = file("./wacc_examples").walkBottomUp().toList().toTypedArray()
  val refRuby = "./refCompile -a "
  inputs.files(*waccExamples)
  fun waccToAsm(wacc: String, asm: File) = (refRuby + wacc).runCommand()!!
    .let { file("ref.out").readLines() }
    // Remove lines that are not assembly code
    .filter { it.isNotEmpty() }
    .filter { it[0] in '0'..'9' }
    // Remove the prefix added by the reference compiler
    .joinToString(separator = "\n") { it.drop(2) }
    .also { println("Wrote ass: $it") }
    .let { str -> asm.writeText(str) }
    .also { println("Wrote: $asm") }

  doLast {
    val waccs = inputs.files
      .filter { ".wacc" in it.path }
      .filterNot { "invalid" in it.path }
    waccs.map { it to file(it.path.replace(".wacc", ".asm")) }
      .filterNot { (_, asm) -> asm.exists() }
      .forEach { (wacc, asm) -> waccToAsm(wacc.readText(), asm) }
  }
}

fun String.runCommand(workingDir: File = file(".")): String? = try {
  ProcessBuilder(*split("\\s".toRegex()).toTypedArray())
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply { waitFor() }
    .inputStream.reader().readText()
} catch (e: java.io.IOException) {
  e.printStackTrace()
  null
}
