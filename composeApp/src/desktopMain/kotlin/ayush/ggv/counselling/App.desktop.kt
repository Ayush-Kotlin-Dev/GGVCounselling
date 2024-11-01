package ayush.ggv.counselling

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.io.FileOutputStream
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter


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
                    val cuetApplicationNo = row.getCell(0)?.stringCellValue
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

                    if (cuetApplicationNo != null && name != null && phoneNo != null && email != null && cuetScore != null && category != null && address != null) {
                        students.add(
                            Student(
                                cuetApplicationNo,
                                name,
                                "$phoneNo, $email",
                                cuetScore,
                                category,
                                address
                            )
                        )
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
        val headers = listOf("S.No.", "Cuet Application" , "Name", "Phone/Email", "CUET Score", "Category", "Address")
        headers.forEachIndexed { index, header ->
            cell = row.createCell(index)
            cell.setCellValue(header)
        }

        // Add student data
        students.forEachIndexed { index, student ->
            row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue((index + 1).toString())
            row.createCell(1).setCellValue(student.cuetApplicationNo)
            row.createCell(2).setCellValue(student.name)
            row.createCell(3).setCellValue(student.phoneNoEmail)
            row.createCell(4).setCellValue(student.cuetScore.toDouble())
            row.createCell(5).setCellValue(student.category)
            row.createCell(6).setCellValue(student.address)
        }

        // Auto-size columns
        for (i in 0..6) {
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

        val table = PdfPTable(6)
        table.widthPercentage = 100f

        // Add header row
        listOf("S.No.", "Cuet Application " , "Name", "Phone/Email", "CUET Score", "Category", "Address").forEach {
            table.addCell(PdfPCell(Phrase(it, Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD))))
        }

        // Add student data
        students.forEachIndexed { index, student ->
            table.addCell((index + 1).toString())
            table.addCell(student.cuetApplicationNo)
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