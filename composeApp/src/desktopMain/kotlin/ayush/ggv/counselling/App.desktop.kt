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
import java.io.PrintStream


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
                    val phoneNo = when (row.getCell(2)?.cellType) {
                        CellType.NUMERIC -> row.getCell(2).numericCellValue.toLong().toString()
                        CellType.STRING -> row.getCell(2).stringCellValue
                        else -> null
                    }
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
    format: ExportFormat
): Boolean {
    return withContext(Dispatchers.IO) {
        PrintStream(System.out).println("Exporting results to $filePath , format: $format")
        try {
            when (format) {
                ExportFormat.XLSX -> exportToExcel(allocatedStudents, filePath)
                ExportFormat.PDF -> exportToPDF(allocatedStudents, filePath)
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
    val document = Document(PageSize.A4, 20f, 20f, 20f, 20f)
    PdfWriter.getInstance(document, FileOutputStream(filePath))
    document.open()

    val sortedCategories = listOf("UR", "OBC", "SC", "ST", "PWD")

    sortedCategories.forEachIndexed { index, category ->
        if (index > 0) {
            document.newPage()
        }

        val students = allocatedStudents[category] ?: return@forEachIndexed

        // Add category title
        val categoryFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
        val categoryParagraph = Paragraph(category, categoryFont)
        categoryParagraph.alignment = Element.ALIGN_CENTER
        categoryParagraph.spacingAfter = 20f
        document.add(categoryParagraph)

        val table = PdfPTable(5)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(1f, 2f, 3f, 3f, 2f))

        // Add header row
        val headers = listOf("S.No.", "CUET App. No.", "Name", "Phone/Email", "CUET Score")
        headers.forEach {
            val cell = PdfPCell(Phrase(it, Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)))
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.verticalAlignment = Element.ALIGN_MIDDLE
            cell.paddingBottom = 8f
            cell.paddingTop = 8f
            cell.backgroundColor = BaseColor.LIGHT_GRAY
            table.addCell(cell)
        }

        // Add student data
        students.forEachIndexed { index, student ->
            addCellToTable(table, (index + 1).toString())
            addCellToTable(table, student.cuetApplicationNo)
            addCellToTable(table, student.name)
            addCellToTable(table, student.phoneNoEmail)
            addCellToTable(table, student.cuetScore.toString())
        }

        document.add(table)
    }

    document.close()
}

private fun addCellToTable(table: PdfPTable, content: String) {
    val cell = PdfPCell(Phrase(content, Font(Font.FontFamily.HELVETICA, 10f)))
    cell.horizontalAlignment = Element.ALIGN_CENTER
    cell.verticalAlignment = Element.ALIGN_MIDDLE
    cell.paddingTop = 5f
    cell.paddingBottom = 5f
    table.addCell(cell)
}