package com.dopetechindia.bikenavigation.data.model

/**
 * Provides sample route instances for testing turn-by-turn navigation and map polylines.
 */
object SampleRoutes {

    val cityScenicLoop = NavigationRoute(
        routeId = "route_city_loop",
        title = "City Scenic Bike Loop",
        totalDistanceMeters = 5200.0,
        estimatedDurationSeconds = 1250,
        steps = listOf(
            RouteStep(
                stepIndex = 0,
                startLatitude = 37.7749,
                startLongitude = -122.4194,
                endLatitude = 37.7780,
                endLongitude = -122.4180,
                instruction = "Head north on Market St toward 8th St",
                distanceMeters = 400.0,
                turnType = TurnType.STRAIGHT,
                streetName = "Market St"
            ),
            RouteStep(
                stepIndex = 1,
                startLatitude = 37.7780,
                startLongitude = -122.4180,
                endLatitude = 37.7810,
                endLongitude = -122.4130,
                instruction = "Turn right onto 5th St",
                distanceMeters = 600.0,
                turnType = TurnType.TURN_RIGHT,
                streetName = "5th St"
            ),
            RouteStep(
                stepIndex = 2,
                startLatitude = 37.7810,
                startLongitude = -122.4130,
                endLatitude = 37.7850,
                endLongitude = -122.4080,
                instruction = "Turn slight left onto Howard St",
                distanceMeters = 800.0,
                turnType = TurnType.SLIGHT_LEFT,
                streetName = "Howard St"
            ),
            RouteStep(
                stepIndex = 3,
                startLatitude = 37.7850,
                startLongitude = -122.4080,
                endLatitude = 37.7890,
                endLongitude = -122.4010,
                instruction = "Turn left onto Embarcadero Ave",
                distanceMeters = 1200.0,
                turnType = TurnType.TURN_LEFT,
                streetName = "Embarcadero Ave"
            ),
            RouteStep(
                stepIndex = 4,
                startLatitude = 37.7890,
                startLongitude = -122.4010,
                endLatitude = 37.7950,
                endLongitude = -122.3950,
                instruction = "Continue straight along Ferry Building Promenade",
                distanceMeters = 1500.0,
                turnType = TurnType.STRAIGHT,
                streetName = "Ferry Building Promenade"
            ),
            RouteStep(
                stepIndex = 5,
                startLatitude = 37.7950,
                startLongitude = -122.3950,
                endLatitude = 37.7980,
                endLongitude = -122.3920,
                instruction = "Arrive at Pier 14 Destination",
                distanceMeters = 700.0,
                turnType = TurnType.ARRIVED,
                streetName = "Pier 14"
            )
        ),
        polylinePoints = listOf(
            Pair(37.7749, -122.4194),
            Pair(37.7780, -122.4180),
            Pair(37.7810, -122.4130),
            Pair(37.7850, -122.4080),
            Pair(37.7890, -122.4010),
            Pair(37.7950, -122.3950),
            Pair(37.7980, -122.3920)
        )
    )

    val mountainTrail = NavigationRoute(
        routeId = "route_mountain_trail",
        title = "Twin Peaks Ridgeline Trail",
        totalDistanceMeters = 8400.0,
        estimatedDurationSeconds = 2400,
        steps = listOf(
            RouteStep(
                stepIndex = 0,
                startLatitude = 37.7550,
                startLongitude = -122.4450,
                endLatitude = 37.7520,
                endLongitude = -122.4470,
                instruction = "Climb south along Portola Dr",
                distanceMeters = 1200.0,
                turnType = TurnType.STRAIGHT,
                streetName = "Portola Dr"
            ),
            RouteStep(
                stepIndex = 1,
                startLatitude = 37.7520,
                startLongitude = -122.4470,
                endLatitude = 37.7540,
                endLongitude = -122.4510,
                instruction = "Turn right onto Twin Peaks Blvd",
                distanceMeters = 1800.0,
                turnType = TurnType.TURN_RIGHT,
                streetName = "Twin Peaks Blvd"
            ),
            RouteStep(
                stepIndex = 2,
                startLatitude = 37.7540,
                startLongitude = -122.4510,
                endLatitude = 37.7560,
                endLongitude = -122.4470,
                instruction = "Arrive at Twin Peaks Overlook",
                distanceMeters = 5400.0,
                turnType = TurnType.ARRIVED,
                streetName = "Twin Peaks Overlook"
            )
        ),
        polylinePoints = listOf(
            Pair(37.7550, -122.4450),
            Pair(37.7520, -122.4470),
            Pair(37.7540, -122.4510),
            Pair(37.7560, -122.4470)
        )
    )
}
