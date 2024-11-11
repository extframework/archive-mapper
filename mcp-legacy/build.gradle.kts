import dev.extframework.gradle.common.archives

dependencies {
    implementation(project(":"))
    implementation("org.ow2.asm:asm-commons:9.6")
    archives()

    testImplementation(project(":transform"))
}

common {
    publishing {
        publication {
            artifactId = "archive-mapper-mcp-legacy"

            commonPom {
                name.set("Archive Mapper Minecraft Coder Pack support")
                description.set("A mappings parser with support for MCP")
                url.set("https://github.com/yakclient/archive-mapper")
            }
        }
    }
}