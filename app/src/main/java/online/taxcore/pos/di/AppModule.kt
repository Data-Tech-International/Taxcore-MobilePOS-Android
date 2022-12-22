package online.taxcore.pos.di

import android.content.Context
import android.content.SharedPreferences
import com.pawegio.kandroid.defaultSharedPreferences
import dagger.Module
import dagger.Provides
import online.taxcore.pos.TaxCoreApp
import online.taxcore.pos.data.PrefService

@Module
class AppModule {

    @Provides
    fun provideApplication(app: TaxCoreApp): Context = app

    @Provides
    fun provideSharedPreferences(application: TaxCoreApp): SharedPreferences =
        application.defaultSharedPreferences

    @Provides
    fun providePrefService(context: Context): PrefService = PrefService(context)
}
