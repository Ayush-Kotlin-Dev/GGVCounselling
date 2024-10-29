package ayush.ggv.counselling

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.CellType
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

actual fun processExcelFile(filePath: String): List<Student> {
    val students = mutableListOf<Student>()
    val file = File(filePath)
    if (!file.exists()) {
        throw FileNotFoundException("File not found: $filePath")
    }
    println("Processing file at path: $filePath")

    FileInputStream(file).use { fis ->
        val workbook = WorkbookFactory.create(fis)
        val sheet = workbook.getSheetAt(0)
        for (row in sheet.dropWhile { it.rowNum == 0 }) { // Skip header row
            val applicationName = row.getCell(0)?.stringCellValue ?: continue
            val phoneNoEmail = row.getCell(1)?.stringCellValue ?: continue
            val cuetScore = when (row.getCell(2)?.cellType) {
                CellType.NUMERIC -> row.getCell(2).numericCellValue.toInt()
                CellType.STRING -> row.getCell(2).stringCellValue.toIntOrNull() ?: continue
                else -> continue
            }
            val category = row.getCell(3)?.stringCellValue ?: continue
            val address = row.getCell(4)?.stringCellValue ?: continue

            students.add(Student(applicationName, phoneNoEmail, cuetScore, category, address))
        }
    }
    return students
}