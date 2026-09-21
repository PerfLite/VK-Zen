package app.morphe.patches.vk.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

private const val EXTENSION_CLASS = "Lapp/morphe/extension/vk/clips/ClipsAdsFilter;"

internal object ClipsFeedInitActionDelegateFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "FeedInitActionDelegate.kt" && !classDef.type.contains("$") },
    name = "i",
    returnType = "V"
)

internal object AsyncListDifferSubmitListFingerprint : Fingerprint(
    definingClass = "Landroidx/recyclerview/widget/d;",
    name = "b",
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "Ljava/lang/Runnable;")
)

@Suppress("unused")
val clipsAdsPatch = bytecodePatch(
    name = "Hide clips ads",
    description = "Removes commercial video ads, market promos, and sponsored content from VK Clips."
) {
    compatibleWith(COMPATIBILITY_VK)

    extendWith("extensions/vk.mpe")

    execute {
        // Filter out ads from feed items list in clips MVI reducer
        ClipsFeedInitActionDelegateFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p2 }, $EXTENSION_CLASS->filterFeedItems(Ljava/util/List;)Ljava/util/List;
                    move-result-object p2
                """
            )
        }

        // Filter out ads if list submitted to AsyncListDiffer contains clip items
        AsyncListDifferSubmitListFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->filterIfClipFeed(Ljava/util/List;)Ljava/util/List;
                    move-result-object p1
                """
            )
        }
    }
}
