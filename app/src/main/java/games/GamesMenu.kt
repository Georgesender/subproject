package games

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.sgb.ActBikeGarage
import com.example.sub.R

class GamesMenu : AppCompatActivity() {
    private  var bikeId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.games_menu)
        setupBottomNavigation()
        bikeId = intent.getIntExtra("bike_id", -1)
        if(bikeId == -1) {
            Toast.makeText(this , "BikeId has def value! Something wrong" , Toast.LENGTH_SHORT).show()
        }
    }
    private fun setupBottomNavigation() {
        val navHome = findViewById<TextView>(R.id.current_bike)
        val navGames = findViewById<TextView>(R.id.games)

// garage test
        navHome.setOnClickListener {
            val intent = Intent(this , ActBikeGarage::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this , R.anim.fade_in_faster , R.anim.fade_out_faster
            )
            intent.putExtra("bike_id", bikeId)
            startActivity(intent , options.toBundle())
            finish()
        }

        navGames.setTypeface(null , Typeface.BOLD)
        navGames.textSize = navGames.textSize / resources.displayMetrics.density + 10
    }
}