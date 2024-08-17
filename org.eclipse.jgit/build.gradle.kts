plugins {
    kotlin("multiplatform")
    `maven-publish`
}

apply(plugin = "kotlinx-atomicfu")

configure<PublishingExtension> {
    repositories {
        maven {
            name = "GitHubPackages"
            url  = uri("https://maven.pkg.github.com/gitliveapp/packages")
            credentials {
                username = project.findProperty("gpr.user") as String
                password = project.findProperty("gpr.key") as String
            }
        }
    }
}

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
        }
    }
    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("src")
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-io-core:0.3.5")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation("junit:junit:4.12")
            }
        }
        val jsMain by getting
        val jsTest by getting
    }
}