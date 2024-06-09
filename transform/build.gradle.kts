import dev.extframework.gradle.common.archives

dependencies {
    implementation(project(":"))
    archives()

    implementation("org.ow2.asm:asm-commons:9.6")

    testImplementation(project(":proguard"))
    testImplementation(project(":tiny"))
}

common {
    publishing {
        publication {
            artifactId = "archive-mapper-transform"

            commonPom {
                name.set("Archive Mapper Transform")
                description.set("An archive transformation library for applying mappings.")
                url.set("https://github.com/yakclient/archive-mapper")
            }
        }
    }
}