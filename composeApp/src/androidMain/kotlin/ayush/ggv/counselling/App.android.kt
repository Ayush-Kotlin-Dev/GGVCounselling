package ayush.ggv.counselling

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

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