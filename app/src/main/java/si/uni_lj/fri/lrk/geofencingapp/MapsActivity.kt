package si.uni_lj.fri.lrk.geofencingapp

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import java.util.concurrent.atomic.AtomicReference

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnCompleteListener<Void> {

    companion object {
        const val TAG = "MapsActivity"
        const val PERMISSION_REQUEST_BACKGROUND_LOCATION = 1
        const val PERMISSION_REQUEST_FINE_LOCATION = 2
    }

    private lateinit var mMap: GoogleMap
    private lateinit var mHome: Marker
    private lateinit var mWork: Marker
    private lateinit var mFitness: Marker

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mGeofencingClient: GeofencingClient
    private val mGeofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this,
                GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // TODO: instantiate the geofencing client
        mGeofencingClient = LocationServices.getGeofencingClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        sortPermissions()
    }

    @SuppressLint("MissingPermission")
    private fun whenMapReady() {

        val latitude: AtomicReference<Double>  =  AtomicReference(0.0)
        val longitude: AtomicReference<Double>  =  AtomicReference(0.0)

        mMap.isMyLocationEnabled = true

        val markerHome: MarkerOptions = MarkerOptions()
                .position(LatLng(latitude.get(), longitude.get()))
                .title(getString(R.string.map_marker_home))
                .draggable(true)
        markerHome.icon(bitmapDescriptorFromVector(this, R.drawable.ic_home_black_24dp))
        mHome = mMap.addMarker(markerHome)

        val markerWork: MarkerOptions = MarkerOptions()
                .position(LatLng(latitude.get(), longitude.get() + 0.005))
                .title(getString(R.string.map_marker_work))
                .draggable(true)
        markerWork.icon(bitmapDescriptorFromVector(this, R.drawable.ic_work_black_24dp))
        mWork = mMap.addMarker(markerWork)

        val markerFitness: MarkerOptions = MarkerOptions()
                .position(LatLng(latitude.get(), longitude.get() - 0.005))
                .title(getString(R.string.map_marker_fitness))
                .draggable(true)
        markerFitness.icon(bitmapDescriptorFromVector(this, R.drawable.ic_fitness_black_24dp))
        mFitness = mMap.addMarker(markerFitness)

        mFusedLocationProviderClient.lastLocation.addOnSuccessListener {

            if (it == null) {
                Log.d(TAG, "Last known location is null")
            } else {
                latitude.set(it.latitude)
                longitude.set(it.longitude)

                Log.d(TAG, "Last known location is $latitude , $longitude")

                if (mHome.position.latitude == 0.0) {
                    mHome.setPosition(LatLng(latitude.get(), longitude.get()))
                }

                if (mWork.position.latitude == 0.0) {
                    mWork.setPosition(LatLng(latitude.get(), longitude.get() + 0.005))
                }

                if (mFitness.position.latitude == 0.0) {
                    mFitness.setPosition(LatLng(latitude.get(), longitude.get() - 0.005))
                }

                val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(latitude.get(), longitude.get())).zoom(14f).build()
                mMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(cameraPosition))
            }
        }

        mMap.setOnMarkerDragListener(object: GoogleMap.OnMarkerDragListener{
            override fun onMarkerDragEnd(marker: Marker?) {
                marker?:return

                val lat = marker.position.latitude
                val lon = marker.position.longitude

                // TODO: Uncomment when addGeofence function is completed

                if (marker.title.equals(getString(R.string.map_marker_home))) {
                    addGeofence(getGeofencingRequest(getString(R.string.map_marker_home), lat, lon))
                } else if (marker.title.equals(getString(R.string.map_marker_work))) {
                    addGeofence(getGeofencingRequest(getString(R.string.map_marker_work), lat, lon))
                } else if (marker.title.equals(getString(R.string.map_marker_fitness))) {
                    addGeofence(getGeofencingRequest(getString(R.string.map_marker_fitness), lat, lon))
                }
            }

            override fun onMarkerDragStart(p0: Marker?) {}

            override fun onMarkerDrag(p0: Marker?) {}
        })

    }

    // TODO: Implement getGeofencingRequest function

    private fun getGeofencingRequest (type: String, lat: Double, lon: Double): GeofencingRequest {

        Log.d(TAG, "getGeofencingRequest: $lat, $lon")
        lateinit var geofence: Geofence

        if (type.equals(getString(R.string.map_marker_home))) {
            geofence = with(Geofence.Builder()){
                setRequestId(type)
                setCircularRegion(lat, lon, 200F)
                setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                build()
            }
        } else if (type.equals(getString(R.string.map_marker_work))) {
            geofence = with(Geofence.Builder()){
                setRequestId(type)
                setCircularRegion(lat, lon, 300F)
                setExpirationDuration(Geofence.NEVER_EXPIRE)
                setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                build()
            }
        } else if (type.equals(getString(R.string.map_marker_fitness))) {
            geofence = with(Geofence.Builder()){
                setRequestId(type)
                setCircularRegion(lat, lon, 300F)
                setExpirationDuration(Geofence.NEVER_EXPIRE)
                setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                setLoiteringDelay(1*1000)
                build()
            }
        }

        return with(GeofencingRequest.Builder()){
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(listOf(geofence))
            build()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(request : GeofencingRequest){

        // TODO: add geofence through the Geofencing Client
        mGeofencingClient.addGeofences(request, mGeofencePendingIntent)
               .addOnCompleteListener(this)
    }


    override fun onComplete(task: Task<Void>) {
        if (task.isSuccessful) {
            Toast.makeText(this, "Setting geofence successful", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Setting geofence unsuccessful", Toast.LENGTH_LONG).show()
        }
    }

    // For drawing icons on the map
    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    // For handling permissions
    private fun sortPermissions(){

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("This app needs background location access")
                    builder.setMessage(getString(R.string.rationale_location))
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener {
                        requestPermissions(
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                PERMISSION_REQUEST_BACKGROUND_LOCATION
                        )
                    }
                    builder.show()
                } else {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val builder =
                                AlertDialog.Builder(this)
                        builder.setTitle("Functionality limited")
                        builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.setOnDismissListener {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri: Uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            // This will take the user to a page where they have to click twice to drill down to grant the permission
                            startActivity(intent)
                        }
                        builder.show()
                    }
                }
            } else {
                whenMapReady()
            }
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSION_REQUEST_FINE_LOCATION
                )
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Functionality limited")
                builder.setMessage("Since location access has not been granted, this app will not be able to discover geofences.  Please go to Settings -> Applications -> Permissions and grant location access to this app.")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri: Uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    // This will take the user to a page where they have to click twice to drill down to grant the permission
                    startActivity(intent)
                }
                builder.show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_BACKGROUND_LOCATION) {
            if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                sortPermissions()
            }
            else {
                whenMapReady()
            }
        } else if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            sortPermissions()
        }
    }

}