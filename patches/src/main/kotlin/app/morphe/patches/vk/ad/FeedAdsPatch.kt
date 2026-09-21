package app.morphe.patches.vk.ad

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.vk.shared.Constants.COMPATIBILITY_VK

private const val EXTENSION_CLASS = "Lapp/morphe/extension/vk/ad/FeedAdsFilter;"

// PostDisplayItemsBuilder.g (static helper returning ArrayList)
internal object PostDisplayItemsBuilderGFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "g",
    returnType = "Ljava/util/ArrayList;"
)

// PostDisplayItemsBuilder.f (main entry point for building display items)
internal object PostDisplayItemsBuilderFFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "f",
    returnType = "V"
)

// PostDisplayItemsBuilder.d (Post item builder)
internal object PostDisplayItemsBuilderDFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "d",
    returnType = "V"
)

// PostDisplayItemsBuilder.e (ShitAttachment builder)
internal object PostDisplayItemsBuilderEFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "e",
    returnType = "V"
)

// PostDisplayItemsBuilder.w (RecommendedMiniApp builder)
internal object PostDisplayItemsBuilderWFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "w",
    returnType = "V"
)

// PostDisplayItemsBuilder.l (ChannelsRecommendations builder)
internal object PostDisplayItemsBuilderLFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "l",
    returnType = "V",
    parameters = listOf("Lcom/vk/feed/core/models/channels/ChannelsRecommendations;", "Ljava/util/ArrayList;")
)

// PostDisplayItemsBuilder.r (GroupsSuggestions builder)
internal object PostDisplayItemsBuilderRFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "r",
    returnType = "V",
    parameters = listOf("Lcom/vk/dto/common/GroupsSuggestions;", "Ljava/util/ArrayList;")
)

// PostDisplayItemsBuilder.o (DzenArticlesBlock builder)
internal object PostDisplayItemsBuilderOFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "o",
    returnType = "V",
    parameters = listOf("Lcom/vk/dto/newsfeed/entries/DzenArticlesBlock;", "Ljava/util/ArrayList;")
)

// PostDisplayItemsBuilder.p (DzenStory builder)
internal object PostDisplayItemsBuilderPFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "p",
    returnType = "V"
)

// PostDisplayItemsBuilder.a (Carousel recommendations builder)
internal object PostDisplayItemsBuilderAFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "a",
    returnType = "V",
    parameters = listOf("Ljava/util/ArrayList;", "Lcom/vk/dto/discover/carousel/Carousel;")
)

// PostDisplayItemsBuilder.k (Carousel recommendations builder)
internal object PostDisplayItemsBuilderKFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemsBuilder.kt" },
    name = "k",
    returnType = "V",
    parameters = listOf("Z", "Lcom/vk/dto/discover/carousel/Carousel;", "Ljava/util/ArrayList;")
)

// NewsfeedItemAdsBlockDtoToNewsEntryMapper.a
internal object NewsfeedAdsBlockMapperFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "NewsfeedItemAdsBlockDtoToNewsEntryMapper.kt" },
    name = "a",
    returnType = "Lcom/vk/feed/core/models/news/NewsEntry;"
)

// NewsfeedItemPromoButtonDtoToPromoButtonMapper.a
internal object NewsfeedPromoButtonMapperFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "NewsfeedItemPromoButtonDtoToPromoButtonMapper.kt" },
    name = "a",
    returnType = "Lcom/vk/feed/core/models/news/PromoButton;"
)

internal object NewsfeedDataConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/vk/dto/newsfeed/NewsfeedData;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "Lcom/vk/dto/newsfeed/NewsfeedData\$Info;")
)

internal object NewsfeedAdsEnricherFingerprint : Fingerprint(
    returnType = "V",
    custom = { _, classDef -> classDef.sourceFile == "NewsfeedEntriesNativeAdsEnricher.kt" }
)

internal object NewsEntriesContainerConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/vk/newsfeed/api/data/discover/NewsEntriesContainer;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Lcom/vk/newsfeed/api/data/discover/NewsEntriesContainer\$Info;",
        "Ljava/util/List;"
    )
)

internal object EntriesListPresenterSetItemsFingerprint : Fingerprint(
    definingClass = "Lcom/vk/newsfeed/impl/presenters/EntriesListPresenter;",
    name = "m",
    returnType = "V",
    parameters = listOf("Ljava/util/List;")
)

internal object EntriesListPresenterAddItemsFingerprint : Fingerprint(
    definingClass = "Lcom/vk/newsfeed/impl/presenters/EntriesListPresenter;",
    name = "n",
    returnType = "V",
    parameters = listOf("Ljava/util/List;")
)

