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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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


@Composable
fun App(scrollState: ScrollState) {
    val viewModel = remember { GGVCounsellingViewModel() }

    MaterialTheme {
        MainContent(viewModel, scrollState)
    }
}

@Composable
fun MainContent(
    viewModel: GGVCounsellingViewModel,
    scrollState: ScrollState
) {
    val uiState = viewModel.uiState
    var exportMenuExpanded by remember { mutableStateOf(false) }

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("xlsx", "xls")),
        mode = PickerMode.Single
    ) { file ->
        if (file != null) {
            viewModel.setSelectedFilePath(file.path)
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "GGV Counselling",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            FileSelectionCard(
                selectedFilePath = uiState.selectedFilePath,
                totalSeats = uiState.totalSeats,
                onFilePickerLaunch = { filePicker.launch() },
                onTotalSeatsChanged = viewModel::setTotalSeats,
                onProcessExcelFile = viewModel::processExcelFile
            )

            AnimatedVisibility(visible = uiState.selectedStudents.isNotEmpty()) {
                StudentAllocationCard(
                    selectedStudentsCount = uiState.selectedStudents.size,
                    totalSeats = uiState.totalSeats,
                    onTotalSeatsChanged = viewModel::setTotalSeats,
                    onAllocateSeats = viewModel::allocateSeats
                )
            }

            AnimatedVisibility(visible = uiState.allocatedStudents.isNotEmpty()) {
                AllocatedStudentsCard(
                    allocatedStudents = uiState.allocatedStudents,
                    isExporting = uiState.isExporting,
                    onExportClick = { exportMenuExpanded = true },
                    onExportFormat = { format ->
                        exportMenuExpanded = false
                        viewModel.exportResults(format)
                    }
                )
            }
        }
    }

    if (uiState.showStudentSelection) {
        StudentSelectionDialog(
            students = uiState.processedStudents,
            onDismiss = { viewModel.setSelectedStudents(emptyList()) },
            onConfirm = viewModel::setSelectedStudents
        )
    }

    DropdownMenu(
        expanded = exportMenuExpanded,
        onDismissRequest = { exportMenuExpanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Export as Excel") },
            onClick = { viewModel.exportResults("Excel") }
        )
        DropdownMenuItem(
            text = { Text("Export as PDF") },
            onClick = { viewModel.exportResults("PDF") }
        )
    }
}

@Composable
fun FileSelectionCard(
    selectedFilePath: String?,
    totalSeats: String,
    onFilePickerLaunch: () -> Unit,
    onTotalSeatsChanged: (String) -> Unit,
    onProcessExcelFile: () -> Unit
) {
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
                onClick = onFilePickerLaunch,
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
                        onValueChange = onTotalSeatsChanged,
                        label = { Text("Total Number of Seats") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onProcessExcelFile,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Process")
                        Spacer(Modifier.width(8.dp))
                        Text("Process Excel File")
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAllocationCard(
    selectedStudentsCount: Int,
    totalSeats: String,
    onTotalSeatsChanged: (String) -> Unit,
    onAllocateSeats: () -> Unit
) {
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
                "Selected Students: $selectedStudentsCount",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = totalSeats,
                onValueChange = onTotalSeatsChanged,
                label = { Text("Total Number of Seats") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAllocateSeats,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Allocate Seats")
            }
        }
    }
}

@Composable
fun AllocatedStudentsCard(
    allocatedStudents: Map<String, List<Student>>,
    isExporting: Boolean,
    onExportClick: () -> Unit,
    onExportFormat: (String) -> Unit
) {
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
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text("Export Results")
            }
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by Application No. or Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

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