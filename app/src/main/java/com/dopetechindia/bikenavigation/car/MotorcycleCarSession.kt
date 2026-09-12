package com.dopetechindia.bikenavigation.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

/**
 * Android Auto Session managing the motorcycle navigation screen lifecycle.
 */
class MotorcycleCarSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        return MotorcycleCarScreen(carContext)
    }
}
