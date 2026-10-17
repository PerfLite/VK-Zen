package app.morphe.patches.vk.misc

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

val nativePatch = resourcePatch(
    name = "Native anti-tamper bypass",
    description = "Neutralizes signature and integrity checks in libvkcore.so and libvkmedia.so to prevent crashing on custom-signed APKs.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VK)

    finalize {
        try {
            val arm64Core = this["lib/arm64-v8a/libvkcore.so"]
            if (arm64Core.exists()) {
                val b = arm64Core.readBytes()
                val patchBytes = byteArrayOf(
                    0x48.toByte(), 0x00.toByte(), 0x00.toByte(), 0xb0.toByte(),
                    0x00.toByte(), 0x71.toByte(), 0x43.toByte(), 0xf9.toByte(),
                    0xe0.toByte(), 0x02.toByte(), 0x00.toByte(), 0xb4.toByte(),
                    0x08.toByte(), 0x00.toByte(), 0x40.toByte(), 0xf9.toByte(),
                    0x08.toByte(), 0x15.toByte(), 0x40.toByte(), 0xf9.toByte(),
                    0x00.toByte(), 0x01.toByte(), 0x3f.toByte(), 0xd6.toByte(),
                    0x13.toByte(), 0x00.toByte(), 0x00.toByte(), 0x14.toByte()
                )
                var modified = false
                if (b.size > 0x744c + patchBytes.size) {
                    System.arraycopy(patchBytes, 0, b, 0x744c, patchBytes.size)
                    modified = true
                }
                if (b.size > 0x74dc) {
                    b[0x74d8] = 0xc0.toByte(); b[0x74d9] = 0x03.toByte(); b[0x74da] = 0x5f.toByte(); b[0x74db] = 0xd6.toByte()
                    modified = true
                }
                if (modified) {
                    arm64Core.writeBytes(b)
                    println("Patched libvkcore.so anti-tamper in APK")
                }
            }

            val aarch64Nop = byteArrayOf(0x1f, 0x20, 0x03, 0xd5.toByte())
            val vkMediaTamperBranchSites = listOf(
                Triple(0x68874, byteArrayOf(0x80.toByte(), 0x07, 0x00, 0x36), aarch64Nop),
                Triple(0x689ac, byteArrayOf(0x80.toByte(), 0x29, 0x00, 0x36), aarch64Nop),
                Triple(0x68f1c, byteArrayOf(0x80.toByte(), 0x08, 0x00, 0x36), aarch64Nop),
                Triple(0x6d540, byteArrayOf(0x00, 0x05, 0x00, 0x54), byteArrayOf(0x20, 0x00, 0x00, 0x14)),
            )
            val arm64Media = this["lib/arm64-v8a/libvkmedia.so"]
            if (arm64Media.exists()) {
                val mb = arm64Media.readBytes()
                var patched = 0
                for ((off, expect, repl) in vkMediaTamperBranchSites) {
                    if (off + expect.size <= mb.size &&
                        mb.copyOfRange(off, off + expect.size).contentEquals(expect)
                    ) {
                        repl.copyInto(mb, off)
                        patched++
                    }
                }
                if (patched > 0) {
                    arm64Media.writeBytes(mb)
                    println("Patched $patched libvkmedia.so tamper-kill branches in APK")
                }
            }
        } catch (t: Throwable) {
            println("WARNING: Native anti-tamper patch failed: ${t.message}")
        }
    }
}
