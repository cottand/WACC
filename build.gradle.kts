import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.3.61"
  kotlin("kapt") version "1.3.61"
}

group = "org.ic"
version = "1.0"

repositories {
  jcenter()
}

val arrowVer = "0.10.4"
dependencies {
  implementation(kotlin("stdlib-jdk8"))

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
  implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3")
  implementation("io.arrow-kt:arrow-optics:$arrowVer")
  implementation("io.arrow-kt:arrow-syntax:$arrowVer")
  kapt("io.arrow-kt:arrow-meta:$arrowVer")

  testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

tasks {
  test {
    useJUnitPlatform()
  }
  // config JVM target to 1.8 for kotlin compilation tasks
  withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
  }
}