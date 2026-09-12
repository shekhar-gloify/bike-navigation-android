package com.dopetechindia.bikenavigation.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * CarAppService entry point for Android Auto motorcycle navigation & group telemetry.
 */
class MotorcycleCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return MotorcycleCarSession()
    }
}
