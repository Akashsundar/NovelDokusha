package my.noveldokusha.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import my.noveldokusha.App
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.database.AppDatabase
import my.noveldokusha.data.database.AppDatabaseOperations
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.ScraperNetworkClient
import my.noveldokusha.repository.AppFileResolver
import my.noveldokusha.repository.BookChaptersRepository
import my.noveldokusha.repository.ChapterBodyRepository
import my.noveldokusha.repository.LibraryBooksRepository
import my.noveldokusha.repository.Repository
import my.noveldokusha.repository.ScraperRepository
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.ui.Toasty
import my.noveldokusha.ui.ToastyToast
import my.noveldokusha.ui.screens.reader.manager.ReaderManager
import my.noveldokusha.utils.NotificationsCenter
import java.io.File
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class AppModule {

    companion object {
        val mainDatabaseName = "bookEntry"

        @Provides
        @Singleton
        fun providesApp(@ApplicationContext context: Context): App {
            return context as App
        }

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.createRoom(context, mainDatabaseName)
        }

        @Provides
        @Singleton
        fun provideRepository(
            database: AppDatabase,
            @ApplicationContext context: Context,
            libraryBooksRepository: LibraryBooksRepository,
            bookChaptersRepository: BookChaptersRepository,
            chapterBodyRepository: ChapterBodyRepository,
            appFileResolver: AppFileResolver,
        ): Repository {
            return Repository(
                database,
                context,
                mainDatabaseName,
                libraryBooksRepository,
                bookChaptersRepository,
                chapterBodyRepository,
                appFileResolver
            )
        }

        @Provides
        @Singleton
        fun provideAppDatabaseOperations(database: AppDatabase): AppDatabaseOperations {
            return database
        }

        @Provides
        @Singleton
        fun provideAppFileResolver(@ApplicationContext context: Context): AppFileResolver {
            return AppFileResolver(context = context)
        }

        @Provides
        @Singleton
        fun provideLibraryBooksRepository(database: AppDatabase): LibraryBooksRepository {
            return LibraryBooksRepository(libraryDao = database.libraryDao(), database)
        }

        @Provides
        @Singleton
        fun provideChapterBooksRepository(database: AppDatabase): BookChaptersRepository {
            return BookChaptersRepository(chapterDao = database.chapterDao(), database)
        }

        @Provides
        @Singleton
        fun provideChapterBodyRepository(
            database: AppDatabase,
            networkClient: NetworkClient,
            scraper: Scraper,
            bookChaptersRepository: BookChaptersRepository,

            ): ChapterBodyRepository {
            return ChapterBodyRepository(
                chapterBodyDao = database.chapterBodyDao(),
                networkClient = networkClient,
                scraper = scraper,
                bookChaptersRepository = bookChaptersRepository,
                operations = database
            )
        }

        @Provides
        @Singleton
        fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
            return AppPreferences(context)
        }

        @Provides
        @Singleton
        fun provideAppCoroutineScope(): AppCoroutineScope {
            return object : AppCoroutineScope {
                override val coroutineContext =
                    SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("App")
            }
        }

        @Provides
        @Singleton
        fun provideNetworkClient(app: App, @ApplicationContext appContext: Context): NetworkClient {
            return ScraperNetworkClient(
                cacheDir = File(app.cacheDir, "network_cache"),
                cacheSize = 5L * 1024 * 1024,
                appContext = appContext
            )
        }


        @Provides
        @Singleton
        fun provideReaderManager(
            repository: Repository,
            translationManager: TranslationManager,
            appPreferences: AppPreferences,
            appCoroutineScope: AppCoroutineScope,
            @ApplicationContext context: Context
        ): ReaderManager {
            return ReaderManager(
                repository,
                translationManager,
                appPreferences,
                context,
                appScope = appCoroutineScope
            )
        }

        @Provides
        @Singleton
        fun provideToasty(@ApplicationContext context: Context): Toasty {
            return ToastyToast(applicationContext = context)
        }

        @Provides
        @Singleton
        fun provideScraperRepository(
            appPreferences: AppPreferences, scraper: Scraper
        ): ScraperRepository {
            return ScraperRepository(appPreferences, scraper)
        }

        @Provides
        @Singleton
        fun provideNotificationCenter(
            @ApplicationContext context: Context,
        ): NotificationsCenter {
            return NotificationsCenter(context)
        }
    }
}