internal object EntriesListPresenterKFingerprint : Fingerprint(
    definingClass = "Lcom/vk/newsfeed/impl/presenters/EntriesListPresenter;",
    name = "K",
    returnType = "V",
    parameters = listOf("Ljava/util/List;", "Z")
)

internal object GetStoriesResponseConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/vk/dto/stories/model/GetStoriesResponse;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "I",
        "Ljava/lang/String;",
        "Ljava/util/List;",
        "Lcom/vk/dto/stories/model/StoriesAds;",
        "Ljava/lang/String;",
        "Lcom/vk/dto/stories/model/ideas/StoryIdeasBlock;"
    )
)

internal object PostDisplayItemListExtKFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "PostDisplayItemListExt.kt" },
    name = "k",
    returnType = "V",
    parameters = listOf(
        "Ljava/util/List;",
        "Lcom/vkontakte/android/attachments/VideoSnippetAttachment;",
        "Lcom/vk/feed/core/models/news/NewsEntry;",
        "Lcom/vk/feed/core/models/news/NewsEntry;",
        "Lxsna/qfi0;"
    )
)

internal object SuperAppAdapterSetItemsFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "SuperAppAdapter.kt" },
    name = "setItems",
    returnType = "V",
    parameters = listOf("Ljava/util/List;")
)

internal object CatalogSectionConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/vk/catalog2/common/dto/api/section/CatalogSection;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/String;",
        "Lcom/vk/catalog2/common/dto/api/CatalogDataType;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Lcom/vk/catalog2/common/dto/api/badge/CatalogBadge;",
        "Ljava/util/List;",
        "Ljava/util/List;",
        "Ljava/util/List;",
        "Lcom/vk/catalog2/common/dto/api/hint/CatalogHint;",
        "Lcom/vk/catalog2/common/dto/api/section/CatalogSectionStyle;",
        "Lcom/vk/catalog2/common/dto/api/section/CatalogHeaderStyle;",
        "Lcom/vk/catalog2/common/dto/api/section/CatalogAdBanner;",
        "Ljava/lang/String;"
    )
)

internal object ProductAttachesHolderImplDFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "ProductAttachesHolderImpl.kt" && !classDef.type.contains("\$") },
    name = "d",
    returnType = "V",
    parameters = listOf("Landroid/view/View;")
)

internal object ProductAttachesHolderImplEFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "ProductAttachesHolderImpl.kt" && !classDef.type.contains("\$") },
    name = "e",
    returnType = "V"
)

internal object ProductAttachesLargeViewBinderFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "ProductAttachesLargeViewBinder.kt" && !classDef.type.contains("\$") },
    name = "a",
    returnType = "V"
)

internal object ProductAttachesTileViewBinderFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "ProductAttachesTileViewBinder.kt" && !classDef.type.contains("\$") },
    name = "a",
    returnType = "V"
)

internal object ClipProductAttachesLargeViewHolderFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "ClipProductAttachesLargeViewHolder.kt" && !classDef.type.contains("\$") },
    name = "L6",
    returnType = "V"
)

internal object MarketAdsItemMviViewOnAttachedFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "MarketAdsItemMviView.kt" && !classDef.type.contains("\$") },
    name = "onAttachedToWindow",
    returnType = "V",
    parameters = emptyList()
)

internal object MarketAdsItemMviViewSjFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "MarketAdsItemMviView.kt" && !classDef.type.contains("\$") },
    name = "Sj",
    returnType = "V"
)

internal object ShopsMoreBadgeHolderFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "ShopsMoreBadgeHolder.kt" && classDef.type.contains("holders/k;") },
    name = "L6",
    returnType = "V"
)

internal object TopShopsMoreBadgeHolderFingerprint : Fingerprint(
    custom = { _, classDef -> classDef.sourceFile == "TopShopsMoreBadgeHolder.kt" && classDef.type.contains("holders/o;") },
    name = "L6",
    returnType = "V"
)

