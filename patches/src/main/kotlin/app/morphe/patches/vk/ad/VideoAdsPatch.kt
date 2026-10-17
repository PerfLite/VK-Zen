package app.morphe.patches.vk.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

// VideoAdvertisementsRepositoryImpl (xsna.ed01)
// g: initiates instream/overlay ad loading
internal object VideoAdvertisementsRepoGFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoAdvertisementsRepositoryImpl.kt" &&
            method.name == "g" &&
            method.parameterTypes.size == 5
    },
    returnType = "V"
)

// b: sets ad section
internal object VideoAdvertisementsRepoBFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoAdvertisementsRepositoryImpl.kt" &&
            method.name == "b" &&
            method.parameterTypes.size == 2
    },
    returnType = "V"
)

// c: sets ad type
internal object VideoAdvertisementsRepoCFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoAdvertisementsRepositoryImpl.kt" &&
            method.name == "c" &&
            method.parameterTypes.size == 1
    },
    returnType = "V"
)

// d: loads banner/overlay ad data
internal object VideoAdvertisementsRepoDFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoAdvertisementsRepositoryImpl.kt" &&
            method.name == "d" &&
            method.parameterTypes.size == 1
    },
    returnType = "V"
)

// VideoAdLayout (com.vk.libvideo.design.view.ad.VideoAdLayout)
// a: binds and displays video ad overlay controls ("Реклама", redirect button, timers)
internal object VideoAdLayoutAFingerprint : Fingerprint(
    definingClass = "Lcom/vk/libvideo/design/view/ad/VideoAdLayout;",
    name = "a",
    returnType = "V",
    custom = { method, _ -> method.parameterTypes.size == 6 }
)

// VideoFileOld (com.vk.dto.common.VideoFileOld)
// p8: returns InstreamAd
internal object VideoFileOldInstreamAdFingerprint : Fingerprint(
    definingClass = "Lcom/vk/dto/common/VideoFileOld;",
    name = "p8",
    returnType = "Lcom/vk/dto/common/InstreamAd;"
)

// b1: returns VideoAdInfo
internal object VideoFileOldVideoAdInfoFingerprint : Fingerprint(
    definingClass = "Lcom/vk/dto/common/VideoFileOld;",
    name = "b1",
    returnType = "Lcom/vk/dto/common/VideoAdInfo;"
)

// p1: returns OrdAdInfo
internal object VideoFileOldOrdAdInfoFingerprint : Fingerprint(
    definingClass = "Lcom/vk/dto/common/VideoFileOld;",
    name = "p1",
    returnType = "Lcom/vk/dto/common/OrdAdInfo;"
)

// VideoAttachment (com.vkontakte.android.attachments.VideoAttachment)
// bc: returns ShitAttachment (promo snippet card under video)
internal object VideoAttachmentBcFingerprint : Fingerprint(
    definingClass = "Lcom/vkontakte/android/attachments/VideoAttachment;",
    name = "bc",
    returnType = "Lcom/vkontakte/android/attachments/ShitAttachment;"
)

// gc: sets ShitAttachment
internal object VideoAttachmentGcFingerprint : Fingerprint(
    definingClass = "Lcom/vkontakte/android/attachments/VideoAttachment;",
    name = "gc",
    returnType = "V"
)

// VideoGrowthTrapsManagerImpl (Video growth traps and superapp standalone bottomsheet / bombing popups)
internal object VideoGrowthTrapsManagerAFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoGrowthTrapsManagerImpl.kt" &&
            !classDef.type.contains("$") &&
            method.name == "a" &&
            method.parameterTypes.size == 4
    },
    returnType = "V"
)

internal object VideoGrowthTrapsManagerBFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoGrowthTrapsManagerImpl.kt" &&
            !classDef.type.contains("$") &&
            method.name == "b" &&
            method.parameterTypes.size == 4
    },
    returnType = "V"
)

internal object VideoGrowthTrapsManagerCFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoGrowthTrapsManagerImpl.kt" &&
            !classDef.type.contains("$") &&
            method.name == "c" &&
            method.parameterTypes.size == 3
    },
    returnType = "V"
)

// VideoGrowthTrapLauncherImpl
internal object VideoGrowthTrapLauncherAFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoGrowthTrapLauncherImpl.kt" &&
            !classDef.type.contains("$") &&
            method.name == "a" &&
            method.parameterTypes.size == 2
    },
    returnType = "V"
)

// VideoGrowthBombingControllerImpl
internal object VideoGrowthBombingControllerAFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoGrowthBombingControllerImpl.kt" &&
            !classDef.type.contains("$") &&
            method.name == "a" &&
            method.parameterTypes.isEmpty()
    },
    returnType = "Z"
)

// VideoGrowthListingControllerImpl
internal object VideoGrowthListingControllerBFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.sourceFile == "VideoGrowthListingControllerImpl.kt" &&
            !classDef.type.contains("$") &&
            method.name == "b" &&
            method.parameterTypes.size == 1
    },
    returnType = "Z"
)

@Suppress("unused")
val videoAdsPatch = bytecodePatch(
    name = "Video ads",
    description = "Blocks video player in-stream advertisements (prerolls, midrolls, postrolls, ad overlays), attached promo ad cards below videos, and VK Video / RuStore download prompts.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VK)

    execute {
        // Neutralize VideoAdvertisementsRepositoryImpl methods so video ads are never loaded
        VideoAdvertisementsRepoGFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        VideoAdvertisementsRepoBFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        VideoAdvertisementsRepoCFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        VideoAdvertisementsRepoDFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Prevent VideoAdLayout from showing ad overlays in video player
        VideoAdLayoutAFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Return null for video file ad getters so player sees no attached ad streams
        VideoFileOldInstreamAdFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """
            )
        }

        VideoFileOldVideoAdInfoFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """
            )
        }

        VideoFileOldOrdAdInfoFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """
            )
        }

        // Prevent VideoAttachment from returning or holding attached promo cards (ShitAttachment)
        VideoAttachmentBcFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """
            )
        }

        VideoAttachmentGcFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Neutralize VideoGrowth traps and "Смотри в приложении VK Видео" popups / bottomsheets
        VideoGrowthTrapsManagerAFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        VideoGrowthTrapsManagerBFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        VideoGrowthTrapsManagerCFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        VideoGrowthTrapLauncherAFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        VideoGrowthBombingControllerAFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """
            )
        }

        VideoGrowthListingControllerBFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """
            )
        }
    }
}

