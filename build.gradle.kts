plugins {
    id("java")
    id("java-library")
}

group = "net.fndanko"
version = "1.4"

base {
    archivesName.set("xml-artisan")
}

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
}

tasks.test {
    useJUnitPlatform()
}
