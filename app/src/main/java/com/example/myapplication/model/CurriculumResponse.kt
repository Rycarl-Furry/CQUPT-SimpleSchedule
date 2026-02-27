package com.example.myapplication.model

data class CurriculumResponse(
    val student_id: String,
    val student_name: String,
    val academic_year: String,
    val semester: String,
    val week_1_monday: String,
    val instances: List<CourseInstance>
)

data class CourseInstance(
    val course: String,
    val teacher: String,
    val week: Int,
    val day: Int,
    val periods: List<Int>,
    val date: String,
    val start_time: String,
    val end_time: String,
    val location: String,
    val type: String
)
