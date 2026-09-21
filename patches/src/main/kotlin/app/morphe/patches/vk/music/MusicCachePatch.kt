package app.morphe.patches.vk.music

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

private const val EXTENSION_CLASS = "Lapp/morphe/extension/vk/music/MusicCache;"

/**
 * AlertMusicTrackModel (xsna.x02) — wraps the real track actions model and blocks
 * the "download" action with a subscription popup when hasMusicSubscription() is
 * false. Replacing the body routes every press straight to the extension MP3 saver.
 */
internal object AlertMusicTrackModelDownloadFingerprint : Fingerprint(
    definingClass = "Lxsna/x02;",
    name = "M",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Lcom/vk/dto/music/MusicTrack;")
)

/**
 * MusicDownloadInteractorImpl (xsna.b4a0) — entry point of the track "download"
 * action. Gates on network state and music subscription before delegating to the
 * worker m(track). The prologue sends the track to the extension instead.
 */
internal object DownloadInteractorDownloadFingerprint : Fingerprint(
    definingClass = "Lxsna/b4a0;",
    name = "l",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "Lcom/vk/dto/music/MusicTrack;")
)

/**
 * MusicDownloadInteractorImpl (xsna.b4a0) — the single worker every track
 * download funnels into (from l and from the offline manager's J). Replacing its
 * body with a call to the extension turns every manual download press into a
 * plain MP3 file in Music/VK Morphe, bypassing the encrypted offline cache.
 */
internal object DownloadInteractorDownloadWorkerFingerprint : Fingerprint(
    definingClass = "Lxsna/b4a0;",
    name = "m",
    returnType = "V",
    parameters = listOf("Lcom/vk/dto/music/MusicTrack;")
)

/**
 * MusicOfflineManagerImpl (xsna.eda0) — alternate track download entry that
 * throws SubscriptionExpiredException when unsubscribed. The prologue sends the
 * track to the extension instead.
 */
internal object MusicOfflineManagerDownloadFingerprint : Fingerprint(
    definingClass = "Lxsna/eda0;",
    name = "J",
    returnType = "V",
    parameters = listOf("Lcom/vk/dto/music/MusicTrack;")
)

/**
 * PrefetchConfig.Disabled (com.vk.music.player.cache.a$b) — returned instead of
 * the Default config when the video-based music player toggle is active, which
 * disables the whole music SimpleCache. Forcing Default values re-enables
 * cache writes and prefetch of upcoming tracks.
 */
internal object PrefetchConfigDisabledAFingerprint : Fingerprint(
    definingClass = "Lcom/vk/music/player/cache/a\$b;",
    name = "a",
    returnType = "I",
    parameters = emptyList()
)

internal object PrefetchConfigDisabledBFingerprint : Fingerprint(
    definingClass = "Lcom/vk/music/player/cache/a\$b;",
    name = "b",
    returnType = "I",
    parameters = emptyList()
)

internal object PrefetchConfigDisabledCFingerprint : Fingerprint(
    definingClass = "Lcom/vk/music/player/cache/a\$b;",
    name = "c",
    returnType = "I",
    parameters = emptyList()
)

internal object PrefetchConfigDisabledDFingerprint : Fingerprint(
    definingClass = "Lcom/vk/music/player/cache/a\$b;",
    name = "d",
    returnType = "I",
    parameters = emptyList()
)

internal object PrefetchConfigDisabledEFingerprint : Fingerprint(
    definingClass = "Lcom/vk/music/player/cache/a\$b;",
    name = "e",
    returnType = "I",
    parameters = emptyList()
)

@Suppress("unused")
val musicCachePatch = bytecodePatch(
    name = "Save tracks as MP3 on download",
    description = "Makes the native download button save the track as a plain MP3 into Music/VK Morphe " +
        "instead of the encrypted offline cache, without a VK Music subscription. " +
        "Also re-enables the music player disk cache.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VK)

    extendWith("extensions/vk.mpe")

    execute {
        // 1. Bottom-sheet entry: subscription gate + dialog replaced by the extension.
        AlertMusicTrackModelDownloadFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p2 .. p2 }, $EXTENSION_CLASS->onDownloadRequested(Ljava/lang/Object;)V
                return-void
            """
        )

        // 2. Interactor entry (network + subscription gates skipped).
        DownloadInteractorDownloadFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p2 .. p2 }, $EXTENSION_CLASS->onDownloadRequested(Ljava/lang/Object;)V
                return-void
            """
        )

        // 3. Alternate entry from the offline manager (was: SubscriptionExpiredException).
        MusicOfflineManagerDownloadFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->onDownloadRequested(Ljava/lang/Object;)V
                return-void
            """
        )

        // 4. Worker: every download request lands in the extension MP3 saver.
        //    Early return makes the rest of the native (encrypted) queueing dead code.
        DownloadInteractorDownloadWorkerFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->onDownloadRequested(Ljava/lang/Object;)V
                return-void
            """
        )

        // Turn PrefetchConfig.Disabled into the Default config:
        //   a() = prefetchTracksCount      = 1
        //   b() = cacheSizeMb               = 50
        //   c() = firstPhasePrefetchDurSec  = 5
        //   d() = secondPhasePrefetchDurMin = 6
        //   e() = firstPhasePrefetchTracks  = 5
        PrefetchConfigDisabledAFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """
        )
        PrefetchConfigDisabledBFingerprint.method.addInstructions(
            0,
            """
                const/16 v0, 0x32
                return v0
            """
        )
        PrefetchConfigDisabledCFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x5
                return v0
            """
        )
        PrefetchConfigDisabledDFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x6
                return v0
            """
        )
        PrefetchConfigDisabledEFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x5
                return v0
            """
        )
    }
}
