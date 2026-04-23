plugins {
    id("java")
    id("java-library")
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "net.fn-danko"
version = "1.6.0"

base {
    archivesName.set("xml-artisan")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("net.jqwik:jqwik:1.9.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform {
        includeEngines("jqwik", "junit-jupiter")
    }
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

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(group.toString(), "xml-artisan", version.toString())

    pom {
        name.set("XML Artisan")
        description.set("Fluent XML document manipulation in Java, with selections inspired by D3.js.")
        url.set("https://github.com/fn-danko/xml-artisan")
        inceptionYear.set("2025")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("fn-danko")
                name.set("Danko")
                url.set("https://github.com/fn-danko")
            }
        }
        scm {
            url.set("https://github.com/fn-danko/xml-artisan")
            connection.set("scm:git:git://github.com/fn-danko/xml-artisan.git")
            developerConnection.set("scm:git:ssh://git@github.com/fn-danko/xml-artisan.git")
        }
    }
}
