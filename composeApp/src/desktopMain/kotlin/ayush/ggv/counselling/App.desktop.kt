package ayush.ggv.counselling

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.CellType
import java.io.FileInputStream

actual fun processExcelFile(filePath: String): List<Student> {
    println("Starting to process Excel file: $filePath")
    val students = mutableListOf<Student>()
    try {
        FileInputStream(filePath).use { fis ->
            val workbook = WorkbookFactory.create(fis)
            val sheet = workbook.getSheetAt(0)
            println("Total rows in sheet: ${sheet.physicalNumberOfRows}")
            for (row in sheet.dropWhile { it.rowNum == 0 }) { // Skip header row
                try {
                    val applicationNo = row.getCell(0)?.stringCellValue
                    val name = row.getCell(1)?.stringCellValue
                    val phoneNo = row.getCell(2)?.stringCellValue
                    val email = row.getCell(3)?.stringCellValue
                    val cuetScore = when (row.getCell(4)?.cellType) {
                        CellType.NUMERIC -> row.getCell(4).numericCellValue.toInt()
                        CellType.STRING -> row.getCell(4).stringCellValue.toIntOrNull()
                        else -> null
                    }
                    val category = row.getCell(5)?.stringCellValue
                    val address = row.getCell(6)?.stringCellValue

                    println("Row ${row.rowNum}: AppNo=$applicationNo, Name=$name, Phone=$phoneNo, Email=$email, Score=$cuetScore, Category=$category, Address=$address")

                    if (name != null && phoneNo != null && email != null && cuetScore != null && category != null && address != null) {
                        students.add(Student(name, "$phoneNo, $email", cuetScore, category, address))
                        println("Added student: $name, CUET Score: $cuetScore")
                    } else {
                        println("Skipping row ${row.rowNum} due to missing data")
                    }
                } catch (e: Exception) {
                    println("Error processing row ${row.rowNum}: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    } catch (e: Exception) {
        println("Error processing Excel file: ${e.message}")
        e.printStackTrace()
    }
    println("Total students processed: ${students.size}")
    return students
}