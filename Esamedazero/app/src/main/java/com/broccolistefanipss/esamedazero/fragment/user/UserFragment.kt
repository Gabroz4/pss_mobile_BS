package com.broccolistefanipss.esamedazero.fragment.user

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.broccolistefanipss.esamedazero.activity.EditUserActivity
import com.broccolistefanipss.esamedazero.databinding.FragmentUserBinding
import com.broccolistefanipss.esamedazero.global.DB
import com.broccolistefanipss.esamedazero.manager.LoginManager
import com.broccolistefanipss.esamedazero.manager.SessionManager
import com.broccolistefanipss.esamedazero.model.User


// TODO: associare immagine profilo all'utente e grafico con calorie per giorni


class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var editProfileLauncher: ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var database: DB
    private lateinit var loginManager: LoginManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sessionManager = SessionManager(requireContext())
        database = DB(requireContext())
        loginManager = LoginManager(requireContext())

        setupEditProfileLauncher()
        setupImagePicker()
        setupEditProfileButton()
        setupChangeProfilePictureButton()

        loadProfileImage()
        loadUserData()
        disconnect()

        return root
    }

    private fun setupEditProfileLauncher() {
        editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("UserFragment", "Modifiche ricevute correttamente")
                loadUserData()
            } else {
                Log.d("UserFragment", "Modifica fallita")
            }
        }
    }

    private fun setupImagePicker() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                imageUri = result.data?.data
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(imageUri!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.profilePicture.setImageBitmap(bitmap)
                    saveProfileImage(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Errore nel caricamento dell'immagine", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupEditProfileButton() {
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(context, EditUserActivity::class.java)
            editProfileLauncher.launch(intent)
        }
    }

    private fun setupChangeProfilePictureButton() {
        binding.changeProfilePictureButton.setOnClickListener {
            openGallery()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("UserFragment", "Caricamento dei dati utente")
        loadUserData()
    }

    private fun loadUserData() {
        val userName = sessionManager.userName ?: ""
        Log.d("UserFragment", "Caricamento dei dati di: $userName")
        val utente: User? = database.getUserData(userName)

        if (utente != null) {
            Log.d("UserFragment", "Dati di: $utente")
            binding.UserProfile.text = utente.userName
            binding.SexProfile.text = utente.sesso
            binding.AgeProfile.text = utente.eta.toString()
            binding.HeightProfile.text = utente.altezza.toString()
            binding.WeightProfile.text = utente.peso.toString()
            binding.objectiveProfile.text = utente.obiettivo
        } else {
            Log.e("UserFragment", "Nessun dato trovato di: $userName")
            Toast.makeText(requireContext(), "Username non trovato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun saveProfileImage(bitmap: Bitmap) {
        try {
            val filename = "profile_picture_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProfilePictures")
            }

            val imageUri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            imageUri?.let { uri ->
                requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
                saveProfileImageUri(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Errore nel salvataggio dell'immagine", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImageUri(uri: Uri) {
        //salvataggio uri dell'immagine nelle shared preferences
        val sharedPreferences = requireContext().getSharedPreferences("UserData", Activity.MODE_PRIVATE)
        sharedPreferences.edit().putString("profile_image_uri", uri.toString()).apply()
    }

    private fun loadProfileImage() {
        val sharedPreferences = requireContext().getSharedPreferences("UserData", Activity.MODE_PRIVATE)
        val uriString = sharedPreferences.getString("profile_image_uri", null)
        uriString?.let {
            imageUri = Uri.parse(it)
            binding.profilePicture.setImageURI(imageUri)
        }
    }
    private fun disconnect() {
        val btnDisconnect: Button = binding.btnDisconnect
        btnDisconnect.setOnClickListener {
            loginManager.logout()  // Effettua il logout dell'utente
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}