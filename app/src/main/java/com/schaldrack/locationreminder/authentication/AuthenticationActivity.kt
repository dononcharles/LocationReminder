package com.schaldrack.locationreminder.authentication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.schaldrack.locationreminder.R
import com.schaldrack.locationreminder.databinding.ActivityAuthenticationBinding
import com.schaldrack.locationreminder.locationreminders.RemindersActivity
import com.schaldrack.locationreminder.utils.DataStoreManager
import com.schaldrack.locationreminder.utils.DataStoreManager.Companion.PREF_KEY_DISPLAY_NAME
import com.schaldrack.locationreminder.utils.DataStoreManager.Companion.PREF_KEY_IS_USER_CONNECTED
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAuthenticationBinding.inflate(layoutInflater) }
    private val dataStoreManager by inject<DataStoreManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.authButton.setOnClickListener { launchSignInFlow() }

        // If the user was authenticated, send him to RemindersActivity
        lifecycleScope.launch {
            if (dataStoreManager.isUserConnected.first()) {
                gotoRemindersActivity()
            }
        }
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())

        val customLayout = AuthMethodPickerLayout.Builder(R.layout.custom_login_view)
            .setGoogleButtonId(R.id.signWithGoogle)
            .setEmailButtonId(R.id.signWithEmail)
            // .setTosAndPrivacyPolicyId(R.id.policyLink)
            .build()

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(customLayout)
            .setAvailableProviders(providers)
            .setTheme(R.style.Theme_LocationReminder)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private val signInLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) { res ->
        this.onSignInResult(res)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            lifecycleScope.launch {
                val user = FirebaseAuth.getInstance().currentUser
                dataStoreManager.saveBooleanDataStore(PREF_KEY_IS_USER_CONNECTED, true)
                dataStoreManager.saveStringDataStore(PREF_KEY_DISPLAY_NAME, user?.displayName ?: "")
                gotoRemindersActivity()
            }
        } else {
            Snackbar.make(binding.root, "Sign in failed. Error code : ${response?.error?.errorCode ?: ""}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun gotoRemindersActivity() {
        startActivity(Intent(this@AuthenticationActivity, RemindersActivity::class.java))
    }
}
