package e.aman.minicabit.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import e.aman.minicabit.R
import e.aman.minicabit.models.TokenModel
import e.aman.minicabit.utils.Constants

class FirebaseMessaging: FirebaseMessagingService() {

    /** only triggered when app in foreground **/
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data: Map<String, String> = message.data
        NotificationService.showNotification(data , this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToDatabase(token)
    }

    /** save token to database **/
    fun saveTokenToDatabase(token: String) {
        if(FirebaseAuth.getInstance().currentUser!=null){
            val tokenModel = TokenModel(token)
            FirebaseDatabase.getInstance().getReference(Constants.TOKEN_REF)
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .setValue(tokenModel)
        }
    }

}