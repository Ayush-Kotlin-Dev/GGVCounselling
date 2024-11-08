package ayush.ggv.counselling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(scrollState: ScrollState) {
    MaterialTheme {
        MainContent(scrollState)
    }
}
@Composable
fun MainContent(
    scrollState: ScrollState
) {
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var totalSeats by remember { mutableStateOf("") }
    var allocatedStudents by remember { mutableStateOf<Map<String, List<Student>>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("Excel") }
    var exportMenuExpanded by remember { mutableStateOf(false) }

    var processedStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    var selectedStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    var showStudentSelection by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 12.dp, start = 16.dp, top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "GGV Counselling",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { filePicker.launch() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Upload")
                        Spacer(Modifier.width(8.dp))
                        Text("Select Excel File")
                    }

                    AnimatedVisibility(visible = selectedFilePath != null) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = totalSeats,
                                onValueChange = { totalSeats = it },
                                label = { Text("Total Number of Seats") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    processedStudents = processExcelFile(selectedFilePath!!)
                                    showStudentSelection = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Process")
                                Spacer(Modifier.width(8.dp))
                                Text("Process Excel File")
                            }
                        }
                    }
                    if (showStudentSelection) {
                        StudentSelectionDialog(
                            students = processedStudents,
                            onDismiss = { showStudentSelection = false },
                            onConfirm = { selected ->
                                selectedStudents = selected
                                showStudentSelection = false
                            }
                        )
                    }
                }
            }
            AnimatedVisibility(visible = selectedStudents.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Selected Students: ${selectedStudents.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = totalSeats,
                            onValueChange = { totalSeats = it },
                            label = { Text("Total Number of Seats") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (validateInputs(totalSeats, selectedStudents)) {
                                    val totalSeatsCount = totalSeats.toInt()
                                    allocatedStudents = allocateSeats(
                                        selectedStudents,
                                        totalSeatsCount,
                                        categoryQuotas
                                    )
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Please check your inputs and try again.")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Allocate Seats")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = allocatedStudents.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Allocated Students",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            allocatedStudents.forEach { (category, students) ->
                                item {
                                    Text(
                                        "$category (${students.count { !it.name.contains("Waiting List") }}):",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                items(students) { student ->
                                    Text(
                                        "${student.name} - CUET Score: ${student.cuetScore}",
                                        color = if (student.name.contains("Waiting List")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (student.name.contains("Waiting List")) MaterialTheme.colorScheme.secondary.copy(
                                                    alpha = 0.1f
                                                ) else Color.Transparent
                                            )
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { exportMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isExporting,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("Export Results")
                        }
                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as Excel") },
                                onClick = {
                                    exportFormat = "Excel"
                                    exportMenuExpanded = false
                                    coroutineScope.launch {
                                        isExporting = true
                                        val exportFilePath = "${selectedFilePath?.removeSuffix(".xlsx")}-results.xlsx"
                                        val success = exportResults(allocatedStudents, exportFilePath, "Excel")
                                        isExporting = false
                                        // Show success/failure message
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    exportFormat = "PDF"
                                    exportMenuExpanded = false
                                    coroutineScope.launch {
                                        isExporting = true
                                        val exportFilePath = "${selectedFilePath?.removeSuffix(".xlsx")}-results.pdf"
                                        val success = exportResults(allocatedStudents, exportFilePath, "PDF")
                                        isExporting = false
                                        // Show success/failure message
                                    }
                                }
                            )
                        }
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentSelectionDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onConfirm: (List<Student>) -> Unit
) {
    var selectedStudents by remember { mutableStateOf(setOf<Student>()) }
    var searchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Students for Counselling") },
        text = {
            Column {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by Application No. or Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                // Headings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Text("Select", modifier = Modifier.weight(0.1f), fontWeight = FontWeight.Bold)
                    Text("App. No.", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold)
                    Text("Name", modifier = Modifier.weight(0.3f), fontWeight = FontWeight.Bold)
                    Text(
                        "CUET Score",
                        modifier = Modifier.weight(0.2f),
                        fontWeight = FontWeight.Bold
                    )
                    Text("Category", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold)
                }

                // Student list
                LazyColumn {
                    val filteredStudents = students.filter { student ->
                        searchQuery.isEmpty() || student.cuetApplicationNo.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                                student.name.contains(searchQuery, ignoreCase = true)
                    }
                    items(filteredStudents) { student ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedStudents = if (selectedStudents.contains(student)) {
                                        selectedStudents - student
                                    } else {
                                        selectedStudents + student
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = selectedStudents.contains(student),
                                onCheckedChange = null,
                                modifier = Modifier.weight(0.1f)
                            )
                            Text(student.cuetApplicationNo, modifier = Modifier.weight(0.2f))
                            Text(student.name, modifier = Modifier.weight(0.3f))
                            Text(student.cuetScore.toString(), modifier = Modifier.weight(0.2f))
                            Text(student.category, modifier = Modifier.weight(0.2f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedStudents.toList()) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

fun validateInputs(totalSeats: String, selectedStudents: List<Student>): Boolean {
    val totalSeatsCount = totalSeats.toIntOrNull()
    return when {
        totalSeatsCount == null || totalSeatsCount <= 0 -> false
        selectedStudents.isEmpty() -> false
        else -> true
    }
}

data class Student(
    val cuetApplicationNo: String,
    val name: String,
    val phoneNoEmail: String,
    val cuetScore: Int,
    val category: String,
    val address: String,
    val serialNo: Int = 0  // Add this line
)


expect suspend fun exportResults(
    allocatedStudents: Map<String, List<Student>>,
    filePath: String,
    format: String
): Boolean

expect fun processExcelFile(filePath: String): List<Student>

