package app.morphe.patches.vk.music

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

internal object MusicRestrictionPopupDisplayerDFingerprint : Fingerprint(
    definingClass = "Lcom/vk/music/notifications/restriction/MusicRestrictionPopupDisplayer;",
    name = "d"
)

internal object MusicRestrictionPopupDisplayerEFingerprint : Fingerprint(
    definingClass = "Lcom/vk/music/notifications/restriction/MusicRestrictionPopupDisplayer;",
    name = "e"
)

internal object MusicRestrictionPopupDisplayerBFingerprint : Fingerprint(
    definingClass = "Lcom/vk/music/notifications/restriction/MusicRestrictionPopupDisplayer;",
    name = "b"
)

internal object MusicPrefsPlayedTimeFingerprint : Fingerprint(
    strings = listOf("played_time"),
    returnType = "J",
    parameters = listOf()
)

internal object MusicBackgroundRestrictionStrategyFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "MusicBackgroundRestrictionStrategy.kt" },
    name = "a",
    returnType = "V",
    parameters = listOf()
)

internal object ScreenStateReceiverFingerprint : Fingerprint(
    definingClass = "Lcom/vk/music/broadcast/ScreenStateReceiver;",
    name = "onReceive"
)

internal object ScreenStateObserverImplDFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "ScreenStateObserverImpl.kt" },
    name = "d",
    returnType = "V",
    parameters = listOf()
)

internal object ScreenStateObserverImplGFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "ScreenStateObserverImpl.kt" },
    name = "G",
    returnType = "V",
    parameters = listOf()
)

@Suppress("unused")
val backgroundPlaybackPatch = bytecodePatch(
    name = "Unlock background music playback",
    description = "Removes background playback restrictions, disables screen-off pause, and disables restriction popups.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VK)

    execute {
        // Disable subscription restriction popup dialogs (static helpers on interface)
        MusicRestrictionPopupDisplayerDFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        MusicRestrictionPopupDisplayerEFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        MusicRestrictionPopupDisplayerBFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // Disable background restriction strategy check so it never pauses playback
        MusicBackgroundRestrictionStrategyFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // Always return 0 played background time so background timer never expires
        MusicPrefsPlayedTimeFingerprint.method.addInstructions(
            0,
            """
                const-wide/16 v0, 0x0
                return-wide v0
            """
        )

        // Disable screen-off broadcast handling in ScreenStateReceiver
        ScreenStateReceiverFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // Disable screen-off playback restriction pause in ScreenStateObserverImpl
        ScreenStateObserverImplDFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        ScreenStateObserverImplGFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}
