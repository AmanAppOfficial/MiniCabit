package e.aman.minicabit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.storage.FirebaseStorage
import e.aman.minicabit.databinding.ActivityRegisterBinding
import e.aman.minicabit.models.User
import e.aman.minicabit.service.FirebaseMessaging
import e.aman.minicabit.utils.Constants
import e.aman.minicabit.utils.PermissionHandler

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var imageUrl = ""
    private var imagePath: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListener()
    }

    private fun setupListener() {
        binding.changeProfile.setOnClickListener{
            pickImageFromGallery()
        }

        binding.registerBtn.setOnClickListener{
            if(imagePath!=null)
                uploadImage()
            else
                saveInfo()
        }
    }

    private fun pickImageFromGallery() {
        /** check read storage permission **/
        val isGranted = PermissionHandler.checkPermissionForReadExternalStorage(this@RegisterActivity)
        if(!isGranted)
            PermissionHandler.requestPermissionForReadExternalStorage(this@RegisterActivity)
        else{
            /** pick image from gallery **/
            val imageIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI
            )
            intent.type = "image/*"
            pickImageResultLauncher.launch(imageIntent)
        }

    }

    /** get intent callback **/
    private var pickImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data;
            imagePath = imageUri!!.data!!
            binding.imageview.setImageURI(imagePath)
        }
    }

    /** upload image to storage **/
    private fun uploadImage() {
        var storageRef = FirebaseStorage.getInstance().reference.child(Constants.PROFILE_IMAGE_REF +
                FirebaseAuth.getInstance().currentUser!!.uid)

        storageRef.putFile(imagePath!!).addOnSuccessListener{
            storageRef.downloadUrl.addOnSuccessListener {
                imageUrl = it.toString()
                /** save data to database **/
                saveInfo()
            }
        }
    }

    /** save user info to database **/
    private fun saveInfo() {

        var user = createUser()
        if(user != null){
            val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
            val database = Firebase.database
            val driverRef = database.getReference(Constants.DRIVER_REF)

            /** set value to db**/
            driverRef.child(currentUserId).setValue(user).addOnCompleteListener{
                val mainIntent = Intent(this@RegisterActivity, DriverHomeActivity::class.java)
                startActivity(mainIntent)
                finish()
            }
        }

    }

    private fun createUser(): User? {
        var firstName = binding.firstnameText.text
        var lastName = binding.lastnameText.text
        var email = binding.emailText.text
        var phone = binding.phonenumberText.text
        if(firstName.isNullOrBlank() || lastName.isNullOrBlank() ||
                email.isNullOrBlank() || phone.isNullOrBlank()){
            Toast.makeText(applicationContext , getString(R.string.error_empty_fields),
                Toast.LENGTH_SHORT).show()
            return null
        }
        return User(firstName.toString(), lastName.toString(),
            phone.toString(), email.toString(), imageUrl , "")
    }

}