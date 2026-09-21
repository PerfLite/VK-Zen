package app.morphe.patches.vk.layout

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

private const val EXTENSION_CLASS = "Lapp/morphe/extension/vk/layout/NavigationFilter;"

internal object TabbarStateConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/vk/tabbar/core/api/domain/TabbarState;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Ljava/util/List;",
        "Ljava/lang/Boolean;"
    )
)

internal object TabbarStateGetItemsFingerprint : Fingerprint(
    definingClass = "Lcom/vk/tabbar/core/api/domain/TabbarState;",
    name = "c",
    returnType = "Ljava/util/List;",
    parameters = emptyList()
)

internal object MainActivityOnResumeFingerprint : Fingerprint(
    definingClass = "Lcom/vkontakte/android/MainActivity;",
    name = "onResume",
    returnType = "V",
    parameters = emptyList()
)

// Intercept TabbarSettingsRouterImpl.c(Context, String)
internal object TabbarSettingsRouterImplCFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "TabbarSettingsRouterImpl.kt" },
    name = "c",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Ljava/lang/String;"
    )
)

// Intercept TabbarSettingsRouterImpl.a(NavigationDelegateActivity, int, TabbarState)
internal object TabbarSettingsRouterImplAFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "TabbarSettingsRouterImpl.kt" },
    name = "a",
    returnType = "V",
    parameters = listOf(
        "Lcom/vk/navigation/NavigationDelegateActivity;",
        "I",
        "Lcom/vk/tabbar/core/api/domain/TabbarState;"
    )
)

// Intercept TabbarSettingsFragment.Sj (onViewCreated)
internal object TabbarSettingsFragmentFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "TabbarSettingsFragment.kt" },
    name = "Sj",
    returnType = "V",
    parameters = listOf(
        "Lxsna/kab0;",
        "Landroid/view/View;"
    )
)

private const val SUPERAPP_FILTER_CLASS = "Lapp/morphe/extension/vk/layout/SuperAppFilter;"

internal object SuperAppFragmentSubmitFingerprint : Fingerprint(
    definingClass = "Lcom/vk/superapp/ui/SuperAppFragment;",
    name = "Sq",
    returnType = "V",
    parameters = listOf("Lcom/vk/superapp/dto/ListData;")
)


@Suppress("unused")
val navigationPatch = bytecodePatch(
    name = "Customize navigation bar",
    description = "Removes unwanted tabs (Clips, Services, etc.) from the bottom navigation bar."
) {
    compatibleWith(COMPATIBILITY_VK)

    extendWith("extensions/vk.mpe")

    execute {
        TabbarStateConstructorFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->filterTabbarItems(Ljava/util/List;)Ljava/util/List;
                    move-result-object p1
                """
            )
        }

        TabbarStateGetItemsFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p0 }, $EXTENSION_CLASS->filterTabbarStateItems(Ljava/lang/Object;)Ljava/util/List;
                    move-result-object v0
                    if-eqz v0, :cond_orig
                    return-object v0
                    :cond_orig
                """
            )
        }

        MainActivityOnResumeFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p0 }, $EXTENSION_CLASS->onMainActivityResume(Landroid/app/Activity;)V
                """
            )
        }

        // Intercept opening VK tabbar settings to show our full 5-tab customization UI
        TabbarSettingsRouterImplCFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->openSettings(Landroid/content/Context;)V
                    return-void
                """
            )
        }

        TabbarSettingsRouterImplAFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->openSettings(Landroid/content/Context;)V
                    return-void
                """
            )
        }

        // If TabbarSettingsFragment is ever created directly, dismiss it and show our dialog
        TabbarSettingsFragmentFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p0 }, $EXTENSION_CLASS->onTabbarSettingsFragmentCreated(Ljava/lang/Object;)V
                """
            )
        }

        // Filter SuperApp (Services) tab to keep only Weather, Communities, Video, Clips, Settings
        SuperAppFragmentSubmitFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $SUPERAPP_FILTER_CLASS->filterListData(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object p1
                    check-cast p1, Lcom/vk/superapp/dto/ListData;
                """
            )
        }
    }
}

