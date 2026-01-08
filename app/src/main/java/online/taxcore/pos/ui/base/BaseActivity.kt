package online.taxcore.pos.ui.base

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import online.taxcore.pos.data.PrefService
import online.taxcore.pos.utils.CtxUtils
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), HasSupportFragmentInjector {

    private var originalContext: Context? = null

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector
    }

    override fun attachBaseContext(newBase: Context?) {
        originalContext = newBase

        newBase?.let {
            // get chosen language from shread preference
            val localeToSwitchTo = PrefService(it).loadLocale()
            val localeUpdatedContext: ContextWrapper =
                CtxUtils.updateLocale(newBase, localeToSwitchTo)

//            super.attachBaseContext(ViewPumpContextWrapper.wrap(it))
            super.attachBaseContext(localeUpdatedContext)

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        // Configure display cutout handling for devices with notches/camera holes
        configureDisplayCutoutHandling()
    }

    private fun configureDisplayCutoutHandling() {
        // For Android P+ (API 28+), allow content to extend into display cutout area
        // This works together with fitsSystemWindows="true" in layouts to properly
        // position content below the status bar and cutouts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    fun originalActivityContext(): Context {
        return originalContext ?: this
    }

}
