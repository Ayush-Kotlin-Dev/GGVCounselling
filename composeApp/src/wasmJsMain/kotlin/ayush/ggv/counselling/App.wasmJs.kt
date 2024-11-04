package ayush.ggv.counselling

actual fun processExcelFile(filePath: String): List<Student> {
    return emptyList()
}

actual suspend fun exportResults(
    allocatedStudents: Map<String, List<Student>>,
    filePath: String,
    format: String
): Boolean {
    return false
}

