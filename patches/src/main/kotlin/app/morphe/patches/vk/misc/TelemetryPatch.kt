package app.morphe.patches.vk.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

internal object MyTrackerInitFingerprint : Fingerprint(
    definingClass = "Lcom/my/tracker/MyTracker;",
    name = "initTracker",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/app/Application;")
)

internal object MyTrackerTrackEventFingerprint : Fingerprint(
    definingClass = "Lcom/my/tracker/MyTracker;",
    name = "trackEvent",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;")
)

internal object MyTrackerTrackEventWithMapFingerprint : Fingerprint(
    definingClass = "Lcom/my/tracker/MyTracker;",
    name = "trackEvent",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;")
)

internal object AppStartReporterBFingerprint : Fingerprint(
    definingClass = "Lcom/vk/stat/AppStartReporter;",
    name = "b",
    returnType = "V"
)

internal object AppStartReporterCFingerprint : Fingerprint(
    definingClass = "Lcom/vk/stat/AppStartReporter;",
    name = "c",
    returnType = "V"
)

internal object NativeAdAnalyticsSenderAFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "NativeAdUserLevelAnalyticsSenderImpl.kt" && !classDef.type.contains("\$") },
    name = "a",
    returnType = "V"
)

internal object NativeAdAnalyticsSenderBFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "NativeAdUserLevelAnalyticsSenderImpl.kt" && !classDef.type.contains("\$") },
    name = "b",
    returnType = "V"
)

internal object NativeAdAnalyticsSenderCFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "NativeAdUserLevelAnalyticsSenderImpl.kt" && !classDef.type.contains("\$") },
    name = "c",
    returnType = "V"
)

internal object BaseAnalyticsSenderFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "BaseAnalyticsSender.kt" && classDef.type == "Lxsna/qv6;" },
    name = "a",
    returnType = "V"
)

@Suppress("unused")
val telemetryPatch = bytecodePatch(
    name = "Disable telemetry",
    description = "Disables MyTracker, VK Stat / AppStartReporter, and native ad analytics senders."
) {
    compatibleWith(COMPATIBILITY_VK)

    execute {
        // Prevent MyTracker from initializing
        MyTrackerInitFingerprint.method.apply {
            addInstructions(0, "return-void")
        }

        // Suppress MyTracker event reporting
        MyTrackerTrackEventFingerprint.method.apply {
            addInstructions(0, "return-void")
        }
        MyTrackerTrackEventWithMapFingerprint.method.apply {
            addInstructions(0, "return-void")
        }

        // Suppress AppStartReporter background polling / reporter loops
        AppStartReporterBFingerprint.method.apply {
            addInstructions(0, "return-void")
        }
        AppStartReporterCFingerprint.method.apply {
            addInstructions(0, "return-void")
        }

        // Suppress native ad telemetry & attribution reporting
        NativeAdAnalyticsSenderAFingerprint.method.apply {
            addInstructions(0, "return-void")
        }
        NativeAdAnalyticsSenderBFingerprint.method.apply {
            addInstructions(0, "return-void")
        }
        NativeAdAnalyticsSenderCFingerprint.method.apply {
            addInstructions(0, "return-void")
        }

        // Suppress base ad/event analytics sender
        BaseAnalyticsSenderFingerprint.method.apply {
            addInstructions(0, "return-void")
        }
    }
}
