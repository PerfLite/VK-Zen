package app.morphe.patches.vk.video

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

private const val EXTENSION_CLASS = "Lapp/morphe/extension/vk/video/VideoMirrorHelper;"

internal object VideoShortActionsInitFingerprint : Fingerprint(
    definingClass = "Lone/video/controls/views/VideoShortActions;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Landroid/util/AttributeSet;",
        "I",
        "I"
    )
)

@Suppress("unused")
val videoMirrorPatch = bytecodePatch(
    name = "Video mirror button",
    description = "Adds a horizontal flip / mirror button to the video player overlay to flip video horizontally with a single tap.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VK)

    extendWith("extensions/vk.mpe")

    execute {
        VideoShortActionsInitFingerprint.method.apply {
            val lastIdx = implementation!!.instructions.size - 1
            addInstructions(
                lastIdx,
                """
                    invoke-static { p0 }, $EXTENSION_CLASS->injectMirrorButton(Landroid/view/ViewGroup;)V
                """
            )
        }
    }
}
