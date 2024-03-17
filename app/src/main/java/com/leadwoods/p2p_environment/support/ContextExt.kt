package com.leadwoods.p2p_environment.support

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.Manifest.permission.CHANGE_NETWORK_STATE
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.Manifest.permission.INTERNET
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.leadwoods.p2p_environment.activities.SecondaryActivity

val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, ACCESS_NETWORK_STATE, CHANGE_NETWORK_STATE, INTERNET, NEARBY_WIFI_DEVICES)
else
    arrayOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, ACCESS_NETWORK_STATE, CHANGE_NETWORK_STATE, INTERNET)

class PermissionDialog(private val activity: Activity, private val permissionName: String, private val permissions: Array<String>): DialogFragment(){

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Logger.f()
        return AlertDialog.Builder(activity).apply {
            setMessage("$permissionName is required")
            setPositiveButton("Accept") { dialog, _ ->
                ActivityCompat.requestPermissions(
                    activity, permissions, SecondaryActivity.PERMISSION_REQUEST_CODE
                )
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { _, _ ->
            }
            setCancelable(false)
        }.create()
    }
}

fun Context.checkAllPermissions(): Boolean {
    Logger.f()
//    val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//        arrayOf(CHANGE_WIFI_STATE, ACCESS_WIFI_STATE, NEARBY_WIFI_DEVICES, CHANGE_NETWORK_STATE, ACCESS_NETWORK_STATE, INTERNET)
//    else
//        arrayOf(CHANGE_WIFI_STATE, ACCESS_WIFI_STATE, CHANGE_NETWORK_STATE, ACCESS_NETWORK_STATE, INTERNET)

    permissions.forEach {
        if(ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
            Logger.w("$it: not granted")
            return false
        }
    }

    return true
}

fun requestAllPermissions(activity: Activity, fragmentManager: FragmentManager) {
    Logger.f()


    permissions.forEach {
        PermissionDialog(
            activity, it.split(".").last(),
            arrayOf(it)
        ).show(fragmentManager, "Permission Request")
    }
}

fun Context.permissionsOverview(): Triple<String, Int, Int>{
    Logger.f()
//    val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
//        arrayOf(CHANGE_WIFI_STATE, ACCESS_WIFI_STATE, NEARBY_WIFI_DEVICES, CHANGE_NETWORK_STATE, ACCESS_NETWORK_STATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
//    else
//        arrayOf(CHANGE_WIFI_STATE, ACCESS_WIFI_STATE, CHANGE_NETWORK_STATE, ACCESS_NETWORK_STATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)

    var notGranted = permissions.size

    return Triple(StringBuilder().apply {
        append("Permissions Status:\n")

        for (permission in permissions) {
            append(
                permission.split(".").last() +
                if (ContextCompat.checkSelfPermission(this@permissionsOverview, permission) == PackageManager.PERMISSION_GRANTED) {
                    notGranted -= 1
                    ": granted\n"
                }
                else
                    ": not granted\n"
            )
        }
    }.toString(), permissions.size - notGranted, permissions.size)

}