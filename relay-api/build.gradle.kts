plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    testImplementation("org.jetbrains:annotations:24.1.0")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    val options = options as StandardJavadocDocletOptions
    options.encoding = "UTF-8"
    options.charSet = "UTF-8"
    options.addBooleanOption("html5", true)
    options.addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "relay-api"
            version = project.version.toString()
        }
    }
}
