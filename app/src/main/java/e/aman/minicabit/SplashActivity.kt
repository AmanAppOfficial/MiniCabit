package e.aman.minicabit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import e.aman.minicabit.utils.Constants
import java.util.*

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()
    }

    /** move to LoginScreen after x seconds...**/
    private fun moveToLogin(){
        Timer().schedule(object : TimerTask() {
            override fun run() {
                val loginIntent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(loginIntent)
                finish()
            }
        }, 3000)
    }

    private fun moveToMainActivity(){
        Timer().schedule(object : TimerTask() {
            override fun run() {
                val mainIntent = Intent(this@SplashActivity, DriverHomeActivity::class.java)
                startActivity(mainIntent)
                finish()
            }
        }, 3000)

    }

    override fun onStart() {
        super.onStart()
        /** check user logged in or not...**/
        if(auth.currentUser!=null)
            checkUserInfoAvailable()
        else
            moveToLogin()
    }

    /** check user details present in database **/
    private fun checkUserInfoAvailable(){
        val currentUserId = auth.currentUser!!.uid
        val database = Firebase.database
        val driverRef = database.getReference(Constants.DRIVER_REF)

        driverRef.child(currentUserId).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    moveToMainActivity()
                }
                else{
                    moveToRegisterActivity()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext , error.message , Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun moveToRegisterActivity() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                val registerIntent = Intent(this@SplashActivity, RegisterActivity::class.java)
                startActivity(registerIntent)
                finish()
            }
        }, 3000)

    }

}