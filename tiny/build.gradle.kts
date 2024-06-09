import dev.extframework.gradle.common.archives

dependencies {
    implementation(project(":"))
    archives()
    implementation("net.fabricmc:mapping-io:0.5.0")
}

common {
    publishing {
        publication {
            artifactId = "archive-mapper-tiny"

            commonPom {
                name.set("Archive Mapper Tiny mappings support")
                description.set("A mappings parser with support for tiny")
                url.set("https://github.com/yakclient/archive-mapper")
            }
        }
    }
}