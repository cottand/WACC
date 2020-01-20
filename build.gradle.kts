@file:Suppress("SpellCheckingInspection")

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect

plugins {
  kotlin("jvm") version "1.3.61"
  //kotlin("kapt") version "1.3.61"
  antlr
  idea
}

group = "ic"
version = "1.0"

repositories {
  jcenter()
}

val arrowVer = "0.10.4"
val antlrJar = "lib/antlr-4.7-complete.jar"
dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
  implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3")
  implementation("io.arrow-kt:arrow-optics:$arrowVer")
  implementation("io.arrow-kt:arrow-syntax:$arrowVer")
  //kapt("io.arrow-kt:arrow-meta:$arrowVer")

  antlr("org.antlr:antlr4:4.7.1")

  compile(kotlin("reflect"))
  testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

val generatedPackage = "antlr"
val generatedDirectory = "build/generated/sources/antlr/"
tasks.generateGrammarSource {
  arguments = arguments + listOf("-package", generatedPackage, "-no-listener", "-Werror")
  this.outputDirectory = file(generatedDirectory + generatedPackage)
}

sourceSets {
  val main = sourceSets["main"]
  main.withConvention(KotlinSourceSet::class) {
    kotlin.srcDir(generatedDirectory)
    println(kotlin.srcDirs)
  }
  main.java.srcDir(generatedDirectory)
  println(main.java.srcDirs)
}
sourceSets["main"].java.srcDir(generatedDirectory)
sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir(generatedDirectory)
}

//idea {
//  module {
//    // Marks the already(!) added srcDir as "generated"
//    generatedSourceDirs.add(file(generatedAntlr))
//    generatedSourceDirs.add(file("build/generated-src/antlr/main"))
//  }
//}

tasks {
  test {
    useJUnitPlatform()
  }

  // config JVM target to 1.8 for kotlin compilation tasks
  withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
    dependsOn(generateGrammarSource)
  }

  compileTestKotlin {
    dependsOn(generateTestGrammarSource)
  }
}

fun String.runCommand(workingDir: File = file(".")) =
  this.split("\n").forEach {
    ProcessBuilder(*it.split(" ").toTypedArray())
      .directory(workingDir)
      .redirectOutput(Redirect.INHERIT)
      .redirectError(Redirect.INHERIT)
      .start()
      .waitFor(60, TimeUnit.SECONDS)
  }
