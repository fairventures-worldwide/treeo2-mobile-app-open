package org.treeo.treeo.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.treeo.treeo.db.TreeoDatabase
import org.treeo.treeo.db.dao.ActivityDao
import org.treeo.treeo.db.dao.LandSurveyDao
import org.treeo.treeo.db.dao.TreeMeasurementDao
import org.treeo.treeo.db.models.mappers.ActivityDtoToEntityMapper
import org.treeo.treeo.models.mappers.QuestionnaireWithPagesMapper
import org.treeo.treeo.network.AccessTokenInterceptor
import org.treeo.treeo.network.ApiService
import org.treeo.treeo.network.RequestManager
import org.treeo.treeo.repositories.DBMainRepository
import org.treeo.treeo.repositories.IMainRepository
import org.treeo.treeo.repositories.MainRepository
import org.treeo.treeo.util.*
import org.treeo.treeo.util.mappers.DtoEntityMapper
import org.treeo.treeo.util.mappers.ModelDtoMapper
import org.treeo.treeo.util.mappers.ModelEntityMapper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun providesDeviceInfoUtils(
        @ApplicationContext context: Context,
        dispatcher: IDispatcherProvider
    ) = DeviceInfoUtils(context, dispatcher)

    @Singleton
    @Provides
    fun providesDispatcherProvider() =
        DefaultDispatcherProvider() as IDispatcherProvider

    @Singleton
    @Provides
    fun providesRequestManager(apiService: ApiService) =
        RequestManager(apiService)

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext applicationContext: Context): SharedPreferences {
        return applicationContext.getSharedPreferences(
            SHARED_PREFERENCES_NAME,
            MODE_PRIVATE
        )
    }

    @Singleton
    @Provides
    fun providesNetworkInterceptor(@ApplicationContext context: Context) =
        NetworkConnectionInterceptor(context)


    @Singleton
    @Provides
    fun providesAuthTokenInterceptor(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences
    ) = AccessTokenInterceptor(context, buildTokenApi(), sharedPreferences)

    @Singleton
    @Provides
    fun providesHttpClient(
        networkInterceptor: NetworkConnectionInterceptor,
        accessTokenInterceptor: AccessTokenInterceptor
    ): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .addInterceptor(accessTokenInterceptor)
            .addInterceptor(networkInterceptor)
            .addInterceptor(interceptor)
            .build()
    }

    @Singleton
    @Provides
    fun providesRetrofit(client: OkHttpClient): ApiService {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .client(client)
            .build()
        return retrofit.create(ApiService::class.java)
    }

    @Singleton
    @Provides
    fun providesMainRepository(
        requestManager: RequestManager,
        preferences: SharedPreferences
    ) = MainRepository(requestManager, preferences) as IMainRepository

    @Singleton
    @Provides
    fun providesTreeoDatabase(@ApplicationContext applicationContext: Context): TreeoDatabase {
        return Room.databaseBuilder(
            applicationContext,
            TreeoDatabase::class.java,
            TREEO_DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Singleton
    @Provides
    fun providesActivityDao(database: TreeoDatabase) =
        database.getActivityDao()

    @Singleton
    @Provides
    fun providesLandSurveyDao(database: TreeoDatabase) =
        database.getLandSurveyDao()

    @Singleton
    @Provides
    fun providesTreeMeasurementDao(database: TreeoDatabase) =
        database.getTreeMeasurementDao()

    @Singleton
    @Provides
    fun providesDBMainRepository(
        activityDao: ActivityDao,
        landSurveyDao: LandSurveyDao,
        tmDao: TreeMeasurementDao,
        mapper: ActivityDtoToEntityMapper,
        @ApplicationContext context: Context,
        modelEntityMapper: ModelEntityMapper,
        preferences: SharedPreferences,
        dispatcher: IDispatcherProvider
    ) = DBMainRepository(
        activityDao,
        landSurveyDao,
        tmDao,
        mapper,
        context,
        modelEntityMapper,
        preferences,
        dispatcher
    )

    @Singleton
    @Provides
    fun providesActivityDtoToEntityMapper() = ActivityDtoToEntityMapper()

    @Singleton
    @Provides
    fun providesQuestionnaireWithPagesMapper() = QuestionnaireWithPagesMapper()

    @Singleton
    @Provides
    fun providesDtoEntityMapper() = DtoEntityMapper()

    @Singleton
    @Provides
    fun providesModelDtoMapper() = ModelDtoMapper()

    @Singleton
    @Provides
    fun providesModelEntityMapper() = ModelEntityMapper()

    @Singleton
    @Provides
    fun providesConnectivityMonitor(@ApplicationContext context: Context) =
        ConnectivityMonitor(context)


    private fun buildTokenApi(): ApiService {
        val httpClient = OkHttpClient.Builder()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

}
