package ayush.ggv.counselling

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ayush.ggv.counselling.ui.GGVCounsellingTheme
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType

sealed interface ExportFormat {
    object Excel : ExportFormat
    object PDF : ExportFormat
}

@Composable
fun App(scrollState: ScrollState) {
    val viewModel = remember { GGVCounsellingViewModel() }

    GGVCounsellingTheme {
        MainContent(viewModel, scrollState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GGV Counselling") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            FileSelectionCard(
                selectedFilePath = uiState.selectedFilePath,
                counsellingRound = uiState.counsellingRound,
                totalSeats = uiState.totalSeats,
                categorySeats = uiState.categorySeats,
                onFilePickerLaunch = { filePicker.launch() },
                onCounsellingRoundChanged = viewModel::setCounsellingRound,
                onTotalSeatsChanged = viewModel::setTotalSeats,
                onCategorySeatsChanged = viewModel::setCategorySeats,
                onProcessExcelFile = viewModel::processExcelFile
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = uiState.selectedStudents.isNotEmpty()) {
                StudentAllocationCard(
                    viewModel = viewModel,
                    selectedStudentsCount = uiState.selectedStudents.size,
                    counsellingRound = uiState.counsellingRound,
                    totalSeats = uiState.totalSeats,
                    categorySeats = uiState.categorySeats,
                    onTotalSeatsChanged = viewModel::setTotalSeats,
                    onCategorySeatsChanged = viewModel::setCategorySeats
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = uiState.allocatedStudents.isNotEmpty()) {
                AllocatedStudentsCard(
                    allocatedStudents = uiState.allocatedStudents,
                    isExporting = uiState.isExporting,
                    onExportClick = { exportMenuExpanded = true },
                    onExportFormat = { format ->
                        exportMenuExpanded = false
                        viewModel.exportResults(format.toString())
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
            onClick = {
                exportMenuExpanded = false
                viewModel.exportResults(ExportFormat.Excel.toString())
            }
        )
        DropdownMenuItem(
            text = { Text("Export as PDF") },
            onClick = {
                exportMenuExpanded = false
                viewModel.exportResults(ExportFormat.PDF.toString())
            }
        )
    }
}

@Composable
fun FileSelectionCard(
    selectedFilePath: String?,
    counsellingRound: Int,
    totalSeats: String,
    categorySeats: Map<String, String>,
    onFilePickerLaunch: () -> Unit,
    onCounsellingRoundChanged: (Int) -> Unit,
    onTotalSeatsChanged: (String) -> Unit,
    onCategorySeatsChanged: (String, String) -> Unit,
    onProcessExcelFile: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "File Selection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onFilePickerLaunch,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload")
                Spacer(Modifier.width(8.dp))
                Text("Select Excel File")
            }

            AnimatedVisibility(visible = selectedFilePath != null) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CounsellingRoundSelector(counsellingRound, onCounsellingRoundChanged)

                    if (counsellingRound == 1) {
                        OutlinedTextField(
                            value = totalSeats,
                            onValueChange = onTotalSeatsChanged,
                            label = { Text("Total Number of Seats") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CategorySeatsInput(categorySeats, onCategorySeatsChanged)
                    }

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
fun CounsellingRoundSelector(
    selectedRound: Int,
    onRoundSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Select Counselling Round:", style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (round in 1..4) {
                FilterChip(
                    selected = selectedRound == round,
                    onClick = { onRoundSelected(round) },
                    label = { Text("Round $round") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun CategorySeatsInput(
    categorySeats: Map<String, String>,
    onCategorySeatsChanged: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categorySeats.forEach { (category, seats) ->
            OutlinedTextField(
                value = seats,
                onValueChange = { onCategorySeatsChanged(category, it) },
                label = { Text("$category Seats") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StudentAllocationCard(
    viewModel: GGVCounsellingViewModel,
    selectedStudentsCount: Int,
    counsellingRound: Int,
    totalSeats: String,
    categorySeats: Map<String, String>,
    onTotalSeatsChanged: (String) -> Unit,
    onCategorySeatsChanged: (String, String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Student Allocation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Selected Students: $selectedStudentsCount",
                style = MaterialTheme.typography.bodyLarge
            )

            if (counsellingRound == 1) {
                OutlinedTextField(
                    value = totalSeats,
                    onValueChange = onTotalSeatsChanged,
                    label = { Text("Total Number of Seats") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CategorySeatsInput(categorySeats, onCategorySeatsChanged)
            }

            Button(
                onClick = { viewModel.allocateSeats() },
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
    onExportFormat: (ExportFormat) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Allocated Students",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        StudentItem(student)
                    }
                }
            }

            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Export Results")
            }
            if (isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun StudentItem(student: Student) {
    val isWaitingList = student.name.contains("Waiting List")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp)),
        color = if (isWaitingList) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isWaitingList) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    student.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "App No: ${student.cuetApplicationNo}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "CUET Score: ${student.cuetScore}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Category: ${student.category}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isWaitingList) {
                Text(
                    "Waiting List",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by Application No. or Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filteredStudents = students.filter { student ->
                        searchQuery.isEmpty() || student.cuetApplicationNo.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                                student.name.contains(searchQuery, ignoreCase = true)
                    }
                    items(filteredStudents) { student ->
                        StudentSelectionItem(
                            student = student,
                            isSelected = selectedStudents.contains(student),
                            onSelectionChanged = { isSelected ->
                                selectedStudents = if (isSelected) {
                                    selectedStudents + student
                                } else {
                                    selectedStudents - student
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedStudents.toList()) }) {
                Text("Confirm (${selectedStudents.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StudentSelectionItem(
    student: Student,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) },
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectionChanged(it) }
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(student.name, fontWeight = FontWeight.Bold)
                Text(
                    "App. No: ${student.cuetApplicationNo}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("CUET Score: ${student.cuetScore}", style = MaterialTheme.typography.bodySmall)
                Text("Category: ${student.category}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}