@Suppress("unused")
val feedAdsPatch = bytecodePatch(
    name = "Hide feed ads",
    description = "Hides sponsored posts, promo articles, and recommended mini-apps in newsfeed."
) {

    compatibleWith(COMPATIBILITY_VK)

    extendWith("extensions/vk.mpe")

    execute {
        // Modern MVI newsfeed: PostDisplayItemsBuilder.g returns empty list for ads
        PostDisplayItemsBuilderGFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->isAdItem(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :cond_not_ad
                    new-instance v0, Ljava/util/ArrayList;
                    invoke-direct { v0 }, Ljava/util/ArrayList;-><init>()V
                    return-object v0
                    :cond_not_ad
                """
            )
        }

        // Modern MVI newsfeed: PostDisplayItemsBuilder.f suppresses ad item dispatch
        PostDisplayItemsBuilderFFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->isAdItem(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :cond_not_ad
                    return-void
                    :cond_not_ad
                """
            )
        }

        // Modern MVI newsfeed: PostDisplayItemsBuilder.d suppresses sponsored Post display and sanitizes attachments
        PostDisplayItemsBuilderDFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->isAdItem(Ljava/lang/Object;)Z
                    move-result v0
                    if-eqz v0, :cond_not_ad
                    return-void
                    :cond_not_ad
                    invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->sanitizePost(Ljava/lang/Object;)V
                """
            )
        }

        // Modern MVI newsfeed: suppress ShitAttachment (native ads)
        PostDisplayItemsBuilderEFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Modern MVI newsfeed: suppress RecommendedMiniAppEntry
        PostDisplayItemsBuilderWFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Modern MVI newsfeed: suppress ChannelsRecommendations
        PostDisplayItemsBuilderLFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Modern MVI newsfeed: suppress GroupsSuggestions
        PostDisplayItemsBuilderRFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Modern MVI newsfeed: suppress DzenArticlesBlock
        PostDisplayItemsBuilderOFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Modern MVI newsfeed: suppress DzenStory
        PostDisplayItemsBuilderPFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Modern MVI newsfeed: suppress Carousel recommendations
        PostDisplayItemsBuilderAFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }
        PostDisplayItemsBuilderKFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // DTO mapper: AdsBlock mapper returns null
        NewsfeedAdsBlockMapperFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """
            )
        }

        // DTO mapper: PromoButton mapper returns null
        NewsfeedPromoButtonMapperFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return-object v0
                """
            )
        }

        // Filter feed entries in legacy NewsfeedData constructor
        NewsfeedDataConstructorFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->filterNewsfeedList(Ljava/util/List;)Ljava/util/List;
                    move-result-object p1
                """
            )
        }

        // Filter feed entries in modern NewsEntriesContainer constructor
        NewsEntriesContainerConstructorFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p2 }, $EXTENSION_CLASS->filterNewsfeedList(Ljava/util/List;)Ljava/util/List;
                    move-result-object p2
                """
            )
        }

        // Filter feed entries in EntriesListPresenter.m (setItems)
        EntriesListPresenterSetItemsFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->filterNewsfeedList(Ljava/util/List;)Ljava/util/List;
                    move-result-object p1
                """
            )
        }

        // Filter feed entries in EntriesListPresenter.n (addItems)
        EntriesListPresenterAddItemsFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->filterNewsfeedList(Ljava/util/List;)Ljava/util/List;
                    move-result-object p1
                """
            )
        }

        // Filter feed entries in EntriesListPresenter.K
        EntriesListPresenterKFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->filterNewsfeedList(Ljava/util/List;)Ljava/util/List;
                    move-result-object p1
                """
            )
        }

        // Remove ads and filter stories containers in GetStoriesResponse
        GetStoriesResponseConstructorFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p3 }, $EXTENSION_CLASS->filterNewsfeedList(Ljava/util/List;)Ljava/util/List;
                    move-result-object p3
                    const/4 p4, 0x0
                """
            )
        }

        // Disable native ads enricher
        NewsfeedAdsEnricherFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Suppress VideoSnippetAttachment item display (ad card under videos)
        PostDisplayItemListExtKFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Filter promo/banner widgets from Services / Hub tab
        SuperAppAdapterSetItemsFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS->filterSuperAppWidgets(Ljava/util/List;)Ljava/util/List;
                    move-result-object p1
                """
            )
        }

        // Filter promo banners and ad blocks from VK Music and Catalog sections
        CatalogSectionConstructorFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p7 }, $EXTENSION_CLASS->filterCatalogBlocks(Ljava/util/List;)Ljava/util/List;
                    move-result-object p7
                    const/4 p13, 0x0
                """
            )
        }

        // Suppress product attach view insertion in clips
        ProductAttachesHolderImplDFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Suppress product attach data binding in clips
        ProductAttachesHolderImplEFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Suppress product attaches large view binding
        ProductAttachesLargeViewBinderFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Suppress product attaches tile view binding
        ProductAttachesTileViewBinderFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Suppress clip product attaches large view holder
        ClipProductAttachesLargeViewHolderFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Suppress clip market ads MVI view attaching to window
        MarketAdsItemMviViewOnAttachedFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Suppress clip market ads MVI view render
        MarketAdsItemMviViewSjFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Suppress shops more badge holder in clips
        ShopsMoreBadgeHolderFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }

        // Suppress top shops more badge holder in clips
        TopShopsMoreBadgeHolderFingerprint.method.apply {
            addInstructions(
                0,
                """
                    return-void
                """
            )
        }
    }
}

