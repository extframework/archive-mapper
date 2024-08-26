import dev.extframework.gradle.common.commonUtil
import dev.extframework.gradle.common.extFramework

plugins {
    kotlin("jvm") version "1.9.21"

    id("dev.extframework.common") version "1.0.5"
}

tasks.wrapper {
    gradleVersion = "7.2"
}

dependencies {
    implementation("org.ow2.asm:asm-commons:9.6")

    testImplementation(project(":tiny"))
    testImplementation(project(":proguard"))

}

common {
    publishing {
        publication {
            artifactId = "archive-mapper"

            commonPom {
                name.set("Archive Mapper")
                description.set("A mapping parser for de-obfuscation mappings(proguard)")
                url.set("https://github.com/yakclient/archive-mapper")
            }
        }
    }
}

allprojects {
    apply(plugin = "dev.extframework.common")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "dev.extframework"
    version = "1.3-SNAPSHOT"

    repositories {
        mavenCentral()
        extFramework()
    }

    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }

    kotlin {
        explicitApi()
    }

    dependencies {
        commonUtil()

        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        testImplementation(kotlin("test"))
    }

    common {
        defaultJavaSettings()

        publishing {
            publication {
                publication {
                    withJava()
                    withSources()
                    withDokka()

                    commonPom {
                        packaging = "jar"

                        withExtFrameworkRepo()

                        defaultDevelopers()
                        gnuLicense()
                        extFrameworkScm("archive-mapper")
                    }
                }
            }
            repositories {
                extFramework(credentials = propertyCredentialProvider)
            }
        }
    }
}