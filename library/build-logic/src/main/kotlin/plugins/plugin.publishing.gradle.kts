import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.gradle.api.publish.PublishingExtension

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    coordinates("dev.toastbits", "spms", "1.1.0")

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    configure(KotlinMultiplatform(
        sourcesJar = true
    ))

    pom {
        name.set("spmp-server")
        description.set("SpMs (short for spmp-server) is the desktop server component for SpMp")
        url.set("https://github.com/toasterofbread/spmp-server")
        inceptionYear.set("2023")

        licenses {
            license {
                name.set("GPL-3.0")
                url.set("https://www.gnu.org/licenses/gpl-3.0.html")
            }
        }
        developers {
            developer {
                id.set("toasterofbread")
                name.set("Talo Halton")
                email.set("talohalton@gmail.com")
                url.set("https://github.com/toasterofbread")
            }
        }
        scm {
            connection.set("https://github.com/toasterofbread/spmp-server.git")
            url.set("https://github.com/toasterofbread/spmp-server")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/toasterofbread/spmp-server/issues")
        }
    }
}
