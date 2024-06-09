dependencies {
    implementation(project(":"))
    implementation("org.ow2.asm:asm-commons:9.6")
}

common {
    publishing {
        publication {
            artifactId = "archive-mapper-proguard"

            commonPom {
                name.set("Archive Mapper Proguard mappings support")
                description.set("A mappings parser with support for Proguard")
                url.set("https://github.com/yakclient/archive-mapper")
            }
        }
    }
}