// For more info: https://docs.gradle.org/9.4.0/userguide/building_java_projects.html

plugins {
    application
    id("com.gradleup.shadow") version "9.4.1" // need this for generating .jar
}

repositories {
    mavenCentral()
}

application {
    mainClass = "va.rembot.Bot"
    version = "0.0.0-PRERELEASE"
}

dependencies {
    implementation("net.dv8tion:JDA:6.4.1") {
        exclude(module = "opus-java") // for audio stuff dont need this
        exclude(module = "tink") // for encrypting and decrypting audio, dont need this
    }

    implementation("com.mysql:mysql-connector-j:9.6.0")//mysql database jdbc
    implementation("com.zaxxer:HikariCP:7.0.2")//for connection pooling = performance

    implementation("ch.qos.logback:logback-classic:1.5.32") // needed for logging (https://jda.wiki/setup/logging/)

    compileOnly("org.projectlombok:lombok:1.18.44") // lombok = less boilerplate code
    annotationProcessor("org.projectlombok:lombok:1.18.44") // lombok = less boilerplate code
    testCompileOnly("org.projectlombok:lombok:1.18.44") // lombok = less boilerplate code
    testAnnotationProcessor("org.projectlombok:lombok:1.18.44") // lombok = less boilerplate code

    testImplementation(libs.junit.jupiter) //testing bs
    testRuntimeOnly("org.junit.platform:junit-platform-launcher") //testing bs

    implementation(libs.guava) // no clue why we need this but gradle included it
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
