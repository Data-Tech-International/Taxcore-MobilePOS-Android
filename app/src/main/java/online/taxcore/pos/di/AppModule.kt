package online.taxcore.pos.di

import android.content.Context
import android.content.SharedPreferences
import com.pawegio.kandroid.defaultSharedPreferences
import dagger.Module
import dagger.Provides
import online.taxcore.pos.TaxCoreApp

@Module
class AppModule {

    @Provides
    fun provideApplication(app: TaxCoreApp): Context = app

    @Provides
    fun provideSharedPreferences(application: TaxCoreApp): SharedPreferences =
        application.defaultSharedPreferences
}
