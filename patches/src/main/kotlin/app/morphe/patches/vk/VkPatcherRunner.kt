package app.morphe.patches.vk

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.apk.ApkSigner
import app.morphe.patcher.apk.ApkUtils
import app.morphe.patcher.apk.ApkUtils.applyTo
import app.morphe.patcher.dex.BytecodeMode
import app.morphe.patches.vk.ad.clipsAdsPatch
import app.morphe.patches.vk.ad.feedAdsPatch
import app.morphe.patches.vk.layout.navigationPatch
import app.morphe.patches.vk.misc.cloneAppPatch
import app.morphe.patches.vk.misc.friendsPatch
import app.morphe.patches.vk.misc.securityPatch
import app.morphe.patches.vk.misc.telemetryPatch
import app.morphe.patches.vk.music.audioAdsPatch
import app.morphe.patches.vk.music.backgroundPlaybackPatch
import app.morphe.patches.vk.music.musicCachePatch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardOpenOption

fun main(args: Array<String>) {
    val inputApk = File(args.getOrNull(0) ?: "vk.apk").canonicalFile
    val outputApk = File(args.getOrNull(1) ?: "vk-morphe-cloned.apk").canonicalFile

    require(inputApk.exists()) { "Input APK not found: ${inputApk.absolutePath}" }

    println("==================================================")
    println("Morphe VK Patcher")
    println("Input APK:  ${inputApk.absolutePath} (${inputApk.length() / (1024 * 1024)} MB)")
    println("Output APK: ${outputApk.absolutePath}")
    println("==================================================")

    val tempWorkingDir = File("build/tmp/patcher-work").canonicalFile.apply {
        deleteRecursively()
        mkdirs()
    }

    val tempApk = File(tempWorkingDir, "vk-temp.apk")
    println("Copying input APK to temporary working location...")
    inputApk.copyTo(tempApk, overwrite = true)

    println("Preparing BKS keystore...")
    val keyStoreFile = File(if (File("patches").isDirectory) "patches/morphe.bks" else "morphe.bks").canonicalFile
    keyStoreFile.parentFile.mkdirs()

    val keyStore = if (keyStoreFile.exists()) {
        ApkSigner.readKeyStore(keyStoreFile.inputStream(), "morphekeystore")
    } else {
        println("Creating new persistent BKS keystore: ${keyStoreFile.absolutePath}")
        val pair = ApkSigner.newPrivateKeyCertificatePair("Morphe", java.util.Date(System.currentTimeMillis() + 100L * 365 * 24 * 3600 * 1000))
        val ks = ApkSigner.newKeyStore(setOf(ApkSigner.KeyStoreEntry("morphe", "morphekeystore", pair)))
        keyStoreFile.outputStream().use { ks.store(it, "morphekeystore".toCharArray()) }
        ks
    }

    val cert = keyStore.getCertificate("morphe") as java.security.cert.X509Certificate
    val sha1Digest = java.security.MessageDigest.getInstance("SHA-1").digest(cert.encoded)
    val sha1Hex = sha1Digest.joinToString("") { "%02x".format(it) }
    println("Using certificate SHA-1: $sha1Hex")

    patchNativeAntiTamper(tempApk, sha1Hex)

    val config = PatcherConfig(
        apkFile = tempApk,
        temporaryFilesPath = File(tempWorkingDir, "morphe-temp"),
        useBytecodeMode = BytecodeMode.STRIP_FAST,
    )

    val patcher = Patcher(config)
    try {
        println("Registering patches...")
        patcher += setOf(
            feedAdsPatch,
            clipsAdsPatch,
            navigationPatch,
            backgroundPlaybackPatch,
            audioAdsPatch,
            cloneAppPatch,
            securityPatch,
            musicCachePatch,
            telemetryPatch,
            friendsPatch
        )

        println("Executing patches...")
        runBlocking {
            patcher().collect { result ->
                if (result.exception != null) {
                    System.err.println("FAILED: ${result.patch.name}")
                    result.exception?.printStackTrace()
                    throw result.exception!!
                } else {
                    println("SUCCESS: ${result.patch.name}")
                }
            }
        }

        println("Compiling patched resources and bytecode...")
        val patcherResult = patcher.get()

        println("Applying changes to APK...")
        patcherResult.applyTo(tempApk)

        injectZenIcons(tempApk)

        println("Signing APK...")
        val keyStoreDetails = ApkUtils.KeyStoreDetails(
            keyStore = keyStoreFile,
            keyStorePassword = "morphekeystore",
            alias = "morphe",
            password = "morphekeystore",
        )

        ApkUtils.signApk(
            inputApkFile = tempApk,
            outputApkFile = outputApk,
            signer = "Morphe",
            keyStoreDetails = keyStoreDetails,
        )

        java.util.zip.ZipFile(outputApk).use { zf ->
            val arsc = zf.getEntry("resources.arsc")
            if (arsc != null) {
                println("Output resources.arsc method: ${arsc.method} (STORED = ${java.util.zip.ZipEntry.STORED})")
                check(arsc.method == java.util.zip.ZipEntry.STORED) {
                    "resources.arsc MUST be STORED (uncompressed) for Android 11+ compatibility!"
                }
            }
        }

        println("==================================================")
        println("Patching completed successfully!")
        println("Saved to: ${outputApk.absolutePath} (${outputApk.length() / (1024 * 1024)} MB)")
        println("==================================================")
    } finally {
        patcher.close()
        tempWorkingDir.deleteRecursively()
    }
}

