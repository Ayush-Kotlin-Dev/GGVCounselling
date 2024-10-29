package ayush.ggv.counselling

import androidx.compose.foundation.layout.*
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
    val fileType = listOf("txt")  // Changed to .txt for simplicity

    FilePicker(show = showFilePicker, fileExtensions = fileType) { platformFile ->
        showFilePicker = false
        platformFile?.let {
            selectedFilePath = it.path
        }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = {
            showFilePicker = true
        }) {
            Text("Select Text File")
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
                val count = initialStudentsCount.toIntOrNull() ?: 0
                students = processTextFile(selectedFilePath!!).sortedByDescending { it.cuetScore }.take(count)
            }) {
                Text("Process File")
            }
        }

        if (students.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Selected Students:", style = MaterialTheme.typography.h6)
            students.forEach { student ->
                Text("${student.name} - CUET Score: ${student.cuetScore}")
            }
        }
    }
}

data class Student(
    val name: String,
    val cuetScore: Int,
    val email: String,
    val number: String
)

expect fun readFileContent(filePath: String): List<String>

fun processTextFile(filePath: String): List<Student> {
    return readFileContent(filePath).mapNotNull { line ->
        parseStudentData(line)
    }
}

fun parseStudentData(line: String): Student? {
    val parts = line.split(",")
    if (parts.size >= 4) {
        val name = parts[0].trim()
        val cuetScore = parts[1].trim().toIntOrNull() ?: return null
        val email = parts[2].trim()
        val number = parts[3].trim()
        return Student(name, cuetScore, email, number)
    }
    return null
}