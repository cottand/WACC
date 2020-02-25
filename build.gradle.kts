@file:Suppress("SpellCheckingInspection")

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.3.61"
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
val fuelVer = "2.2.1"
dependencies {

  // Kotlin standard library
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
  implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3")

  // Kotlin functional programming library, Arrow
  implementation("io.arrow-kt:arrow-optics:$arrowVer")
  implementation("io.arrow-kt:arrow-syntax:$arrowVer")

  // Http client Fuel and serialisation lib Gson in order to call the ref compiler
  testImplementation("com.google.code.gson:gson:2.8.5")
  testImplementation("com.github.kittinunf.fuel:fuel:$fuelVer")
  testImplementation("com.github.kittinunf.fuel:fuel-gson:$fuelVer")

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

  clean {
    doFirst {
      delete(file("./.test_cache/"))
    }
  }

  withType<KotlinCompile>().configureEach {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xinline-classes")
      jvmTarget = "1.8"
    }
    dependsOn(generateGrammarSource)
  }
}

