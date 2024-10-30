package ayush.ggv.counselling

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        MainContent()
    }
}

@Composable
fun MainContent() {
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var totalSeats by remember { mutableStateOf("") }
    var allocatedStudents by remember { mutableStateOf<Map<String, List<Student>>>(emptyMap()) }

    val categoryQuotas = remember {
        mapOf(
            "UR" to 0.4f,
            "OBC" to 0.3f,
            "SC" to 0.15f,
            "ST" to 0.075f,
            "PWD" to 0.075f
        )
    }

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("xlsx", "xls")),
        mode = PickerMode.Single
    ) { file ->
        if (file != null) {
            selectedFilePath = file.path
        }
    }

    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { filePicker.launch() }) {
            Text("Select Excel File")
        }

        if (selectedFilePath != null) {
            Spacer(Modifier.height(16.dp))
            TextField(
                value = totalSeats,
                onValueChange = { totalSeats = it },
                label = { Text("Total Number of Seats") }
            )

            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                println("Processing file: $selectedFilePath")
                val totalSeatsCount = totalSeats.toIntOrNull() ?: 0
                val processedStudents = processExcelFile(selectedFilePath!!)
                allocatedStudents =
                    allocateSeats(processedStudents, totalSeatsCount, categoryQuotas)
                println("Allocated students: ${allocatedStudents.values.sumOf { it.size }}")
            }) {
                Text("Process and Allocate Seats")
            }
        }

        if (allocatedStudents.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Allocated Students:", style = MaterialTheme.typography.h6)
            LazyColumn {
                allocatedStudents.forEach { (category, students) ->
                    item {
                        Text(
                            "$category (${students.count { !it.name.contains("Waiting List") }}):",
                            style = MaterialTheme.typography.subtitle1
                        )
                    }
                    items(students) { student ->
                        Text(
                            "${student.name} - CUET Score: ${student.cuetScore}",
                            color = if (student.name.contains("Waiting List")) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface
                        )
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

fun allocateSeats(
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
    allocatedStudents["UR"] = sortedStudents.take(urSeats).toMutableList()

    val remainingStudents = sortedStudents.drop(urSeats).toMutableList()

    // Allocate seats for other categories
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


data class Student(
    val name: String,
    val phoneNoEmail: String,
    val cuetScore: Int,
    val category: String,
    val address: String
)

expect fun processExcelFile(filePath: String): List<Student>