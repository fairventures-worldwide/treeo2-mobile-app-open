package org.treeo.treeo.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.akexorcist.localizationactivity.core.LocalizationActivityDelegate
import com.akexorcist.localizationactivity.core.OnLocaleChangedListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import org.treeo.treeo.R
import org.treeo.treeo.ui.authentication.AuthViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnLocaleChangedListener {

    @Inject
    lateinit var preferences: SharedPreferences

    private val viewModel: AuthViewModel by viewModels()

    private val localizationDelegate by lazy {
        LocalizationActivityDelegate(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        localizationDelegate.addOnLocaleChangedListener(this)
        localizationDelegate.onCreate()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpNavigation()
        checkUserStatus()

        homeBottomNavigationView.background = null
        homeBottomNavigationView.menu.getItem(2).isEnabled = false
        setUpFab()
        setObservers()
    }

    private fun setObservers() {
        viewModel.localeLanguage.observe(this) {
            setLanguage(it)
        }
    }

    private fun setUpNavigation() {
        homeBottomNavigationView.setupWithNavController(findNavController())
        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    showBottomBar()
                }
                R.id.guideFragment -> {
                    showBottomBar()
                }
                R.id.learnFragment -> {
                    showBottomBar()
                }
                R.id.profileFragment -> {
                    showBottomBar()
                }
                R.id.cameraFragment -> {
                    hideBottomBar()
                }
                else -> {
                    hideBottomBar()
                }
            }
        }
    }

    private fun setUpFab() {
        homeTakePhotoButton.setOnClickListener {
            findNavController().navigate(R.id.bottomSheetDialog)
        }
    }

    private fun hideBottomBar() {
        homeBottomAppBar.visibility = View.GONE
        homeBottomNavigationView.visibility = View.GONE
        homeTakePhotoButton.visibility = View.GONE
    }

    private fun showBottomBar() {
        homeBottomAppBar.visibility = View.VISIBLE
        homeBottomNavigationView.visibility = View.VISIBLE
        homeTakePhotoButton.visibility = View.VISIBLE
    }

    private fun findNavController(): NavController {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp()
    }

    private fun firstTimeUserFlagExists(): Boolean {
        return preferences.contains(getString(R.string.first_time_user))
    }

    private fun isFirstTimeUser(): Boolean {
        return preferences.getBoolean(getString(R.string.first_time_user), true)
    }

    private fun getUserToken(): String? {
        return preferences.getString(getString(R.string.user_token), null)
    }

    private fun checkUserStatus() {
        if (firstTimeUserFlagExists() && !isFirstTimeUser()) {
            navigateToLoginOrHome()
        } else {
            navigateToOnBoarding()
        }
    }

    private fun navigateToLoginOrHome() {
        if (getUserToken().isNullOrEmpty() || getUserToken().isNullOrBlank()) {
            navigateToLogin()
        } else {
            navigateToHome()
        }
    }

    private fun navigateToOnBoarding() {
        if (findNavController().currentDestination?.id != R.id.chooseLangFragment) {
            findNavController().navigate(R.id.action_homeFragment_to_chooseLangFragment)
        }
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.registrationHostFragment)
    }

    private fun navigateToHome() {
        val startDestination = findNavController().graph.startDestination
        val navOptions = NavOptions.Builder()
            .setPopUpTo(startDestination, true)
            .build()
        findNavController().navigate(startDestination, null, navOptions)
    }

    public override fun onResume() {
        super.onResume()
        localizationDelegate.onResume(this)
    }

    override fun attachBaseContext(newBase: Context) {
        applyOverrideConfiguration(localizationDelegate.updateConfigurationLocale(newBase))
        super.attachBaseContext(newBase)
    }

    override fun getApplicationContext(): Context {
        return localizationDelegate.getApplicationContext(super.getApplicationContext())
    }

    override fun getResources(): Resources {
        return localizationDelegate.getResources(super.getResources())
    }

    private fun setLanguage(language: String?) {
        localizationDelegate.setLanguage(this, language!!)
    }

    // Just override method locale change event
    override fun onBeforeLocaleChanged() {}

    override fun onAfterLocaleChanged() {}

    fun navigateToPage(pageId: Int) {
        homeBottomNavigationView.selectedItemId = pageId
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val screenToShow = data?.extras?.getString("screenToShow", "home")
        if (resultCode == Activity.RESULT_OK && screenToShow == "profile") {
            navigateToPage(R.id.profileFragment)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("opencv_java4")
        }
    }
}
