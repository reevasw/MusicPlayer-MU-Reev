package com.example.mureev

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.mureev.databinding.ActivityStatsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.io.File
import java.io.FileOutputStream

class UserActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser

    // Views
    private lateinit var edtEmail: TextView
    private lateinit var iconVerify: ImageView
    private lateinit var iconNotVerify: ImageView
    private lateinit var imgProfile: ImageView

    private lateinit var btnVerify: Button
    private lateinit var btnChangePass: Button

    // CardViews
    private lateinit var cvCurrentPass: CardView
    private lateinit var cvUpdatePass: CardView

    // Password fields
    private lateinit var edtCurrentPass: EditText
    private lateinit var edtNewPass: EditText
    private lateinit var edtConfirmPass: EditText

    // Buttons in password forms
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button
    private lateinit var btnNewCancel: Button
    private lateinit var btnNewChange: Button

    private val PICK_IMAGE_REQUEST = 1
    private val profileImageFileName = "profile_image.png"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.LightTheme)
        setContentView(R.layout.activity_user)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        // Init views
        edtEmail = findViewById(R.id.edt_email)
        iconVerify = findViewById(R.id.icon_verify)
        iconNotVerify = findViewById(R.id.icon_notVerify)
        imgProfile = findViewById(R.id.img_profile)

        btnVerify = findViewById(R.id.btn_verify)
        btnChangePass = findViewById(R.id.btn_changePass)

        cvCurrentPass = findViewById(R.id.cv_currentPass)
        cvUpdatePass = findViewById(R.id.cv_updatePass)

        edtCurrentPass = findViewById(R.id.edt_currentPassword)
        edtNewPass = findViewById(R.id.edt_newPass)
        edtConfirmPass = findViewById(R.id.edt_confirmPass)

        btnCancel = findViewById(R.id.btn_cancel)
        btnConfirm = findViewById(R.id.btn_confirm)
        btnNewCancel = findViewById(R.id.btn_newCancel)
        btnNewChange = findViewById(R.id.btn_newChange)

        loadUserInfo()
        loadProfileImage()

        imgProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnVerify.setOnClickListener {
            if (!currentUser.isEmailVerified) {
                currentUser.sendEmailVerification().addOnCompleteListener {
                    Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Email already verified", Toast.LENGTH_SHORT).show()
            }
        }

        btnChangePass.setOnClickListener {
            cvCurrentPass.visibility = View.VISIBLE
        }

        btnCancel.setOnClickListener {
            cvCurrentPass.visibility = View.GONE
        }

        btnConfirm.setOnClickListener {
            val currentPass = edtCurrentPass.text.toString()
            if (currentPass.isEmpty()) {
                edtCurrentPass.error = "Required"
                return@setOnClickListener
            }

            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(currentUser.email!!, currentPass)
            currentUser.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cvCurrentPass.visibility = View.GONE
                    cvUpdatePass.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnNewCancel.setOnClickListener {
            cvUpdatePass.visibility = View.GONE
        }

        btnNewChange.setOnClickListener {
            val newPass = edtNewPass.text.toString()
            val confirmPass = edtConfirmPass.text.toString()

            if (newPass != confirmPass) {
                edtConfirmPass.error = "Passwords don't match"
                return@setOnClickListener
            }

            currentUser.updatePassword(newPass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password changed", Toast.LENGTH_SHORT).show()
                    cvUpdatePass.visibility = View.GONE
                } else {
                    Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            imageUri?.let {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imgProfile.setImageBitmap(bitmap)
                saveProfileImage(bitmap)
            }
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveProfileImage(bitmap: Bitmap) {
        val file = File(filesDir, profileImageFileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }

    private fun loadProfileImage() {
        val file = File(filesDir, profileImageFileName)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imgProfile.setImageBitmap(bitmap)
        }
    }

    private fun loadUserInfo() {
        edtEmail.text = currentUser.email

        if (currentUser.isEmailVerified) {
            iconVerify.visibility = View.VISIBLE
            iconNotVerify.visibility = View.GONE
        } else {
            iconVerify.visibility = View.GONE
            iconNotVerify.visibility = View.VISIBLE
        }
    }
}