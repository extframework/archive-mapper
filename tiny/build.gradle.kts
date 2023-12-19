repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    implementation(project(":"))
    implementation("net.yakclient:archives:1.1-SNAPSHOT") {
        isChanging = true
    }
    implementation("net.fabricmc:tiny-mappings-parser:0.3.0+build.17")

    implementation("net.yakclient:common-util:1.0-SNAPSHOT")
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
        create<MavenPublication>("archive-mapper-transform-maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            artifactId = "archive-mapper-tiny"

            pom {
                name.set("Archive Mapper Tiny mappings support")
                description.set("A mappings parser with support for tiny")
                url.set("https://github.com/yakclient/archive-mapper")

                packaging = "jar"

                withXml {
                    val repositoriesNode = asNode().appendNode("repositories")
                    val yakclientRepositoryNode = repositoriesNode.appendNode("repository")
                    yakclientRepositoryNode.appendNode("id", "yakclient")
                    yakclientRepositoryNode.appendNode("url", "http://maven.yakclient.net/snapshots")

                    val fabricRepositoryNode = repositoriesNode.appendNode("repository")
                    fabricRepositoryNode.appendNode("id", "fabric")
                    fabricRepositoryNode.appendNode("url", "https://maven.fabricmc.net/")
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