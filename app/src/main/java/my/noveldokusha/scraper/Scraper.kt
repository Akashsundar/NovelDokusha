package my.noveldokusha.scraper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.repository.AppFileResolver
import my.noveldokusha.scraper.databases.BakaUpdates
import my.noveldokusha.scraper.databases.NovelUpdates
import my.noveldokusha.scraper.sources.AT
import my.noveldokusha.scraper.sources.BestLightNovel
import my.noveldokusha.scraper.sources.BoxNovel
import my.noveldokusha.scraper.sources.KoreanNovelsMTL
import my.noveldokusha.scraper.sources.LightNovelWorld
import my.noveldokusha.scraper.sources.LightNovelsTranslations
import my.noveldokusha.scraper.sources.LocalSource
import my.noveldokusha.scraper.sources.MTLNovel
import my.noveldokusha.scraper.sources.NovelHall
import my.noveldokusha.scraper.sources.ReadLightNovel
import my.noveldokusha.scraper.sources.ReadNovelFull
import my.noveldokusha.scraper.sources.Reddit
import my.noveldokusha.scraper.sources.Saikai
import my.noveldokusha.scraper.sources.Sousetsuka
import my.noveldokusha.scraper.sources.Wuxia
import my.noveldokusha.scraper.sources.WuxiaWorld
import my.noveldokusha.scraper.sources._1stKissNovel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Scraper @Inject constructor(
    networkClient: NetworkClient,
    @ApplicationContext appContext: Context,
    localSourcesDirectories: LocalSourcesDirectories,
    appFileResolver: AppFileResolver,
) {
    val databasesList = setOf<DatabaseInterface>(
        NovelUpdates(networkClient),
        BakaUpdates(networkClient)
    )

    val sourcesList = setOf<SourceInterface>(
        LocalSource(appContext, localSourcesDirectories, appFileResolver),
        LightNovelsTranslations(networkClient),
        ReadLightNovel(networkClient),
        ReadNovelFull(networkClient),
        my.noveldokusha.scraper.sources.NovelUpdates(networkClient),
        Reddit(networkClient),
        AT(networkClient),
        Wuxia(networkClient),
        BestLightNovel(networkClient),
        _1stKissNovel(networkClient),
        Sousetsuka(networkClient),
        Saikai(networkClient),
        BoxNovel(networkClient),
        LightNovelWorld(networkClient),
        NovelHall(networkClient),
        MTLNovel(networkClient),
        WuxiaWorld(networkClient),
        KoreanNovelsMTL(networkClient),
    )

    val sourcesCatalogsList = sourcesList.filterIsInstance<SourceInterface.Catalog>()
    val sourcesCatalogsLanguagesList = sourcesCatalogsList.mapNotNull { it.language }.toSet()

    private fun String.isCompatibleWithBaseUrl(baseUrl: String): Boolean {
        val normalizedUrl = if (this.endsWith("/")) this else "$this/"
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return normalizedUrl.startsWith(normalizedBaseUrl)
    }

    fun getCompatibleSource(url: String): SourceInterface? =
        sourcesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleSourceCatalog(url: String): SourceInterface.Catalog? =
        sourcesCatalogsList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }

    fun getCompatibleDatabase(url: String): DatabaseInterface? =
        databasesList.find { url.isCompatibleWithBaseUrl(it.baseUrl) }
}
