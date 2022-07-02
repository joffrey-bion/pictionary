package org.hildan.pictionary.ocr

import com.google.cloud.vision.v1.AnnotateImageRequest
import com.google.cloud.vision.v1.Feature
import com.google.cloud.vision.v1.Image
import com.google.cloud.vision.v1.ImageAnnotatorClient
import com.google.protobuf.ByteString
import org.hildan.ocr.toByteArray
import java.awt.image.BufferedImage

class GoogleVisionOcr : Ocr, AutoCloseable {

    private val vision = ImageAnnotatorClient.create()

    override fun recognizeText(image: BufferedImage): String = vision.recognizeText(image.toByteArray("png"))

    override fun close() {
        vision.close()
    }
}

private fun ImageAnnotatorClient.recognizeText(imageBytes: ByteArray): String {
    val request = annotateImageRequest(imageBytes, Feature.Type.TEXT_DETECTION)
    val res = annotateImage(request)
    if (res.hasError()) {
        error(res.error.message)
    }
    return res.textAnnotationsList.first().description.trim()
}

private fun annotateImageRequest(imageBytes: ByteArray, type: Feature.Type): AnnotateImageRequest {
    val img = Image.newBuilder().setContent(ByteString.copyFrom(imageBytes)).build()
    val feat = Feature.newBuilder().setType(type).build()
    return AnnotateImageRequest.newBuilder()
        .addFeatures(feat)
        .setImage(img)
        .build()
}

private fun ImageAnnotatorClient.annotateImage(request: AnnotateImageRequest) =
    batchAnnotateImages(listOf(request)).responsesList.single()
