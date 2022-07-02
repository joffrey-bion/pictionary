package org.hildan.pictionary

import org.hildan.ocr.Color
import org.hildan.ocr.readImage
import org.hildan.pictionary.api.Language
import org.hildan.pictionary.ocr.ArialDumbOcr
import org.hildan.pictionary.ocr.TesseractOcr
import org.hildan.pictionary.storage.CategorizedElementsStore
import org.hildan.pictionary.storage.CategorizedImageStore
import java.awt.image.BufferedImage
import kotlin.io.path.Path

/*
DumbOCR has better overall results:
'P.m.u.' vs 'Pm.u.'
'T.G.V' vs 'T.GV'
'Chataîgne' vs 'Chataïgne'
'W.C.' vs 'W.c.'

However, Arial doesn't distinguish 'l' from 'I', so DumbOCR always picks 'l' (the only reference image of the 2).
We have to replace DumbOCR's results with the Is from Tesseract's results when they match a 'l'.
 */

private val LANG: Language = Language.ENGLISH

fun main() {
    val dataRootDir = Path("data-${LANG.id}")
    val categorizedImageStore = CategorizedImageStore(dataRootDir)
    val wordsStore = CategorizedElementsStore(rootDir = Path("src/main/resources/words-${LANG.id}"))

    val dumbOcr = ArialDumbOcr()
    val tesseract = TesseractOcr(Path("tesseract"), LANG.codeIso3)

    var nRecognized = 0
    val totalImages = categorizedImageStore.size

    categorizedImageStore.list().forEach { (cat, path) ->
        val image = path.readImage()

        val word = dumbOcr.recognizeText(image).rectifyLsAndIs(tesseract, image)

        nRecognized++
        print("\r$nRecognized/$totalImages images processed (${nRecognized * 100 / totalImages}%)")

        // TODO write to wordsStore
        wordsStore.save(cat, word)
    }

    wordsStore.flushToDisk()
}

private fun String.rectifyLsAndIs(tesseractOcr: TesseractOcr, image: BufferedImage): String {
    if ("l" !in this) {
        return this
    }
    val tesseractText = tesseractOcr.recognizeText(image.invertColors()) // Tesseract fucks up on white-on-black text

    if (this != tesseractText) {
        println("\rOCR mismatch '$this' vs '$tesseractText' (Dumb vs Tesseract)")
    }
    // ArialDumbOcr fucks up on capital I (can't distinguish from l)
    return replaceLsWithIsFrom(tesseractText)
}

private fun String.replaceLsWithIsFrom(text: String): String {
    return zip(text) { c1, c2 -> if (c1 == 'l' && c2 == 'I') 'I' else c1}.joinToString("")
}

private fun BufferedImage.invertColors(): BufferedImage {
    val invertedImg = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until height) {
        for (x in 0 until width) {
            invertedImg.setRGB(x, y, invertColor(getRGB(x, y)))
        }
    }
    return invertedImg
}

private fun invertColor(rgb: Int): Int = Color(rgb.toUInt()).invert().argb.toInt()

private fun Color.invert(): Color = Color(
    alpha = alpha.toUInt(),
    red = 255u - red,
    green = 255u - green,
    blue = 255u - blue,
)
