plugins {
    kotlin("jvm") version "1.9.21"

    id("signing")
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.10"
}

tasks.wrapper {
    gradleVersion = "7.2"
}

dependencies {
    implementation("net.yakclient:common-util:1.0-SNAPSHOT")
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")

    group = "net.yakclient"
    version = "1.1-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            isAllowInsecureProtocol = true
            url = uri("http://maven.yakclient.net/snapshots")
        }
    }

    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }

    kotlin {
        explicitApi()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        testImplementation(kotlin("test"))
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

    publishing {
        repositories {
            if (project.hasProperty("maven-user") && project.hasProperty("maven-secret")) maven {
                logger.quiet("Maven user and password found.")
                val repo = if ((version as String).endsWith("-SNAPSHOT")) "snapshots" else "releases"

                isAllowInsecureProtocol = true

                url = uri("http://maven.yakclient.net/$repo")
                println(url)

                credentials {
                    username = project.findProperty("maven-user") as String
                    password = project.findProperty("maven-secret") as String
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            } else logger.quiet("Maven user and password not found.")
        }
    }
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
}