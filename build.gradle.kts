import net.researchgate.release.GitAdapter
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("org.springframework.boot") version "2.1.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    kotlin("jvm") version "1.3.40"
    kotlin("plugin.spring") version "1.3.40"
    id ("net.researchgate.release") version "2.6.0"
    application
    groovy
}

group = "uk.gov.dwp.dataworks"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

tasks.bootJar {
    launchScript()
}


release {
   failOnPublishNeeded = false
    with (propertyMissing("git") as GitAdapter.GitConfig) {
        requireBranch = ""
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.apache.hbase:hbase-client:2.2.0")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    implementation("org.spockframework:spock-core:1.3-groovy-2.5")
// https://mvnrepository.com/artifact/org/junit
    implementation("junit:junit:4.12")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

application {
    mainClassName = "app.HBaseCrownExportKt"
}

tasks.getByName<BootRun>("bootRun") {
    main = "app.HBaseCrownExportKt"
    systemProperties = properties
}

sourceSets {
    create("integration") {
        java.srcDir(file("src/integration/groovy"))
        java.srcDir(file("src/integration/kotlin"))
        compileClasspath += sourceSets.getByName("main").output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
}

tasks.register<Test>("integration") {
    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
    }
}