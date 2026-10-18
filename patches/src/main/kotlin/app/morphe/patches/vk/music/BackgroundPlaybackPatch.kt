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

// BaseMusicRestrictionPopupDisplayer
internal object BaseMusicRestrictionPopupDisplayerCFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "BaseMusicRestrictionPopupDisplayer.kt" &&
            !classDef.type.contains("$") &&
            method.name == "c" &&
            method.parameterTypes.size == 1
    },
    returnType = "V"
)

internal object BaseMusicRestrictionPopupDisplayerFFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "BaseMusicRestrictionPopupDisplayer.kt" &&
            !classDef.type.contains("$") &&
            method.name == "f" &&
            method.parameterTypes.isEmpty()
    },
    returnType = "V"
)

internal object BaseMusicRestrictionPopupDisplayerGFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "BaseMusicRestrictionPopupDisplayer.kt" &&
            !classDef.type.contains("$") &&
            method.name == "g" &&
            method.parameterTypes.size == 2
    },
    returnType = "V"
)

internal object BaseMusicRestrictionPopupDisplayerHFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "BaseMusicRestrictionPopupDisplayer.kt" &&
            !classDef.type.contains("$") &&
            method.name == "h" &&
            method.parameterTypes.size == 6
    },
    returnType = "V"
)

internal object BaseMusicRestrictionPopupDisplayerIFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "BaseMusicRestrictionPopupDisplayer.kt" &&
            !classDef.type.contains("$") &&
            method.name == "i" &&
            method.parameterTypes.size == 1
    },
    returnType = "V"
)

internal object BaseMusicRestrictionPopupDisplayerKFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "BaseMusicRestrictionPopupDisplayer.kt" &&
            !classDef.type.contains("$") &&
            method.name == "k" &&
            method.parameterTypes.size == 4
    },
    returnType = "V"
)

internal object BaseMusicRestrictionPopupDisplayerLFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "BaseMusicRestrictionPopupDisplayer.kt" &&
            !classDef.type.contains("$") &&
            method.name == "l" &&
            method.parameterTypes.size == 1
    },
    returnType = "V"
)

internal object BaseMusicRestrictionPopupDisplayerMFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "BaseMusicRestrictionPopupDisplayer.kt" &&
            !classDef.type.contains("$") &&
            method.name == "m" &&
            method.parameterTypes.size == 1
    },
    returnType = "V"
)

// VkMusicRestrictionPopupDisplayer (opens BuyMusicSubscription popup bottomsheet)
internal object VkMusicRestrictionPopupDisplayerAFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VkMusicRestrictionPopupDisplayer.kt" &&
            !classDef.type.contains("$") &&
            method.name == "a" &&
            method.parameterTypes.size == 6
    },
    returnType = "V"
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

// Block music promo subscription banners in catalog (CatalogBlockVariant.PromoBannersSlider)
internal object PromoBannerBlockComponentPgFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "PromoBannerBlockComponent.kt" &&
            !classDef.type.contains("$") &&
            method.name == "pg"
    },
    returnType = "V"
)

internal object PromoBannerBlockComponentQgFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "PromoBannerBlockComponent.kt" &&
            !classDef.type.contains("$") &&
            method.name == "qg"
    },
    returnType = "V"
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

        // Disable all restriction dialogs and subscription popups in BaseMusicRestrictionPopupDisplayer
        BaseMusicRestrictionPopupDisplayerCFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        BaseMusicRestrictionPopupDisplayerFFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        BaseMusicRestrictionPopupDisplayerGFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        BaseMusicRestrictionPopupDisplayerHFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        BaseMusicRestrictionPopupDisplayerIFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        BaseMusicRestrictionPopupDisplayerKFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        BaseMusicRestrictionPopupDisplayerLFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        BaseMusicRestrictionPopupDisplayerMFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        // Disable BuyMusicSubscription popup in VkMusicRestrictionPopupDisplayer
        VkMusicRestrictionPopupDisplayerAFingerprint.method.addInstructions(
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

        // Disable registration of PromoBannersSlider in music catalog MVI registry and parser
        PromoBannerBlockComponentPgFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )

        PromoBannerBlockComponentQgFingerprint.method.addInstructions(
            0,
            """
                return-void
            """
        )
    }
}
