package com.example.mureev

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.mureev.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set tema yang memiliki ActionBar
        setTheme(MainActivity.currentThemeNav[MainActivity.themeIndex])
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Atur judul dan tombol kembali pada ActionBar bawaan
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Tentang Aplikasi"

        // Isi data ke setiap TextView
        binding.aboutVersion.text = "Versi ${BuildConfig.VERSION_NAME}"
        binding.aboutDev.text = "Developer: Arif Septiawardi"
        binding.aboutProject.text = "TA Proyek Integrasi Sistem"
    }

    // Fungsi untuk menangani klik tombol kembali di ActionBar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}
