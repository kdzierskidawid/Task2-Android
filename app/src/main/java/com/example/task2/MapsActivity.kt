package com.example.task2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
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
        searchSetup()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //setTimerActive()
    }

    fun initLayout(){
        searchView = findViewById(R.id.searchView)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    private fun deleteMarkers(seconds: Int) {
        for (marker in markerList) {
            if ((marker.lifetime - seconds)==0) {
                marker.marker.remove()
            }
        }
        if(timerLifespan == 0){
            mMap.clear()
            if(isTimerStarted) {
                timer.cancel()
                timer.purge()
                isTimerStarted = false
            }
            sec = 0
            markerList = ArrayList()
        }
    }

    private fun setTimerActive() {
        timerLifespan = markerList.size
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                this@MapsActivity.runOnUiThread {
                    deleteMarkers(sec)
                    sec += 1
                    timerLifespan -=1
                    isTimerStarted = true
                }
            }
        }, 1, 1000)
    }

    private fun addMarkers(coordinatesOnMap: HashMap<Int, LatLng>) {
        this@MapsActivity.runOnUiThread {
            for ((lifespan, position) in coordinatesOnMap) {
                val marker = mMap.addMarker(MarkerOptions().position(position).title("Request marker"))
                markerList.add(Marker(marker, lifespan))
            }
        }
    }

    private fun searchSetup() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isBlank()) {
                    if(timerLifespan == 0){
                        mMap.clear()
                        if(isTimerStarted) {
                            timer.cancel()
                            timer.purge()
                            isTimerStarted = false
                        }
                        sec = 0
                        markerList = ArrayList()
                    }
                }
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }
        })
    }



}
