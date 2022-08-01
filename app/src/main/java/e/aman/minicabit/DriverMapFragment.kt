package e.aman.minicabit

import android.Manifest
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import e.aman.minicabit.databinding.FragmentDriverMapBinding
import e.aman.minicabit.utils.Constants


class DriverMapFragment : Fragment(), OnMapReadyCallback{

    private lateinit var binding: FragmentDriverMapBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    //This is just for registering/de-registering the listener(online system)
    private lateinit var onlineRef: DatabaseReference
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var currentUserRef: DatabaseReference
    private lateinit var geoFire: GeoFire
    private lateinit var currUserId: String

    /** if user disconnects the database just delete the user from driverLocationRef **/
    private var onlineEventListener = object: ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists())
                currentUserRef.onDisconnect().removeValue()
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("error" , error.message)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentDriverMapBinding.inflate(inflater, container, false)
        currUserId = FirebaseAuth.getInstance().currentUser!!.uid

        //initialize map
        val mapFragment = childFragmentManager.findFragmentById(e.aman.minicabit.R.id.google_map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        initLocation()
        return binding.root
    }

    private fun initLocation() {

        /** initialize driver online variables **/
        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")
        driversLocationRef = FirebaseDatabase.getInstance().getReference(Constants.DRIVER_LOCATION_REF)
        currentUserRef = FirebaseDatabase.getInstance().getReference(Constants.DRIVER_LOCATION_REF).child(currUserId)
        geoFire = GeoFire(driversLocationRef)

        /** init location request **/
        locationRequest = LocationRequest.create().apply {
            smallestDisplacement = 10f
            interval = 5000
            fastestInterval = 3000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationCallback =  object: LocationCallback(){
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                //set location to database using geofire
                geoFire.setLocation(currUserId ,
                GeoLocation(p0.lastLocation!!.latitude, p0.lastLocation!!.longitude)
                )

                Log.e("myloc" , p0.lastLocation.toString())
                var newPos = LatLng(p0.lastLocation!!.latitude , p0.lastLocation!!.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos , 18f))
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        //register fused location api with location callback
        fusedLocationProviderClient.requestLocationUpdates(locationRequest , locationCallback , Looper.myLooper())

    }

    /** map callbacks when ready **/
    override fun onMapReady(googleMap: GoogleMap) {

        /** init google maps **/
        mMap = googleMap

        try{
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext() , R.raw.uber_maps_style))
        }
        catch (e: Resources.NotFoundException) {
        }

        /** check location permission **/
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        Dexter.withContext(activity)
                .withPermissions(permissions)
                .withListener(object : MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        /** if location permission granted show current loc button **/
                        mMap.isMyLocationEnabled = true
                        mMap.uiSettings.isMyLocationButtonEnabled = true

                        mMap.setOnMyLocationButtonClickListener {
                            try {
                                /** get last location and move camera... **/
                                fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                                    var newPos = LatLng(it!!.latitude, it!!.longitude)
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))
                                }
                            }
                            catch(e: Exception){}
                            true
                        }
                    }
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        Toast.makeText(requireContext(), getString(R.string.error_location_permission) ,Toast.LENGTH_SHORT).show()
                    }
                })
                .check()
    }

    override fun onResume() {
        super.onResume()
        onlineRef.addValueEventListener(onlineEventListener)
    }


    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        geoFire.removeLocation(currUserId)
        onlineRef.removeEventListener(onlineEventListener)
        super.onDestroy()
    }
}