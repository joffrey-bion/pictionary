package org.hildan.pictionary.storage

import org.hildan.ocr.reference.UniqueImageStore
import java.nio.file.Path
import kotlin.io.path.*
import org.hildan.pictionary.api.*
import java.util.*

data class CategorizedImagePath(val category: Category, val path: Path)

class CategorizedImageStore(
    dataRootDir: Path,
    private val maxUnflushedImages: Int = 10,
) {
    private val imagesDir = dataRootDir.resolve("images")
    private val categoryFilesDir = dataRootDir.resolve("categories")

    private val imageStore = UniqueImageStore(imagesDir)

    private val pathsStore = CategorizedElementsStore(rootDir = categoryFilesDir)

    private var unflushedImages = 0

    val size: Int
        get() = pathsStore.size

    fun list() = pathsStore.list().asSequence().map { (cat, file) -> CategorizedImagePath(cat, imagesDir.resolve(file)) }

    fun save(category: Category, imageBytes: ByteArray): Boolean {
        val path = imageStore.saveOrGetPath(imageBytes)
        val added = pathsStore.save(category, imagesDir.relativize(path).toString())
        if (added) {
            unflushedImages++
            if (unflushedImages >= maxUnflushedImages) {
                flushToDisk()
            }
        }
        return added
    }

    fun flushToDisk() {
        pathsStore.flushToDisk()
        unflushedImages = 0
    }
}

data class CategorizedElement(val category: Category, val element: String)

class CategorizedElementsStore(private val rootDir: Path) {
    init {
        rootDir.createDirectories()
    }

    private val elementsByCategory = Category.ALL.associateWithTo(EnumMap(Category::class.java)) { cat ->
        cat.elementsFile.readLines().toMutableSet()
    }

    val size: Int
        get() = elementsByCategory.values.sumOf { it.size }

    fun list() = elementsByCategory.flatMap { (cat, elements) -> elements.map { CategorizedElement(cat, it) } }

    fun save(category: Category, element: String): Boolean = elementsByCategory.getValue(category).add(element)

    fun flushToDisk() {
        elementsByCategory.forEach { (cat, words) ->
            cat.elementsFile.writeLines(words.sorted())
        }
    }

    private val Category.elementsFile: Path
        get() = rootDir.resolve("$id.txt").apply {
            if (!exists()) {
                createFile()
            }
        }
}
