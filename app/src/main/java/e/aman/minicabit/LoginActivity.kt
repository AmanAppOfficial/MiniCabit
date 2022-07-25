package e.aman.minicabit

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import e.aman.minicabit.databinding.ActivityLoginBinding
import e.aman.minicabit.databinding.OtpDialogBinding
import e.aman.minicabit.databinding.PhoneNumberDialogBinding
import e.aman.minicabit.utils.Constants
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var verificationId: String
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mSignInClient: GoogleSignInClient
    private lateinit var database: FirebaseDatabase
    private lateinit var driverRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.visibility = View.GONE
        firebaseAuth = FirebaseAuth.getInstance()
        setupListener()
    }


    private fun setupListener() {

        binding.googleSigninBtn.setOnClickListener{
            binding.progressBar.visibility = View.VISIBLE
            googleSignIn()
        }
        binding.phoneSigininBtn.setOnClickListener{
            binding.progressBar.visibility = View.VISIBLE
            /** open phone number dialog**/
            phoneNumberDialog()

        }

    }

    /** send OTP **/
    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(phoneCallback) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

    }


    /** verify otp with code **/
    private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }


    /** phone number sign in callbacks **/
    private val phoneCallback = object: PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Toast.makeText(applicationContext, e.message.toString(), Toast.LENGTH_LONG).show();
            binding.progressBar.visibility = View.GONE
        }

        override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(p0, p1)
            verificationId = p0
        }
    }

    /** final sign in using otp **/
    private fun signInWithCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkUserInfoAvailable()
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(applicationContext, getString(R.string.error_invalid_otp), Toast.LENGTH_LONG).show()
                }
            }
    }


    private fun googleSignIn() {

        /** initialize google sign in options **/
        val googleSignInOptions: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mSignInClient = GoogleSignIn.getClient(this,googleSignInOptions)

        /** launch sign in intent **/
        val signInIntent = mSignInClient.signInIntent
        googleResultLauncher.launch(signInIntent)


    }

    /** get intent callback **/
    private var googleResultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            val data: Intent? = result.data
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                binding.progressBar.visibility = View.GONE
            }

        }
    }

    /** save to firebase **/
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener(this) {
                checkUserInfoAvailable()
            }
            .addOnFailureListener(this) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, getString(R.string.error_authentication_failed), Toast.LENGTH_SHORT).show()
            }
    }

    /** dialog for phone number **/
    private fun phoneNumberDialog(){
        val builder = AlertDialog.Builder(this,R.style.DialogTheme)
        builder.setTitle(getString(R.string.phone_number_input))
        builder.setCancelable(false)
        val view = PhoneNumberDialogBinding.inflate(layoutInflater)
        builder.setView(view.root)

        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
                val phoneNumber = view.phoneNumberText.text
                if(!phoneNumber.isNullOrBlank()){
                    if(phoneNumber.toString().trim().length <= 10){
                        Toast.makeText(applicationContext , "Country Code missing" , Toast.LENGTH_SHORT).show()
                        phoneNumberDialog()
                    }
                    else{
                        sendVerificationCode("+" + phoneNumber!!.toString().trim())
                        /** open otp dialog **/
                        otpDialog()
                    }
                }
            else{
                    binding.progressBar.visibility = View.GONE
                }

            }

        builder.setNegativeButton(getString(R.string.cancel)){dialog , _ ->
            binding.progressBar.visibility = View.GONE
                 dialog.cancel()
        }
        builder.create().show()
    }

    /** dialog for OTP **/
    private fun otpDialog(){
        val builder = AlertDialog.Builder(this,R.style.DialogTheme)
        builder.setTitle(getString(R.string.otp_input))
        builder.setCancelable(false)
        val view = OtpDialogBinding.inflate(layoutInflater)
        builder.setView(view.root)

        builder.setPositiveButton(getString(R.string.ok)) { _, _ ->
            val otp = view.otpText.text
            if(!otp.isNullOrBlank()){
                verifyPhoneNumberWithCode(verificationId , otp.toString().trim())
            }
            else{
                binding.progressBar.visibility = View.GONE
            }
        }

        builder.setNegativeButton(getString(R.string.cancel)){dialog , _ ->
            binding.progressBar.visibility = View.GONE
            dialog.cancel()
        }
        builder.create().show()
    }

    /** check if user info available in database **/
    private fun checkUserInfoAvailable(){
        val currentUserId = firebaseAuth.currentUser!!.uid
        database = Firebase.database
        driverRef = database.getReference(Constants.DRIVER_REF)

        driverRef.child(currentUserId).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.progressBar.visibility = View.GONE
                if(snapshot.exists())
                    moveToMainActivity()
                else
                    moveToRegisterActivity()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext , error.message , Toast.LENGTH_SHORT).show()
            }

        })

    }

    private fun moveToRegisterActivity() {
        val registerIntent = Intent(this@LoginActivity, RegisterActivity::class.java)
        startActivity(registerIntent)
        finish()
    }

    private fun moveToMainActivity(){
        val mainIntent = Intent(this@LoginActivity, DriverHomeActivity::class.java)
        startActivity(mainIntent)
        finish()
    }

}