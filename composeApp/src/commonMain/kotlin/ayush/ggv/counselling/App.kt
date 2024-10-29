package ayush.ggv.counselling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
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
    var showFilePicker by remember { mutableStateOf(false) }
    val fileType = listOf("xlsx", "xls")  // Excel file extensions

    FilePicker(show = showFilePicker, fileExtensions = fileType) { platformFile ->
        showFilePicker = false
        platformFile?.let {
            selectedFilePath = it.path
            println("Selected file path: ${it.path}") // Add this line
        }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = {
            showFilePicker = true
        }) {
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
                println("Processing file: $selectedFilePath") // Add this line
                val count = initialStudentsCount.toIntOrNull() ?: 0
                println("Initial student count: $count") // Add this line
                students = processExcelFile(selectedFilePath!!).sortedByDescending { it.cuetScore }.take(count)
                println("Processed students: ${students.size}") // Add this line
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