package app.morphe.patches.vk.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

@Suppress("unused")
val friendsPatch = bytecodePatch(
    name = "Friends customization",
    description = "Customization for friends."
) {
    compatibleWith(COMPATIBILITY_VK)

    extendWith("extensions/vk.mpe")

    execute {
        // Disabled per user request
    }
}

