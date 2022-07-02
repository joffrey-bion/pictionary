package org.hildan.pictionary.ocr

import java.awt.image.BufferedImage

interface Ocr {

    fun recognizeText(image: BufferedImage): String
}
