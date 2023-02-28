package online.taxcore.pos.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import online.taxcore.pos.ui.catalog.CatalogActivity
import online.taxcore.pos.ui.catalog.CatalogDetailsActivity
import online.taxcore.pos.ui.catalog.ItemDetailActivity
import online.taxcore.pos.ui.dashboard.DashboardActivity
import online.taxcore.pos.ui.invoice.InvoiceActivity
import online.taxcore.pos.ui.journal.JournalActivity
import online.taxcore.pos.ui.journal.JournalDetailsActivity
import online.taxcore.pos.ui.settings.SettingsActivity
import online.taxcore.pos.ui.settings.SettingsDetailsActivity
import online.taxcore.pos.ui.splash.SplashActivity

@Module
abstract class ActivityBuilder {
    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindSplashActivity(): SplashActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindMainActivity(): InvoiceActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindDashboardActivity(): DashboardActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindPluDetailsActivity(): ItemDetailActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindCatalogActivity(): CatalogActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindCatalogDetailsActivity(): CatalogDetailsActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindJournalActivity(): JournalActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindJournalDetailsActivity(): JournalDetailsActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindSettingsDetailsActivity(): SettingsDetailsActivity

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindSettingsActivity(): SettingsActivity

}
