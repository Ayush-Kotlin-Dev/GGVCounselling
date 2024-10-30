package ayush.ggv.counselling

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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

                    println("Row ${row.rowNum}: Name=$name, Phone=$phoneNo, Email=$email, Score=$cuetScore, Category=$category, Address=$address")

                    if (name != null && phoneNo != null && email != null && cuetScore != null && category != null && address != null) {
                        students.add(Student(name, "$phoneNo, $email", cuetScore, category, address))
                        println("Added student: $name, CUET Score: $cuetScore, Category: $category")
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

actual fun exportResults(allocatedStudents: Map<String, List<Student>>, filePath: String) {
    val workbook = XSSFWorkbook()
    var rowNum = 0

    allocatedStudents.forEach { (category, students) ->
        val sheet = workbook.createSheet(category)

        var row = sheet.createRow(rowNum++)
        var cell = row.createCell(0)
        cell.setCellValue(category)

        // Create header row
        row = sheet.createRow(rowNum++)
        val headers = listOf("Name", "Phone/Email", "CUET Score", "Category", "Address")
        headers.forEachIndexed { index, header ->
            cell = row.createCell(index)
            cell.setCellValue(header)
        }

        // Add student data
        students.forEach { student ->
            row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(student.name)
            row.createCell(1).setCellValue(student.phoneNoEmail)
            row.createCell(2).setCellValue(student.cuetScore.toDouble())
            row.createCell(3).setCellValue(student.category)
            row.createCell(4).setCellValue(student.address)
        }

        rowNum += 2 // Add some space between categories
    }

    // Auto-size columns for all sheets
    for (i in 0 until workbook.numberOfSheets) {
        val sheet = workbook.getSheetAt(i)
        for (j in 0..4) { // Assuming 5 columns (0-4)
            sheet.autoSizeColumn(j)
        }
    }

    // Write the output to a file
    FileOutputStream(filePath).use { fileOut ->
        workbook.write(fileOut)
    }
    workbook.close()
    println("Results exported successfully to $filePath")
}