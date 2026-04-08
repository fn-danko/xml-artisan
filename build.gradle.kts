plugins {
    id("java")
    id("java-library")
    id("maven-publish")
}

group = "net.fndanko"
version = "1.4.2"

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

tasks.register("updateReadmeVersion") {
    doLast {
        val readme = file("README.md")
        val updated = readme.readText()
            .replace(Regex("""xml-artisan:[\d.]+"""), "xml-artisan:$version")
            .replace(Regex("""<version>[\d.]+</version>"""), "<version>$version</version>")
        readme.writeText(updated)
    }
}

tasks.register("checkReadmeVersion") {
    doLast {
        val readme = file("README.md").readText()
        if ("xml-artisan:$version" !in readme) {
            error("README.md has stale version — run: ./gradlew updateReadmeVersion")
        }
    }
}

tasks.named("check") { dependsOn("checkReadmeVersion") }

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/fn-danko/xml-artisan")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
