package app.morphe.patches.vk.video

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

private const val EXTENSION_CLASS = "Lapp/morphe/extension/vk/video/VideoMirrorHelper;"

// 1. VK Video fullscreen player toolbar
internal object VideoToolbarViewInitFingerprint : Fingerprint(
    definingClass = "Lcom/vk/libvideo/design/view/toolbar/VideoToolbarView;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Landroid/util/AttributeSet;"
    )
)

// 2. OneVideo / VK Video timeline buttons view
internal object VideoButtonsViewInitFingerprint : Fingerprint(
    definingClass = "Lone/video/controls/views/VideoButtonsView;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Landroid/util/AttributeSet;"
    )
)

// 3. OneVideo short actions overlay
internal object VideoShortActionsInitFingerprint : Fingerprint(
    definingClass = "Lone/video/controls/views/VideoShortActions;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Landroid/util/AttributeSet;"
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
        VideoToolbarViewInitFingerprint.method.apply {
            val lastIdx = implementation!!.instructions.size - 1
            addInstructions(
                lastIdx,
                """
                    invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->injectMirrorButton(Landroid/view/ViewGroup;)V
                """
            )
        }

        VideoButtonsViewInitFingerprint.method.apply {
            val lastIdx = implementation!!.instructions.size - 1
            addInstructions(
                lastIdx,
                """
                    invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->injectMirrorButton(Landroid/view/ViewGroup;)V
                """
            )
        }

        VideoShortActionsInitFingerprint.method.apply {
            val lastIdx = implementation!!.instructions.size - 1
            addInstructions(
                lastIdx,
                """
                    invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->injectMirrorButton(Landroid/view/ViewGroup;)V
                """
            )
        }
    }
}
