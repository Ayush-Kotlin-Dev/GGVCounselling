package ayush.ggv.counselling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
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
    var initialStudentsCount by remember { mutableStateOf("") }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }

    val filePicker = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("xlsx", "xls")),
        mode = PickerMode.Single
    ) { file ->
          if (file != null) {
              selectedFilePath = file.path
          }
        }

    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { filePicker.launch() }) {
            Text("Select Excel File")
        }

        if (selectedFilePath != null) {
            Spacer(Modifier.height(16.dp))
            TextField(
                value = initialStudentsCount,
                onValueChange = { initialStudentsCount = it },
                label = { Text("Number of Initial Students") }
            )

            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                println("Processing file: $selectedFilePath")
                val count = initialStudentsCount.toIntOrNull() ?: 0
                println("Initial student count: $count")
                students = processExcelFile(selectedFilePath!!).sortedByDescending { it.cuetScore }.take(count)
                println("Processed students: ${students.size}")
            }) {
                Text("Process File")
            }
        }

        if (students.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Selected Students:", style = MaterialTheme.typography.h6)
            LazyColumn {
                items(students) { student ->
                    Text("${student.name} - CUET Score: ${student.cuetScore}")
                }
            }
        }
    }
}

data class Student(
    val name: String,
    val phoneNoEmail: String,
    val cuetScore: Int,
    val category: String,
    val address: String
)

expect fun processExcelFile(filePath: String): List<Student>