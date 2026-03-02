package com.example.myapplication.model

data class XzcyLoginRequest(
    val uid: String,
    val password: String
)

data class XzcyLoginResponse(
    val success: Boolean,
    val session: String?,
    val message: String
)

data class Rollcall(
    val id: Int,
    val name: String,
    val teacher_name: String,
    val type: String,
    val is_checked_in: Boolean
)

data class RollcallResponse(
    val success: Boolean,
    val rollcalls: List<Rollcall>
)

data class CheckinResponse(
    val success: Boolean,
    val message: String
)

data class RollcallsRequest(
    val session: String
)

data class QrCheckinRequest(
    val session: String,
    val rollcall_id: Int,
    val code: String
)

data class NumberCheckinRequest(
    val session: String,
    val rollcall_id: Int,
    val number: String
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Double,
    val verticalAccuracy: Double,
    val speed: Double
)

data class RadarCheckinRequest(
    val session: String,
    val rollcall_id: Int,
    val location: LocationData
)
