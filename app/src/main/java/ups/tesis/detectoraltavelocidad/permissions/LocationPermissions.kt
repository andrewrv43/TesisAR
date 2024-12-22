package ups.tesis.detectoraltavelocidad.permissions

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ups.tesis.detectoraltavelocidad.R

class LocationPermissions (private val activity: Activity, private val permissionCallback: PermissionCallback) {
    interface PermissionCallback {
        fun onPermissionGranted()
    }

    companion object {
        const val REQUEST_FOREGROUND_LOCATION_PERMISSION = 1000
        const val REQUEST_BACKGROUND_LOCATION_PERMISSION = 2000
    }
    /**
     *  Verifica si existen permisos de foreground y background location aceptados
     */
    fun checkAndRequestLocationPermissions() {
        when {
            isForegroundLocationPermissionGranted() && isBackgroundLocationPermissionGranted() -> {
                // Todos los permisos son aceptados
                permissionCallback.onPermissionGranted()
            }
            !isForegroundLocationPermissionGranted() -> {
                // Solicitar permiso de foreground location
                requestForegroundLocationPermission()
            }
            else -> {
                // Solicitar permiso de background location
                requestBackgroundLocationPermission()
            }
        }
    }
    /**
     *  Accion de respuesta del usuario a los permisos solicitados
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_FOREGROUND_LOCATION_PERMISSION,
            REQUEST_BACKGROUND_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAndRequestLocationPermissions()
                } else {
                    Toast.makeText(
                        activity,
                        "Permiso Denegado: FOREGROUND_LOCATION / BACKGROUND_LOCATION.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    /**
     *  Boolean permisos de foreground location
     *  @return true si los permisos son aceptados, false en caso contrario
     */
    private fun isForegroundLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        activity,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    /**
     *  Boolean permisos de background location
     *  @return true si los permisos son aceptados, false en caso contrario
     */
    private fun isBackgroundLocationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Para Android versiones anteriores a Q, la ubicación en segundo plano está incluida en ACCESS_FINE_LOCATION
            true
        }
    }
    /**
     *  Pide los permisos de Foreground Location al usuario
     */
    private fun requestForegroundLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_LOCATION_PERMISSION
        )
    }
    /**
     *  Pide los permisos de Background Location al usuario
     */
    private fun requestBackgroundLocationPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Para Android 11 y superiores, redirige al usuario a la configuración de la aplicación
                showBackgroundLocationPermissionDialog()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Para Android 10, pide el permiso de ACCESS_BACKGROUND_LOCATION
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_BACKGROUND_LOCATION_PERMISSION
                )
            }
            else -> {
                // No hay necesidad de solicitar el permiso de ACCESS_BACKGROUND_LOCATION en versiones anteriores a Android 10
                permissionCallback.onPermissionGranted()
            }
        }
    }

    /**
     *  Muestra un diálogo al solicitar los permisos de background location
     */
    private fun showBackgroundLocationPermissionDialog() {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_location_permission, null)
        val builder = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setPositiveButton("ACEPTAR") { dialog, which ->
                // Abrir configuración de la aplicación
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_BACKGROUND_LOCATION_PERMISSION
                )
                dialog.dismiss()
            }
            .setNegativeButton("CANCELAR") { dialog, which ->
                // Opcional: manejar la cancelación si es necesario
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }
}