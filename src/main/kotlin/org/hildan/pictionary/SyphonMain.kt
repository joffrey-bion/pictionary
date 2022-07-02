package org.hildan.pictionary

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import org.hildan.pictionary.api.*
import org.hildan.pictionary.storage.CategorizedImageStore
import kotlin.io.path.Path
import kotlin.random.Random

private const val WORD_COUNT_LIMIT = 5000

// Low win rate so we repeat lots of words in a single game.
// This assumes the game doesn't give twice the same word in the same game (unchecked assumption)
private const val WIN_RATE = 0.05

private val LANG: Language = Language.ENGLISH

/**
 * Fetches lots of word images from the game http://www.edust.net/pct.
 */
suspend fun main() {
    val imageStore = CategorizedImageStore(Path("data-${LANG.id}"))
    val stats = Stats()

    try {
        fetchWordImages(LANG).take(WORD_COUNT_LIMIT).collect { image ->
            val newImageAdded = imageStore.save(image.category, image.imageBytes)

            stats.countWord(newImageAdded)

            print("\r$stats, ${imageStore.size} total images")
        }
    } finally {
        imageStore.flushToDisk()
    }
}

private fun percent(fraction: Int, total: Int) = fraction * 100 / total

private class Stats {
    var newWords = 0
    var alreadyKnown = 0
    val processed: Int get() = newWords + alreadyKnown

    fun countWord(new: Boolean) {
        if (new) newWords++ else alreadyKnown++
    }

    override fun toString(): String {
        val percentProcessed = percent(processed, WORD_COUNT_LIMIT)
        val percentNew = percent(newWords, processed)
        val processedInfo = "$processed/$WORD_COUNT_LIMIT ($percentProcessed%) processed images"
        val newVsKnownInfo = "$newWords added, $alreadyKnown skipped - $percentNew% new"
        return "$processedInfo ($newVsKnownInfo)"
    }
}

private class CategorizedImage(
    val imageBytes: ByteArray,
    val category: Category,
)

private fun fetchWordImages(language: Language) = playGamesIndefinitely(language).map {
    CategorizedImage(imageBytes = it.wordImageBytes, category = it.category)
}

private fun playGamesIndefinitely(language: Language) = flow {
    val dummyTeams = listOf(
        Team(piece = Piece.BLUE, playerNames = listOf("Michel", "Bob")),
        Team(piece = Piece.GREEN, playerNames = listOf("Fred", "Georges")),
    )
    // using allPlayRate 100% so we can pick winning team at random without checking if it's playing
    // (picking at random so we play longer the same game to reduce duplicate words)
    val gameConfig = GameConfig(language = language, teams = dummyTeams, allPlayRate = 1.0)
    val teamIds = (1..dummyTeams.size).toList()

    val client = PictionaryClient(gameConfig)
    while (true) {
        println("\nStarting new game to generate words")
        var state = client.newGame()
        this.emit(state)
        while (!state.reachedFinalSquare) {
            state = client.endTurn(randomWinner(teamIds))
            this.emit(state)
        }
    }
}

private fun randomWinner(teamIds: List<Int>): Int {
    val nextDouble = Random.nextDouble()
    if (nextDouble < WIN_RATE) {
        return teamIds.random()
    }
    return 0 // 0 = no winner
}
