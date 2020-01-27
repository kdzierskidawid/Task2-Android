package com.example.task2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*
import kotlin.collections.ArrayList

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var searchView: SearchView
    private var markerList: ArrayList<Marker> = ArrayList()
    private var timer: Timer = Timer()

    private var sec = 0
    private var isTimerStarted = false
    private var timerLifespan = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        initLayout()
        //initValues()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setTimerActive()
        // Add a marker in Sydney and move the camera
        //val sydney = LatLng(-34.0, 151.0)
        //mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    fun initLayout(){
        searchView = findViewById(R.id.searchView)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

/*    fun initValues(){
        markerList = ArrayList()
        timer = Timer()
    }*/

    private fun setTimerActive() {
        timerLifespan = markerList.size
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                this@MapsActivity.runOnUiThread {
                    checkTimerMarker()
                    isTimerStarted = true
                    sec += 1
                    timerLifespan -=1
                }
            }
        }, 1, 1000)
    }

    fun checkTimerMarker(){
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
    }
}
