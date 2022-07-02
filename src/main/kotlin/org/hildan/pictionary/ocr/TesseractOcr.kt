package org.hildan.pictionary.ocr

import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage
import java.nio.file.Path

class TesseractOcr(
    private val dataDir: Path,
    private val languageIso3: String,
) : Ocr {
    private val tesseract = Tesseract().apply {
        setLanguage(languageIso3)
        setDatapath(dataDir.toString())
    }

    override fun recognizeText(image: BufferedImage): String = tesseract.doOCR(image).trim()
}
