package putra.yanuar.tarot

import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import putra.yanuar.tarot.databinding.ActivityReaderLocationBinding

class ReaderLocationActivity : AppCompatActivity() {

    private lateinit var b: ActivityReaderLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wajib — osmdroid butuh shared prefs untuk caching peta
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        b = ActivityReaderLocationBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbarLocation)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val readerName = intent.getStringExtra("READER_NAME") ?: "Reader"
        val lat = intent.getDoubleExtra("LAT", 0.0)
        val lng = intent.getDoubleExtra("LNG", 0.0)

        supportActionBar?.title = "Lokasi $readerName"

        if (lat == 0.0 && lng == 0.0) {
            Toast.makeText(this, "Reader belum membagikan lokasi", Toast.LENGTH_SHORT).show()
        }

        b.mapReaderLocation.setTileSource(TileSourceFactory.MAPNIK)
        b.mapReaderLocation.setMultiTouchControls(true)

        val point = GeoPoint(lat, lng)
        b.mapReaderLocation.controller.setZoom(16.0)
        b.mapReaderLocation.controller.setCenter(point)

        val marker = Marker(b.mapReaderLocation)
        marker.position = point
        marker.title = readerName
        marker.snippet = "Lokasi praktik reader"
        b.mapReaderLocation.overlays.add(marker)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        b.mapReaderLocation.onResume()
    }

    override fun onPause() {
        super.onPause()
        b.mapReaderLocation.onPause()
    }
}