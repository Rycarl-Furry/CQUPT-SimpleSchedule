package com.example.myapplication.model

data class CustomSchedule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val location: String = "",
    val day: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weeks: List<Int> = emptyList(),
    val color: String = "#FFB74D"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "location" to location,
            "day" to day,
            "startPeriod" to startPeriod,
            "endPeriod" to endPeriod,
            "weeks" to weeks,
            "color" to color
        )
    }
    
    companion object {
        fun fromMap(map: Map<String, Any>): CustomSchedule {
            return CustomSchedule(
                id = map["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                title = map["title"] as? String ?: "",
                location = map["location"] as? String ?: "",
                day = (map["day"] as? Number)?.toInt() ?: 1,
                startPeriod = (map["startPeriod"] as? Number)?.toInt() ?: 1,
                endPeriod = (map["endPeriod"] as? Number)?.toInt() ?: 1,
                weeks = (map["weeks"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList(),
                color = map["color"] as? String ?: "#FFB74D"
            )
        }
    }
}
