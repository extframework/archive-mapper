dependencies {
    implementation(project(":"))
    implementation("net.yakclient:archives:1.2-SNAPSHOT") {
        isChanging = true
    }
    implementation("org.ow2.asm:asm-commons:9.6")

    testImplementation(project(":proguard"))
    testImplementation(project(":tiny"))
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

            artifactId = "archive-mapper-transform"

            pom {
                name.set("Archive Mapper Transform")
                description.set("An archive transformation library for applying mappings.")
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