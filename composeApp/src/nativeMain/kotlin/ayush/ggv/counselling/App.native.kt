package ayush.ggv.counselling

import platform.Foundation.*

actual fun readFileContent(filePath: String): List<String> {
    val content = NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)
    return content?.split("\n") ?: emptyList()
}

actual fun processExcelFile(filePath: String): List<Student> {
    TODO("Not yet implemented")
}