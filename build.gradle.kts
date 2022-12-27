plugins {
    kotlin("jvm") version "1.6.21"

    id("signing")
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.6.21"
}

group = "net.yakclient"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://repo.yakclient.net/snapshots")
    }
}

tasks.wrapper {
    gradleVersion = "7.2"
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    implementation("net.yakclient:common-util:1.0-SNAPSHOT")
}

tasks.compileKotlin {
    destinationDirectory.set(tasks.compileJava.get().destinationDirectory.asFile.get())


    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    targetCompatibility = "17"
    sourceCompatibility = "17"
}

task<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

task<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}

publishing {
    publications {
        create<MavenPublication>("archive-mapper-maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            artifactId = "archive-mapper"

            pom {
                name.set("Archive Mapper")
                description.set("A mapping parser for de-obfuscation mappings(proguard)")
                url.set("https://github.com/yakclient/archive-mapper")

                packaging = "jar"

                withXml {
                    val repositoriesNode = asNode().appendNode("repositories")
                    val yakclientRepositoryNode = repositoriesNode.appendNode("repository")
                    yakclientRepositoryNode.appendNode("id", "yakclient")
                    yakclientRepositoryNode.appendNode("url", "http://maven.yakclient.net/snapshots")
                }

                developers {
                    developer {
                        id.set("Chestly")
                        name.set("Durgan McBroom")
                    }
                }

                licenses {
                    license {
                        name.set("GNU General Public License")
                        url.set("https://opensource.org/licenses/gpl-license")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/yakclient/archive-mapper")
                    developerConnection.set("scm:git:ssh://github.com:yakclient/archive-mapper.git")
                    url.set("https://github.com/yakclient/archive-mapper")
                }
            }
        }
    }

    repositories {
        if (!project.hasProperty("maven-user") || !project.hasProperty("maven-pass")) return@repositories

        maven {
            val repo = if (project.findProperty("isSnapshot") == "true") "snapshots" else "releases"

            isAllowInsecureProtocol = true

            url = uri("http://repo.yakclient.net/$repo")

            credentials {
                username = project.findProperty("maven-user") as String
                password = project.findProperty("maven-pass") as String
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}