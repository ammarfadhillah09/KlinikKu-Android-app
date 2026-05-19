package com.example.klinikku.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.R
import com.example.klinikku.data.local.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LandingPageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)

        lifecycleScope.launch {
            delay(2500)
            val sessionManager = SessionManager(this@LandingPageActivity)
            if (sessionManager.isLoggedIn()) {
                val role = sessionManager.getRole()
                val nik = sessionManager.getNik()
                if (role == "admin") {
                    val intent = Intent(this@LandingPageActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                } else {
                    if (!nik.isNullOrEmpty()) {
                        val intent = Intent(this@LandingPageActivity, DetailPasienActivity::class.java).apply {
                            putExtra("NIK_PASIEN", nik)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    } else {
                        // Safe fallback if NIK is missing
                        val intent = Intent(this@LandingPageActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                }
            } else {
                val intent = Intent(this@LandingPageActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
            finish()
        }
    }
}
