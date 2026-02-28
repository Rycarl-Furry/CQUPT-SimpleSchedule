package com.example.myapplication.model

data class Exam(
    val course_code: String,
    val course_name: String,
    val exam_location: String,
    val exam_qualification: String,
    val exam_time: String,
    val exam_type: String,
    val exam_week: String,
    val name: String,
    val no: String,
    val seat: String,
    val student_id: String,
    val weekday: String
)

data class MakeupExam(
    val class_name: String,
    val course_code: String,
    val course_name: String,
    val exam_date: String,
    val exam_location: String,
    val exam_time: String,
    val name: String,
    val no: String,
    val remark: String,
    val seat: String,
    val student_id: String
)

data class ExamResponse(
    val exams: List<Exam>,
    val makeup_exams: List<MakeupExam>,
    val student_id: String
)
