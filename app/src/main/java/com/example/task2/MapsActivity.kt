package com.example.task2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView

import com.google.android.gms.maps.CameraUpdateFactory
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
    private var limit = 0;
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
                val marker = mMap.addMarker(MarkerOptions().position(position).title("Lifespan: " + lifespan))
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
                limit=20
                coroutineHTTP(query, limit, 0)
                return false
            }
        })
    }

    private fun coroutineHTTP(query: String, limit: Int, offset: Int) {
        GlobalScope.launch {
            var resultJSON = httpGet(query, limit, offset)

            if (!resultJSON.has("places")) {
                Log.e("---","ERROR encountered")
                Thread.sleep(1000)
                coroutineHTTP(query,limit,offset)
                Log.e("---",resultJSON.toString())
                return@launch
            }

            val places: JSONArray = resultJSON.getJSONArray("places")
            val coordinateList = HashMap<Int, LatLng>()

            for (i in 0 until places.length()) {
                val place: JSONObject = places.getJSONObject(i)
                if (place.has("coordinates") && place.has("life-span")) {
                    if (place.getJSONObject("life-span").has("begin")) {
                        val beginDate =
                            place.getJSONObject("life-span").getString("begin").substring(0, 4)
                                .toInt()
                        if (beginDate >= 1990) {
                            val lat =
                                place.getJSONObject("coordinates").getString("latitude").toDouble()
                            val lng =
                                place.getJSONObject("coordinates").getString("longitude").toDouble()
                            val lifeSpan = beginDate - 1990
                            coordinateList[lifeSpan] = LatLng(lat, lng)
                        }
                    }
                }
            }
            addMarkers(coordinateList)

            if ((offset + limit) < resultJSON.getInt("count")) {
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
        val html = "http://musicbrainz.org/ws/2/place/?query=$setQuery&limit=$setLimit&offset=$setOffset&fmt=json"

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

}
