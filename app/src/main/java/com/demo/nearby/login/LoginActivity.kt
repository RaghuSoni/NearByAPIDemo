package com.demo.nearby.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.demo.nearby.MainActivity
import com.demo.nearby.Util
import com.demo.nearby.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

import java.util.*

class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding
    companion object {
        private const val TAG: String = "LoginActivity"
        const val KEY_USERNAME = "username"
        const val KEY_USER_UUID = "user-uuid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        Util.clearSharedPreferences(this)

        binding.usernameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else {
                false
            }
        }

        binding.loginButton.setOnClickListener { login() }
       }

    private fun login() {
        binding.loginContainerView.requestFocus()
        Util.hideKeyboard(this)

        if (Util.isConnected(this@LoginActivity)) {
            binding.loginButton.isEnabled = false

            var username = binding.usernameInput.text.toString()
            if (username.isEmpty()) username = "Anonymous"

            val userUUID = UUID.randomUUID().toString()

            Util.getSharedPreferences(this).edit {
                putString(KEY_USER_UUID, userUUID)
                putString(KEY_USERNAME, username)
            }

            Log.i(TAG, "Logging in user.")
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(KEY_USERNAME, username)
                    .putExtra(KEY_USER_UUID, userUUID)
            startActivity(intent)
        } else
            Snackbar.make(binding.container, "No Internet Connection", Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()

        binding.loginButton.isEnabled = true
    }
}
