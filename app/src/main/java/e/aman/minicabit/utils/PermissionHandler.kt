package e.aman.minicabit.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat


object PermissionHandler {

    private const val READ_STORAGE_PERMISSION_REQUEST_CODE = 41

    fun checkPermissionForReadExternalStorage(context: Context): Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val result: Int = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            return result == PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    @Throws(Exception::class)
    fun requestPermissionForReadExternalStorage(context: Context) {
        try {
            ActivityCompat.requestPermissions(
                (context as Activity?)!!, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_STORAGE_PERMISSION_REQUEST_CODE
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}