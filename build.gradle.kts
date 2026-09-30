// For more info: https://docs.gradle.org/9.4.0/userguide/building_java_projects.html

plugins {
    application
    id("com.gradleup.shadow") version "9.6.1" // need this for generating .jar
}

repositories {
    mavenCentral()
}

application {
    mainClass = "va.rembot.Bot"
    version = "0.0.0-PRERELEASE"
}

dependencies {
    implementation("net.dv8tion:JDA:6.7.0") {
        //audio stuff
        exclude(module = "opus-java")
        exclude(module = "tink")
    }

    implementation("com.mysql:mysql-connector-j:26.7.0")
    implementation("com.zaxxer:HikariCP:7.0.2") // Connection pooling = performance

    implementation("ch.qos.logback:logback-classic:1.6.4") // (https://jda.wiki/setup/logging/)

    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
    testCompileOnly("org.projectlombok:lombok:1.18.48")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.48")

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.guava)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
