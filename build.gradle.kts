@file:Suppress("SpellCheckingInspection")

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val linting = false

plugins {
  kotlin("jvm") version "1.3.70"
  // Gradle Shadow plugin for building a fat jar
  id("com.github.johnrengelman.shadow") version "5.2.0"
  // id("org.jmailen.kotlinter") version "2.3.1"

  antlr
  application
}

group = "ic.wacc"
version = "1.0"

application.mainClassName = "ic.org.MainKt"
repositories {
  jcenter()
}

// kotlinter {
//   indentSize = 2
//   continuationIndentSize = 2
// }

dependencies {
  val arrowVer = "0.10.4"
  val antlrVer = "4.7"
  val fuelVer = "2.2.1"
  val junitVer = "5.6.0"

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

  implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("jasmin.jar"))))

  // JUnit5
  testImplementation("org.junit.jupiter:junit-jupiter:$junitVer")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVer")
}

// Antlr compile grammar task output directory, and add it to the java sourceSets
val antlrOut = "build/generated/source/antlr/".also {
  sourceSets["main"].java.srcDir(it)
}

// Add a test-utils sourceset that will not get added to the final jar
sourceSets["test"].withConvention(KotlinSourceSet::class) {
  kotlin.srcDir("src/test-utils/kotlin")
}

tasks {
  generateGrammarSource {
    val generatedPackage = "antlr"
    arguments =
      arguments + listOf("-package", generatedPackage, "-visitor", "-no-listener", "-Werror")
    this.outputDirectory = file(antlrOut + generatedPackage)
  }
  test {
    useJUnitPlatform()
  }

  clean {
    doFirst {
      delete(file("./.test_cache/"))
      delete(fileTree(".") {
        include("**/*.j")
        include("**/*.s")
        include("**/*.class")
      })
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


