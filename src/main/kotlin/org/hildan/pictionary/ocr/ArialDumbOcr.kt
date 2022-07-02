package org.hildan.pictionary.ocr

import org.hildan.ocr.*
import org.hildan.ocr.reference.ReferenceImages
import org.hildan.ocr.reference.UniqueImageStore
import org.hildan.pictionary.storage.CategorizedImageStore
import java.awt.image.BufferedImage
import kotlin.io.path.*

class ArialDumbOcr : Ocr {
    private val knownLettersDir = Path("src/main/resources/ocr/arial")
    private val referenceImages = ReferenceImages.readFrom(knownLettersDir)

    // there seems to be 8 to 11 pixels of space between 2 letters when there is an actual space character
    private val dumbOcr = SimpleOcr(referenceImages, textColor = Color.WHITE, spaceWidthThreshold = 8)

    override fun recognizeText(image: BufferedImage): String = dumbOcr.recognizeText(image)
}

fun main() {

    val ocr = ArialDumbOcr()
    val outputDir = Path("src/main/resources/referenceLettersDraft").apply { createDirectories() }
    val imageStore = UniqueImageStore(outputDir)

    CategorizedImageStore(Path("data-en")).list().forEach { img ->
        try {
            val text = ocr.recognizeText(img.path.readImage())
            // println("Recognized '$text' in ${img.path}")
        } catch (e: NoAcceptableMatchException) {
            val path = imageStore.saveOrGetPath(e.unmatchedSubImage.toByteArray("png"))
            println("Unmatched sub-image written to $path")
        }
    }
}
