package com.example.nvkha.waze

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.PermissionRequest
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.map_bar.*
import permissions.dispatcher.*

@RuntimePermissions
class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,View.OnClickListener {

    private /*lateinit*/ var map: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val CODE_REQUEST_SETTING_FOR_UPDATE_LOCATION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        get_my_location_button.setOnClickListener(this)
        report_button.setOnClickListener(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
            }
        }


    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.get_my_location_button -> {
                enableGetMyLocationWithPermissionCheck()
                createLocationRequest()
                pointMyLocationWithPermissionCheck()
            }
            R.id.report_button->{

            }
            else -> {

            }
        }
    }

    @NeedsPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun pointMyLocation(){
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map?.setOnMarkerClickListener(this)
    }

    @NeedsPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun enableGetMyLocation() {
        map?.isMyLocationEnabled = true
    }

    override fun onMarkerClick(p0: Marker?) = false

    /*
    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)

        val titleStr = getAddress(location)  // add these two lines
        markerOptions.title(titleStr)

        map.addMarker(markerOptions)
    }
    */

    /*
    private fun getAddress(latLng: LatLng): String {
        // 1
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {
                address = addresses[0]
                for (i in 0 until address.maxAddressLineIndex) {
                    addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                }
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return addressText
    }
    */

    @NeedsPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun startLocationUpdates() {
        if (map == null || (map?.isMyLocationEnabled) == false /*|| locationUpdateRunning*/) return
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
        //locationUpdateRunning = true
    }

    @SuppressLint("RestrictedApi")
    private fun createLocationRequest() {
        // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest.interval = 10000
        // 3
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        // 4
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // 5
        task.addOnSuccessListener {
            locationUpdateState = true
            //startLocationUpdatesWithPermissionCheck()
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this@MapsActivity,
                            CODE_REQUEST_SETTING_FOR_UPDATE_LOCATION)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        //super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODE_REQUEST_SETTING_FOR_UPDATE_LOCATION) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                //startLocationUpdatesWithPermissionCheck()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        //Toast.makeText(this,"On Pause",Toast.LENGTH_SHORT).show()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        //locationUpdateRunning = false
    }

    public override fun onStop() {
        super.onStop()
        //Toast.makeText(this,"On Stop",Toast.LENGTH_SHORT).show()
    }

    public override fun onResume() {
        super.onResume()
        //Toast.makeText(this,"On Resume",Toast.LENGTH_SHORT).show()
//        if (map?.isMyLocationEnabled == false && !resumeFromRequestPermissionFail) {
//            enableGetMyLocation()
//        }
//        if (map?.isMyLocationEnabled == true && locationUpdateState && !locationUpdateRunning&& !resumeFromRequestPermissionFail) {
//            startLocationUpdates()
//        }
//        resumeFromRequestPermissionFail=false
        if (locationUpdateState){
            startLocationUpdatesWithPermissionCheck()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        onRequestPermissionsResult(requestCode, grantResults)
    }

    @OnPermissionDenied(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun onAccessFineLocationDenied(){
        Toast.makeText(this,"ACCESS_FINE_LOCATION_DENIED",Toast.LENGTH_SHORT).show()
    }

//    @OnShowRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)
//    fun showRationaleForAccessFineLocation(request: PermissionRequest){
//        Toast.makeText(this,"ACCESS_FINE_LOCATION_RATIONALE",Toast.LENGTH_SHORT).show()
//    }

    @OnNeverAskAgain(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun onAccessFineLocationNeverAskAgain() {
        Toast.makeText(this,"ACCESS_FINE_LOCATION_NEVER_ASK_AGAIN",Toast.LENGTH_SHORT).show()
        val mySnackbar = Snackbar.make(drawer_layout,"Open App Settings",Snackbar.LENGTH_SHORT)
        mySnackbar.setAction("OPEN",MySnackbarListener())
        mySnackbar.show()
    }

    inner class MySnackbarListener : View.OnClickListener {
        override fun onClick(v: View) {
            openAppSettings()
        }
        private fun openAppSettings() {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }
}
