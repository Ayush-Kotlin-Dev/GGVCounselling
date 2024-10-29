package ayush.ggv.counselling

import java.io.File

actual fun readFileContent(filePath: String): List<String> {
    return File(filePath).readLines()
}