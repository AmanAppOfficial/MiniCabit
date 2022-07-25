package e.aman.minicabit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import e.aman.minicabit.databinding.ActivityDriverHomeBinding
import e.aman.minicabit.utils.Constants


class DriverHomeActivity : AppCompatActivity(){

    private lateinit var appBarConfig: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var binding: ActivityDriverHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // set up drawer with navigation component...
        navController = findNavController(R.id.nav_host_fragment_container)
        appBarConfig = AppBarConfiguration(navController.graph , binding.drawerLayout)
        binding.navigationView.setupWithNavController(navController)
        setupActionBarWithNavController(navController , appBarConfig)

        /*** logout implementation */
        binding.navigationView.menu.findItem(R.id.logout).setOnMenuItemClickListener {
            val loginIntent = Intent(this,LoginActivity::class.java)
            FirebaseAuth.getInstance().signOut()
            startActivity(loginIntent)
            finish()
            return@setOnMenuItemClickListener true
        }
        setTextToDrawerHeader()

    }

    /** set text from firebase database **/
    private fun setTextToDrawerHeader() {

        var headerView = binding.navigationView.getHeaderView(0)
        var nameTextView = headerView.findViewById<TextView>(R.id.name_text)
        val imageView = headerView.findViewById<ImageView>(R.id.image)
        val currUid = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance().getReference(Constants.DRIVER_REF).child(currUid)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.child(Constants.DATABASE_FIRST_NAME).exists()){
                        val name = snapshot.child(Constants.DATABASE_FIRST_NAME).value.toString()
                        nameTextView.text = name
                    }
                    if(snapshot.child(Constants.DATABASE_IMAGE).exists()){
                        if(snapshot.child(Constants.DATABASE_IMAGE).value.toString() == ""){
                        }
                        else{
                            val image = snapshot.child(Constants.DATABASE_IMAGE).value.toString()
                            Glide.with(this@DriverHomeActivity).load(image).into(imageView)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(applicationContext , getString(R.string.error_authentication_failed), Toast.LENGTH_SHORT).show()
                }

            })

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfig) ||  super.onSupportNavigateUp()
    }

}