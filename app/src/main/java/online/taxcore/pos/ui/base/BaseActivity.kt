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
        
        // Configure edge-to-edge display for modern Android versions
        configureEdgeToEdgeDisplay()
    }
    
    private fun configureEdgeToEdgeDisplay() {
        // For Android 10+ (API 29+), properly handle system bars and edge-to-edge display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Make the app draw behind the status bar and navigation bar
            window.decorView.systemUiVisibility = 
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            
            // Handle window insets for proper padding
            findViewById<android.view.View>(android.R.id.content)?.let { contentView ->
                ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    // Only apply bottom padding for navigation bar
                    // Let the status bar overlay the content (will be colored by theme)
                    view.setPadding(0, 0, 0, systemBars.bottom)
                    insets
                }
            }
        }
    }

    fun originalActivityContext(): Context {
        return originalContext ?: this
    }

}
