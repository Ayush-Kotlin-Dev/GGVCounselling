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

    fun setCounsellingRound(round: Int) {
        uiState = uiState.copy(counsellingRound = round)
    }

    fun setCategorySeats(category: String, seats: String) {
        val updatedCategorySeats = uiState.categorySeats.toMutableMap()
        updatedCategorySeats[category] = seats
        uiState = uiState.copy(categorySeats = updatedCategorySeats)
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
        viewModelScope.launch {
            if (validateInputs()) {
                val allocatedStudents = if (uiState.counsellingRound == 1) {
                    allocateSeatsForRoundOne()
                } else {
                    allocateSeatsForLaterRounds()
                }
                uiState = uiState.copy(allocatedStudents = allocatedStudents)
            }
        }
    }

    private fun validateInputs(): Boolean {
        return if (uiState.counsellingRound == 1) {
            uiState.totalSeats.toIntOrNull() != null && uiState.totalSeats.toInt() > 0 && uiState.selectedStudents.isNotEmpty()
        } else {
            uiState.categorySeats.all { it.value.toIntOrNull() != null && it.value.toInt() > 0 } && uiState.selectedStudents.isNotEmpty()
        }
    }

    private fun allocateSeatsForRoundOne(): Map<String, List<Student>> {
        val totalSeatsCount = uiState.totalSeats.toInt()
        return allocateSeats(
            uiState.selectedStudents,
            totalSeatsCount,
            categoryQuotas
        )
    }

    private fun allocateSeatsForLaterRounds(): Map<String, List<Student>> {
        val categorySeats = uiState.categorySeats.mapValues { it.value.toInt() }
        return allocateSeatsForCategories(uiState.selectedStudents, categorySeats)
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


    private fun allocateSeatsForCategories(
        students: List<Student>,
        categorySeats: Map<String, Int>
    ): Map<String, List<Student>> {
        val allocatedStudents = mutableMapOf<String, MutableList<Student>>()
        val remainingStudents = students.sortedByDescending { it.cuetScore }.toMutableList()

        // First, allocate UR seats
        val urSeats = categorySeats["UR"] ?: 0
        val urAllocated = remainingStudents.take(urSeats)
        allocatedStudents["UR"] = urAllocated.toMutableList()
        remainingStudents.removeAll(urAllocated)

        // Then allocate seats for other categories
        for ((category, seats) in categorySeats) {
            if (category != "UR") {
                val categoryStudents = remainingStudents.filter { it.category == category }
                val allocatedForCategory = categoryStudents.take(seats)
                allocatedStudents[category] = allocatedForCategory.toMutableList()
                remainingStudents.removeAll(allocatedForCategory)
            }
        }

        // Allocate waiting list seats for all categories
        val waitingListSize = 5
        for (category in categorySeats.keys) {
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
    val counsellingRound: Int = 1,
    val categorySeats: Map<String, String> = mapOf(
        "UR" to "", "OBC" to "", "SC" to "", "ST" to "", "PWD" to ""
    ),
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