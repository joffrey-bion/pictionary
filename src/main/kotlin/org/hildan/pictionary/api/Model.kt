package org.hildan.pictionary.api

private const val LAST_CELL = 55

data class GameConfig(
    val language: Language = Language.FRENCH,
    val teams: List<Team>,
    val allPlayRate: Double = 0.3,
)

enum class Language(
    val id: String,
    /** ISO 639-3 code */
    val codeIso3: String,
    val newGamePage: String,
    val boardPage: String,
) {
    FRENCH("fr", "fra", "nouvelle_partie", "plateau"),
    ENGLISH("en", "eng", "new_game", "board"),
}

data class Team(
    val piece: Piece,
    val playerNames: List<String>,
) {
    val size = playerNames.size
}

enum class Piece(
    val id: Int,
    val imageUrl: String,
) {
    BLUE(0, "images/pions/blue_16.png"),
    GREEN(1, "images/pions/green_16.png"),
    PURPLE(2, "images/pions/purple_16.png"),
    ART(3, "images/pions/art_16.png"),
    HEART(4, "images/pions/heart_16.png"),
    BOY(5, "images/pions/boy_16.png"),
    GIRL(6, "images/pions/girl_16.png"),
    CAT(7, "images/pions/cat_16.png"),
    DOG(8, "images/pions/dog_16.png"),
    PANDA(9, "images/pions/panda_16.png"),
    BASEBALL(10, "images/pions/pion_baseball.png"),
    FOOTBALL(11, "images/pions/pion_football.png"),
    GOLF(12, "images/pions/pion_golf.png"),
    TENNIS(13, "images/pions/pion_tennis.png"),
    BASKETBALL(14, "images/pions/pion_basketball.png"),
    SOCCER(15, "images/pions/pion_soccer.png");

    companion object {
        fun findByImage(src: String) = values().first { it.imageUrl == src }
    }
}

data class GameState(
    val wordImageBytes: ByteArray,
    val category: Category,
    val pieces: List<PlacedPiece>,
) {
    val reachedFinalSquare: Boolean = pieces.any { it.cell == LAST_CELL }

    override fun toString(): String {
        return "GameState(category=$category, reachedFinalSquare=$reachedFinalSquare, pieces=$pieces)"
    }
}

enum class Category(
    val id: String,
    val displayNames: List<String>,
) {
    PEOPLE_PLACE_ANIMAL(
        id = "personne-lieu-animal",
        displayNames = listOf("Personne / Lieu / Animal", "People / Place / Animal"),
    ),
    OBJECT(
        id = "objet",
        displayNames = listOf("Objet", "Object"),
    ),
    ACTION(
        id = "action",
        displayNames = listOf("Action"),
    ),
    DIFFICULT(
        id = "difficile",
        displayNames = listOf("Difficile", "Difficult"),
    ),
    ALL_PLAY(
        id = "defi",
        displayNames = listOf("Défi", "All Play"),
    );

    companion object {
        val ALL = values()

        fun byId(id: String) = values().first { c -> c.id == id }

        fun byDisplayName(name: String) = values().first { c -> c.displayNames.any { it == name } }
    }
}

data class PlacedPiece(val piece: Piece, val cell: Int)
