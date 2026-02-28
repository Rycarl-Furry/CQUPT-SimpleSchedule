package com.example.myapplication.model

data class SportsRequest(
    val access_token: String
)

data class SportsResponse(
    val data: SportsDataWrapper,
    val success: Boolean
)

data class SportsDataWrapper(
    val code: String,
    val data: SportsData,
    val msg: String
)

data class SportsData(
    val totalCount: Int,
    val list: List<SportsRecord>
)

data class SportsRecord(
    val id: String,
    val studentName: String,
    val studentNo: String,
    val sportsDateStr: String,
    val sportsStartTime: String,
    val sportsEndTime: String,
    val sportsType: String,
    val placeName: String,
    val isValid: String,
    val reason: String?,
    val distance: Int,
    val duration: Int
)
