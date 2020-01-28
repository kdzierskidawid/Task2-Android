package com.example.task2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Toast

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
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
    private var requestsIterator = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
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
            resetMarkers()
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
        }, 2, 1000)
    }

    private fun addMarkers(coordinatesOnMap: HashMap<Int, LatLng>) {
        this@MapsActivity.runOnUiThread {
            for ((lifespan, position) in coordinatesOnMap) {
                val marker = mMap.addMarker(MarkerOptions().position(position).title("Lifespan: " + lifespan))
                markerList.add(Marker(marker, lifespan))
            }
        }
    }

    private fun searchSetup() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(searchView: String): Boolean {
                // if search view is empty, then delete added markers
                if (searchView.isBlank()) {
                    resetMarkers()
                }
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                // set limit on 25. Default limit on MusicBrainz server's 25 but allowed range 1-100.
                val limit=25
                coroutineHTTP(query, limit, 0)
                return false
            }
        })
    }

    private fun coroutineHTTP(query: String, limit: Int, offset: Int) {
        GlobalScope.launch {
            var result = httpGet(query, limit, offset)

            if (!result.has("places")) {
                Thread.sleep(2000)
                coroutineHTTP(query,limit,offset)
                return@launch
            }
            val places: JSONArray = result.getJSONArray("places")
            val latlngList = HashMap<Int, LatLng>()

            for (i in 0 until places.length()) {
                val place: JSONObject = places.getJSONObject(i)
                if (place.has("coordinates") && place.has("life-span")) {
                    if (place.getJSONObject("life-span").has("begin")) {

                        val begin = place.getJSONObject("life-span").getString("begin").substring(0,4).toInt()

                        if (begin >= 1990) {
                            val latitude = place.getJSONObject("coordinates").getString("latitude").toDouble()
                            val longitude = place.getJSONObject("coordinates").getString("longitude").toDouble()
                            val lifeSpan = begin - 1990
                            latlngList[lifeSpan] = LatLng(latitude, longitude)
                        }
                    }
                }

            }

            addMarkers(latlngList)

            if ((offset + limit) < result.getInt("count")) {
                coroutineHTTP(query, limit, offset + limit)
            } else {
                setTimerActive()
            }
        }
    }
    private suspend fun httpGet(setQuery: String, setLimit: Int, setOffset: Int): JSONObject {
        requestsIterator += 1

        // On each entity resource, you can perform three different GET requests:
        // search:   /<ENTITY>?query=<QUERY>&limit=<LIMIT>&offset=<OFFSET>
        // source: https://musicbrainz.org/doc/Development/XML_Web_Service/Version_2
        val html = "https://musicbrainz.org/ws/2/place/?query=$setQuery&limit=$setLimit&offset=$setOffset&fmt=json"

        //Coroutine
        return withContext(Dispatchers.IO) {
            val result = try {
                URL(html)
                    .openStream()
                    .bufferedReader()
                    .use { it.readText() }
            } catch (e: IOException) {
                "{error:rateLimit}"
            }
            JSONObject(result)
        }
    }

    // reset markers pointed on map
    private fun resetMarkers(){
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
