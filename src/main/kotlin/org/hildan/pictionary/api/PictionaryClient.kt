package org.hildan.pictionary.api

import com.tfowl.ktor.client.features.JsoupFeature
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class PictionaryClient(private val config: GameConfig) {

    private val httpClient: HttpClient = HttpClient {
        install(HttpRedirect) {
            checkHttpMethod = false // allow POST redirects
        }
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
        install(JsoupFeature)
    }

    suspend fun newGame(): GameState {
        // sets up the language in the session
        httpClient.get<String>("http://www.edust.net/pct/${config.language.newGamePage}")

        val plateauPage = httpClient.submitForm<Document>(
            url = "http://www.edust.net/pct/p?page=config&type=new&action=submit",
            formParameters = config.toHttpParameters(),
        )

        return parseState(plateauPage)
    }

    suspend fun endTurnTimeOut() = endTurn(winningTeam = 0)

    suspend fun endTurn(winningTeam: Int): GameState {
        httpClient.submitForm<String>(
            url = "http://www.edust.net/pct/p?page=game&action=result&team=$winningTeam",
            formParameters = Parameters.build {
                // this seems to be the number of re-rolls still available
                append("t", "5")
            },
        )
        return fetchPlateauState()
    }

    private suspend fun fetchPlateauState(): GameState {
        val plateauDocument = httpClient.get<Document>("http://www.edust.net/pct/${config.language.boardPage}")
        return parseState(plateauDocument)
    }

    private suspend fun parseState(dom: Document) = GameState(
        wordImageBytes = fetchWordImage(parseWordId(plateauHtml = dom.toString())),
        category = dom.findCategory(),
        pieces = dom.findPlacedPieces(),
    )

    private suspend fun fetchWordImage(wordId: String) =
        httpClient.get<ByteArray>("http://www.edust.net/pct/w?p=$wordId")
}

private fun GameConfig.toHttpParameters() = Parameters.build {
    append("f_team_nb", teams.size.toString())

    teams.forEachIndexed { index, team ->
        val prefix = "f_t${index + 1}"
        append("${prefix}_size", team.playerNames.size.toString())
        append("${prefix}_pion_id_cached", team.piece.id.toString())
        append("${prefix}_pion_id", team.piece.id.toString())

        team.playerNames.forEachIndexed { playerIndex, playerName ->
            append("${prefix}_p${playerIndex + 1}_name", playerName)
        }
    }
    append("f_defi_taux", allPlayRate.toString())
    append("f_sablier_timeout", "60")
    append("f_animation_dice", "0")
    append("f_enable_sound", "1")
}

/*
This contains the URL of the image:

<script language="javascript">
$img=$("<img/>").attr("src", "w?p=1642296316_a1e287b89912193990f382b2325d2814").hide().bind("mouseout", hideWord);
$("#w").prepend($img).on("click", displayTempoWord);
 */
private val imgIdRegex = Regex("""\${'$'}img=\${'$'}\("<img/>"\)\.attr\("src", "w\?p=(\w+)"\)\.hide\(\)""")

private fun parseWordId(plateauHtml: String): String {
    val match = imgIdRegex.find(plateauHtml)
    return match?.groupValues?.get(1) ?: error("Couldn't find image ID")
}

private fun Document.findCategory(): Category {
    val categoryWrapper = getElementById("section_categorie_mot") ?: error("category not found")
    val divCategoryCell = categoryWrapper.getElementsByClass("legend_case")
    return Category.byDisplayName(divCategoryCell.text())
}

/*
The HTML for placed pieces is actually set via this JS in plateau's HTML:

<script language='javascript'>plateauReset() ;document.getElementById('cell2').innerHTML = "<span class='pion'><center><img src='images/pions/cat_16.png' width='16' height='16'></center></span>" ;
document.getElementById('cell1').innerHTML = "<span class='pion'><center><img src='images/pions/green_16.png' width='16' height='16'></center></span>" ;
 */
val piecesPlacementJsRegex = Regex("""document\.getElementById\('cell(\d+)'\)\.innerHTML = "([^"]*)"""")

private fun Document.findPlacedPieces(): List<PlacedPiece> {
    val placementJs = getElementsByTag("script").first { it.data().startsWith("plateauReset()") }.data()
    return piecesPlacementJsRegex.findAll(placementJs).toList().flatMap {
        val (_, cell, html) = it.groupValues
        val cellNum = cell.toInt()
        Jsoup.parse(html).getElementsByClass("pion").flatMap { pieceSpan ->
            pieceSpan.getElementsByTag("img").map { pieceImg ->
                val piece = Piece.findByImage(pieceImg.attr("src"))
                PlacedPiece(piece, cellNum)
            }
        }
    }
}
