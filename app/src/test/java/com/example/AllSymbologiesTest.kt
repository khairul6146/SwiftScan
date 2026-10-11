package com.example

import com.example.data.ScanResult
import com.example.util.QrGenerator
import com.example.util.ScanUtils
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.zxing.BarcodeFormat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AllSymbologiesTest {

    @Test
    fun testTestBenchPayloads_generationAndFormatMapping() {
        // 1. QR Code
        val qrBitmap = QrGenerator.generateBarcode("https://example.com/test-qr-2026", BarcodeFormat.QR_CODE, 256, 256)
        assertNotNull(qrBitmap)
        assertEquals("QR Code", ScanUtils.getFormatName(Barcode.FORMAT_QR_CODE))
        assertTrue(ScanUtils.is2dMatrix(Barcode.FORMAT_QR_CODE))

        // 2. Data Matrix
        val dmBitmap = QrGenerator.generateBarcode("DM-PART-TEST-7782A", BarcodeFormat.DATA_MATRIX, 256, 256)
        assertNotNull(dmBitmap)
        assertEquals("Data Matrix", ScanUtils.getFormatName(Barcode.FORMAT_DATA_MATRIX))
        assertTrue(ScanUtils.is2dMatrix(Barcode.FORMAT_DATA_MATRIX))

        // 3. PDF417
        val pdfBitmap = QrGenerator.generateBarcode("ID-PASSPORT-SAMPLE-2026", BarcodeFormat.PDF_417, 500, 200)
        assertNotNull(pdfBitmap)
        assertEquals("PDF417", ScanUtils.getFormatName(Barcode.FORMAT_PDF417))
        assertTrue(ScanUtils.isStacked2d(Barcode.FORMAT_PDF417))

        // 4. Aztec Code
        val aztecBitmap = QrGenerator.generateBarcode("AZTEC-CHECKIN-PASS-99", BarcodeFormat.AZTEC, 256, 256)
        assertNotNull(aztecBitmap)
        assertEquals("Aztec", ScanUtils.getFormatName(Barcode.FORMAT_AZTEC))
        assertTrue(ScanUtils.is2dMatrix(Barcode.FORMAT_AZTEC))

        // 5. Code 128
        val c128Bitmap = QrGenerator.generateBarcode("UNIT-PKG-992140", BarcodeFormat.CODE_128, 400, 150)
        assertNotNull(c128Bitmap)
        assertEquals("Code 128", ScanUtils.getFormatName(Barcode.FORMAT_CODE_128))
        assertTrue(ScanUtils.is1dLinear(Barcode.FORMAT_CODE_128))

        // 6. EAN-13 (Standard retail 12/13 digits)
        val eanBitmap = QrGenerator.generateBarcode("955123456789", BarcodeFormat.EAN_13, 400, 150)
        assertNotNull(eanBitmap)
        assertEquals("EAN-13", ScanUtils.getFormatName(Barcode.FORMAT_EAN_13))
        assertTrue(ScanUtils.is1dLinear(Barcode.FORMAT_EAN_13))

        // 7. Code 39
        val c39Bitmap = QrGenerator.generateBarcode("INV-100234", BarcodeFormat.CODE_39, 400, 150)
        assertNotNull(c39Bitmap)
        assertEquals("Code 39", ScanUtils.getFormatName(Barcode.FORMAT_CODE_39))
        assertTrue(ScanUtils.is1dLinear(Barcode.FORMAT_CODE_39))
    }

    @Test
    fun testSymbologyClassifierBuckets() {
        // Linear 1D
        assertTrue(ScanUtils.is1dLinear(Barcode.FORMAT_UPC_A))
        assertTrue(ScanUtils.is1dLinear(Barcode.FORMAT_UPC_E))
        assertTrue(ScanUtils.is1dLinear(Barcode.FORMAT_CODABAR))
        assertTrue(ScanUtils.is1dLinear(Barcode.FORMAT_ITF))
        assertFalse(ScanUtils.is1dLinear(Barcode.FORMAT_QR_CODE))

        // 2D Matrix
        assertTrue(ScanUtils.is2dMatrix(Barcode.FORMAT_QR_CODE))
        assertTrue(ScanUtils.is2dMatrix(Barcode.FORMAT_DATA_MATRIX))
        assertTrue(ScanUtils.is2dMatrix(Barcode.FORMAT_AZTEC))
        assertFalse(ScanUtils.is2dMatrix(Barcode.FORMAT_CODE_128))
    }
}
