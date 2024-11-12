package ayush.ggv.counselling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GGVCounsellingViewModel : ViewModel() {
    var uiState by mutableStateOf(GGVCounsellingUiState())
        private set

    private val categoryQuotas = mapOf(
        "UR" to 0.4f,
        "OBC" to 0.3f,
        "SC" to 0.15f,
        "ST" to 0.075f,
        "PWD" to 0.075f
    )

    fun setSelectedFilePath(path: String?) {
        uiState = uiState.copy(selectedFilePath = path)
    }

    fun setTotalSeats(seats: String) {
        uiState = uiState.copy(totalSeats = seats)
    }

    fun processExcelFile() {
        viewModelScope.launch {
            val processedStudents = processExcelFile(uiState.selectedFilePath!!)
            uiState = uiState.copy(
                processedStudents = processedStudents,
                showStudentSelection = true
            )
        }
    }

    fun setSelectedStudents(students: List<Student>) {
        uiState = uiState.copy(
            selectedStudents = students,
            showStudentSelection = false
        )
    }

    fun allocateSeats() {
        if (validateInputs(uiState.totalSeats, uiState.selectedStudents)) {
            val totalSeatsCount = uiState.totalSeats.toInt()
            val allocatedStudents = allocateSeats(
                uiState.selectedStudents,
                totalSeatsCount,
                categoryQuotas
            )
            uiState = uiState.copy(allocatedStudents = allocatedStudents)
        }
    }

    fun exportResults(format: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isExporting = true)
            val exportFilePath =
                "${uiState.selectedFilePath?.removeSuffix(".xlsx")}-results.${format.lowercase()}"
            val success = exportResults(uiState.allocatedStudents, exportFilePath, format)
            uiState = uiState.copy(isExporting = false)
            // Handle success/failure message
        }
    }

    private fun validateInputs(totalSeats: String, selectedStudents: List<Student>): Boolean {
        val totalSeatsCount = totalSeats.toIntOrNull()
        return when {
            totalSeatsCount == null || totalSeatsCount <= 0 -> false
            selectedStudents.isEmpty() -> false
            else -> true
        }
    }

    private fun allocateSeats(
        students: List<Student>,
        totalSeats: Int,
        quotas: Map<String, Float>
    ): Map<String, List<Student>> {
        val sortedStudents = students.sortedByDescending { it.cuetScore }
        val allocatedStudents = mutableMapOf<String, MutableList<Student>>()
        val seatsPerCategory = quotas.mapValues { (_, quota) -> (totalSeats * quota).toInt() }
        val waitingListSize = 5

        // Allocate UR seats first
        val urSeats = seatsPerCategory["UR"] ?: 0
        val urAllocated = mutableListOf<Student>()
        var remainingStudents = sortedStudents.toMutableList()
        // Allocate UR seats to top scorers from all categories
        for (i in 0 until urSeats) {
            if (remainingStudents.isNotEmpty()) {
                val student = remainingStudents.removeAt(0)
                urAllocated.add(student)
            }
        }
        allocatedStudents["UR"] = urAllocated
        // Allocate seats for reserved categories

        for ((category, seats) in seatsPerCategory) {
            if (category != "UR") {
                val categoryStudents = remainingStudents.filter { it.category == category }
                val allocatedForCategory = categoryStudents.take(seats)
                allocatedStudents[category] = allocatedForCategory.toMutableList()
                remainingStudents.removeAll(allocatedForCategory)
            }
        }

        // Allocate waiting list seats for all categories
        for (category in seatsPerCategory.keys) {
            val waitingList = if (category == "UR") {
                remainingStudents.take(waitingListSize)
            } else {
                remainingStudents.filter { it.category == category }.take(waitingListSize)
            }
            allocatedStudents[category]!!.addAll(waitingList.mapIndexed { index, student ->
                student.copy(name = "${student.name} (Waiting List ${index + 1})")
            })
            remainingStudents.removeAll(waitingList)
        }

        return allocatedStudents
    }
}

data class GGVCounsellingUiState(
    val selectedFilePath: String? = null,
    val totalSeats: String = "",
    val allocatedStudents: Map<String, List<Student>> = emptyMap(),
    val isExporting: Boolean = false,
    val processedStudents: List<Student> = emptyList(),
    val selectedStudents: List<Student> = emptyList(),
    val showStudentSelection: Boolean = false
)

data class Student(
    val cuetApplicationNo: String,
    val name: String,
    val phoneNoEmail: String,
    val cuetScore: Int,
    val category: String,
    val address: String,
    val serialNo: Int = 0
)

expect suspend fun exportResults(
    allocatedStudents: Map<String, List<Student>>,
    filePath: String,
    format: String
): Boolean

expect fun processExcelFile(filePath: String): List<Student>