private fun patchNativeAntiTamper(apkFile: File, sha1Hex: String) {
    println("Patching libvkcore anti-tamper signature checks with SHA-1: $sha1Hex...")
    val oldHash = "77bdb3287ec28b5b52d844fa93be39776eb97a31".toByteArray(Charsets.US_ASCII)
    val newHash = sha1Hex.toByteArray(Charsets.US_ASCII)

    val uri = URI.create("jar:" + apkFile.toURI().toString())
    FileSystems.newFileSystem(uri, emptyMap<String, Any>()).use { fs ->
        val arm64Path = fs.getPath("lib/arm64-v8a/libvkcore.so")
        if (Files.exists(arm64Path)) {
            val b = Files.readAllBytes(arm64Path)
            val idx = indexOfBytes(b, oldHash)
            if (idx != -1) {
                System.arraycopy(newHash, 0, b, idx, 40)
                println("Patched SHA-1 in arm64-v8a libvkcore.so")
            }
            val patchBytes = byteArrayOf(
                0x48.toByte(), 0x00.toByte(), 0x00.toByte(), 0xb0.toByte(),
                0x00.toByte(), 0x71.toByte(), 0x43.toByte(), 0xf9.toByte(),
                0xe0.toByte(), 0x02.toByte(), 0x00.toByte(), 0xb4.toByte(),
                0x08.toByte(), 0x00.toByte(), 0x40.toByte(), 0xf9.toByte(),
                0x08.toByte(), 0x15.toByte(), 0x40.toByte(), 0xf9.toByte(),
                0x00.toByte(), 0x01.toByte(), 0x3f.toByte(), 0xd6.toByte(),
                0x13.toByte(), 0x00.toByte(), 0x00.toByte(), 0x14.toByte()
            )
            if (b.size > 0x744c + patchBytes.size) {
                System.arraycopy(patchBytes, 0, b, 0x744c, patchBytes.size)
                println("Patched anti-tamper thread body to signal condition and exit cleanly")
            }
            if (b.size > 0x74dc) {
                // ret (0xd65f03c0) at 0x74d8 (abort handler)
                b[0x74d8] = 0xc0.toByte(); b[0x74d9] = 0x03.toByte(); b[0x74da] = 0x5f.toByte(); b[0x74db] = 0xd6.toByte()
                println("Patched abort handler at 0x74d8 with ret")
            }
            Files.write(arm64Path, b, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        }

        val armv7Path = fs.getPath("lib/armeabi-v7a/libvkcore.so")
        if (Files.exists(armv7Path)) {
            val b = Files.readAllBytes(armv7Path)
            val idx = indexOfBytes(b, oldHash)
            if (idx != -1) {
                System.arraycopy(newHash, 0, b, idx, 40)
                println("Patched SHA-1 in armeabi-v7a libvkcore.so")
            }
            Files.write(armv7Path, b, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }
}

private fun indexOfBytes(source: ByteArray, target: ByteArray): Int {
    if (target.isEmpty()) return 0
    outer@ for (i in 0..source.size - target.size) {
        for (j in target.indices) {
            if (source[i + j] != target[j]) continue@outer
        }
        return i
    }
    return -1
}

private fun injectZenIcons(apkFile: File) {
    println("Injecting VK Zen custom launcher icons...")
    val iconBase = listOf(
        File("src/main/resources/zen_icons"),
        File("patches/src/main/resources/zen_icons"),
        File("../patches/src/main/resources/zen_icons")
    ).firstOrNull { it.isDirectory }
    if (iconBase == null) {
        println("WARNING: zen_icons directory not found, skipping icon injection.")
        return
    }
    val uri = URI.create("jar:" + apkFile.toURI().toString())
    FileSystems.newFileSystem(uri, emptyMap<String, Any>()).use { fs ->
        iconBase.walk().filter { it.isFile }.forEach { file ->
            val relPath = file.relativeTo(iconBase).path.replace('\\', '/')
            val targetPath = fs.getPath(relPath)
            val parent = targetPath.parent
            if (parent != null) {
                Files.createDirectories(parent)
            }
            Files.write(targetPath, file.readBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            println("Replaced $relPath in APK")
        }
    }
}
