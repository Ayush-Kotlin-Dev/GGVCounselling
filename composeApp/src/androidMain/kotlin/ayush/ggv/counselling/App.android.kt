package ayush.ggv.counselling

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter

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


actual suspend fun exportResults(
    allocatedStudents: Map<String, List<Student>>,
    filePath: String,
    format: String
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            when (format) {
                "Excel" -> exportToExcel(allocatedStudents, filePath)
                "PDF" -> exportToPDF(allocatedStudents, filePath)
                else -> throw IllegalArgumentException("Unsupported format: $format")
            }
            println("Results exported successfully to $filePath")
            true
        } catch (e: Exception) {
            println("Error exporting results: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}

private fun exportToExcel(allocatedStudents: Map<String, List<Student>>, filePath: String) {
    val workbook = XSSFWorkbook()
    var rowNum = 0

    val sortedCategories = listOf("UR", "OBC", "SC", "ST", "PWD")

    sortedCategories.forEach { category ->
        val students = allocatedStudents[category] ?: return@forEach
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

        // Auto-size columns
        for (i in 0..4) {
            sheet.autoSizeColumn(i)
        }

        rowNum = 0 // Reset row number for next sheet
    }

    FileOutputStream(filePath).use { fileOut ->
        workbook.write(fileOut)
    }
    workbook.close()
}

private fun exportToPDF(allocatedStudents: Map<String, List<Student>>, filePath: String) {
    val document = Document()
    PdfWriter.getInstance(document, FileOutputStream(filePath))
    document.open()

    val sortedCategories = listOf("UR", "OBC", "SC", "ST", "PWD")

    sortedCategories.forEach { category ->
        val students = allocatedStudents[category] ?: return@forEach

        document.add(Paragraph(category, Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD)))
        document.add(Paragraph(" "))

        val table = PdfPTable(5)
        table.widthPercentage = 100f

        // Add header row
        listOf("Name", "Phone/Email", "CUET Score", "Category", "Address").forEach {
            table.addCell(PdfPCell(Phrase(it, Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD))))
        }

        // Add student data
        students.forEach { student ->
            table.addCell(student.name)
            table.addCell(student.phoneNoEmail)
            table.addCell(student.cuetScore.toString())
            table.addCell(student.category)
            table.addCell(student.address)
        }

        document.add(table)
        document.add(Paragraph(" "))
    }

    document.close()
}