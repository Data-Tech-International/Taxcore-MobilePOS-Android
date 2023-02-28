package online.taxcore.pos.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import online.taxcore.pos.ui.catalog.CatalogDashFragment
import online.taxcore.pos.ui.catalog.CatalogListFragment
import online.taxcore.pos.ui.invoice.InvoiceFragment
import online.taxcore.pos.ui.journal.JournalDashFragment
import online.taxcore.pos.ui.settings.SettingsDashFragment
import online.taxcore.pos.ui.settings.about.AboutFragment
import online.taxcore.pos.ui.settings.server.SDCConfigureFragment
import online.taxcore.pos.ui.settings.server.SDCServerFragment
import online.taxcore.pos.ui.settings.taxes.TaxesFragment

@Module
abstract class MainActivityModule {

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun bindCatalogDashFragment(): CatalogDashFragment

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun bindJournalDashFragment(): JournalDashFragment

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun bindCatalogListFragment(): CatalogListFragment

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun bindInvoiceFragment(): InvoiceFragment

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun bindAboutFragment(): AboutFragment

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun bindSDCConfigureFragment(): SDCConfigureFragment

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun bindSDCServerFragment(): SDCServerFragment

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun bindTaxesFragment(): TaxesFragment

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun bindSettingsDashFragment(): SettingsDashFragment

}
