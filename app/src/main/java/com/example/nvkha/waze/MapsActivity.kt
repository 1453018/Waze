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
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    val options = PolylineOptions()
    private var mapReady = false
    private var mapSetup = false
    private var locationUpdateRunning = false
    private var alreadyAskPermission = false
    private var resumeFromRequestPermissionFail = false

    companion object {
        private const val CODE_REQUEST_PERMISSION_FOR_UPDATE_LOCATION = 1
        private const val CODE_REQUEST_SETTING_FOR_UPDATE_LOCATION = 2
        private const val CODE_REQUEST_PERMISSION_FOR_SETUP_MAP = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Toast.makeText(this,"On Create",Toast.LENGTH_SHORT).show()
        setContentView(R.layout.activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                //placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                options.add(LatLng(lastLocation.latitude, lastLocation.longitude))
                if (mapReady) {
                    map.addPolyline(options)
                }
            }
        }

        createLocationRequest()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        mapReady = true

        //map.uiSettings.isZoomControlsEnabled = true

        map.setOnMarkerClickListener(this)

        if (!mapSetup) {
            setUpMapWrapper()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setUpMap() {

        map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                //placeMarkerOnMap(currentLatLng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                options.add(currentLatLng)
            }
        }

        mapSetup = true
    }

    private fun setUpMapWrapper() {
        if (!mapReady) return
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!alreadyAskPermission){
                ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), CODE_REQUEST_PERMISSION_FOR_SETUP_MAP)
                alreadyAskPermission = true
            }
            return
        }
        setUpMap()
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

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
        locationUpdateRunning = true
    }

    private fun startLocationUpdatesWrapper() {
        if (!mapSetup || locationUpdateRunning) return
        if (ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!alreadyAskPermission) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), CODE_REQUEST_PERMISSION_FOR_UPDATE_LOCATION)
                alreadyAskPermission = true
            }
            return
        }
        startLocationUpdates()
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
            //startLocationUpdatesWrapper()
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
                //startLocationUpdatesWrapper()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        //Toast.makeText(this,"On Pause",Toast.LENGTH_SHORT).show()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationUpdateRunning = false
    }

    public override fun onStop() {
        super.onStop()
        //Toast.makeText(this,"On Stop",Toast.LENGTH_SHORT).show()
    }

    public override fun onResume() {
        super.onResume()
        //Toast.makeText(this,"On Resume",Toast.LENGTH_SHORT).show()
        if (!mapSetup && !resumeFromRequestPermissionFail) {
            setUpMapWrapper()
        }
        if (mapSetup && locationUpdateState && !locationUpdateRunning&& !resumeFromRequestPermissionFail) {
            startLocationUpdatesWrapper()
        }
        resumeFromRequestPermissionFail=false
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            CODE_REQUEST_PERMISSION_FOR_UPDATE_LOCATION -> {
                alreadyAskPermission = false
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    startLocationUpdates()
                } else {

                    //Toast.makeText(this, "UPDATE LOCATION DENIED", Toast.LENGTH_LONG).show()
                    //Log.e("K", "UPDATE LOCATION DENIED")
                    resumeFromRequestPermissionFail = true;
                    openAppSettings()

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            CODE_REQUEST_PERMISSION_FOR_SETUP_MAP -> {
                alreadyAskPermission = false
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    setUpMap()
                } else {
                    //Toast.makeText(this, "SETUP MAP DENIED", Toast.LENGTH_LONG).show()
                    //Log.e("K", "SETUP MAP DENIED")
                    resumeFromRequestPermissionFail = true;
                    openAppSettings()


                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

        // Add other 'when' lines to check for other
        // permissions this app might request.

            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}
