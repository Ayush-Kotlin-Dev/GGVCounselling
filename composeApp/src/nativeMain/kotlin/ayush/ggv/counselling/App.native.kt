package ayush.ggv.counselling

import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

actual fun readFileContent(filePath: String): List<String> {
    val content = NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null) ?: return emptyList()
    return content.split("\n")
}