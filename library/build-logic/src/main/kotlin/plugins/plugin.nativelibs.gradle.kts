import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.os.OperatingSystem
import java.nio.file.Files.getPosixFilePermissions
import java.nio.file.Files.setPosixFilePermissions
import java.nio.file.attribute.PosixFilePermission

plugins {
    id("io.github.krakowski.jextract")
    id("de.undercouch.download")
}

fun Copy.getJextractBinary(): File =
    destinationDir.resolve(rootProject.extra["jextract.dirname"] as String).resolve("jextract")

val downloadJextract by tasks.registering(Download::class) {
    src(rootProject.extra["jextract.url"] as String)
    dest(rootProject.file(".jextract/jextract.tar.gz"))
    onlyIfModified(true)
}

val extractJextract by tasks.registering(Copy::class) {
    into(rootProject.file(".jextract"))
    from(tarTree(resources.gzip(downloadJextract.get().dest)))

    doLast {
        downloadJextract.get().dest.delete()
    }

    if (getJextractBinary().isFile) {
        onlyIf { false }
    }
    else {
        dependsOn(downloadJextract)
    }
}

tasks.jextract {
    dependsOn(extractJextract)

    header("/usr/include/mpv/client.h") {
        libraries = listOf("mpv")
        targetPackage = "libmpv"
        className = "client"
        outputDir = project.file("src/jvmMain/java")

        val binary: File = extractJextract.get().getJextractBinary()
        jextractBinary = binary

        doFirst {
            if (OperatingSystem.current().isUnix()) {
                val permissions: MutableSet<PosixFilePermission> = getPosixFilePermissions(binary.toPath())
                permissions.add(PosixFilePermission.OWNER_EXECUTE)
                setPosixFilePermissions(binary.toPath(), permissions)
            }
        }
    }
}

afterEvaluate {
    for (task in listOf("compileKotlinJvm", "jvmSourcesJar")) {
        tasks.getByName(task) {
            dependsOn(tasks.jextract)
        }
    }
}
