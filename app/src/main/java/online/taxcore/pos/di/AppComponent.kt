package online.taxcore.pos.di

import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import online.taxcore.pos.TaxCoreApp

@Component(
    modules = (arrayOf(
        AndroidInjectionModule::class,
        AppModule::class,
        ActivityBuilder::class
    ))
)
interface AppComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: TaxCoreApp): Builder
        fun build(): AppComponent
    }

    fun inject(app: TaxCoreApp)
}
