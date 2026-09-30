plugins {
    id("com.gradleup.shadow") version "8.3.6" apply false
}

allprojects {
    group = "org.vrz"
    version = "1.5.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    dependencies {
        add("testImplementation", platform("org.junit:junit-bom:5.10.2"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing"))
    }

    tasks.withType<Test>().configureEach {
        dependsOn(tasks.named("testClasses"))
        useJUnitPlatform()
        systemProperty("file.encoding", "UTF-8")
        val shortDesktop = "C:\\Users\\turkf\\OneDrive\\MASAST~1"
        classpath = files(classpath.files.map { file ->
            val p = file.absolutePath
            if (p.contains("Masaüstü")) File(p.replace("C:\\Users\\turkf\\OneDrive\\Masaüstü", shortDesktop)) else file
        })
        testClassesDirs = files(testClassesDirs.files.map { file ->
            val p = file.absolutePath
            if (p.contains("Masaüstü")) File(p.replace("C:\\Users\\turkf\\OneDrive\\Masaüstü", shortDesktop)) else file
        })
        filter {
            includeTestsMatching("*Test")
            excludeTestsMatching("*$*")
        }
    }
}



