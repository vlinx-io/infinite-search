package io.vlinx.infinite.controller

import io.vlinx.infinite.constants.InfoModes
import io.vlinx.infinite.constants.Urls
import io.vlinx.infinite.exception.SearchException
import io.vlinx.infinite.props.Settings
import io.vlinx.infinite.rag.RAG
import io.vlinx.infinite.search.Google
import io.vlinx.infinite.search.SearchResult
import io.vlinx.infinite.utils.ExceptionUtils
import io.vlinx.infinite.utils.JSONUtils
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2024/1/28
 */

@RestController
class SearchController {

    @Autowired
    private lateinit var google: Google

    @Autowired
    private lateinit var rag: RAG

    @Autowired
    private lateinit var settings: Settings

    private val logger = LoggerFactory.getLogger(SearchController::class.java)

    private val LLM_SPLIT = "\n\n__LLM_RESPONSE__\n\n"
    private val RELATED_SPLIT = "\n\n__RELATED_QUESTIONS__\n\n"

    @CrossOrigin(origins = ["*"])
    @PostMapping(Urls.SEARCH)
    fun search(@RequestBody payload: String, response: HttpServletResponse) {

        val writer = response.outputStream.writer()
        try {
            val map = JSONUtils.fromJson<HashMap<String, String>>(payload)!!
            val query = map["query"]!!
            if (query.isBlank()) {
                throw SearchException("query is blank")
            }
            logger.info("RAG Search: $query")

            response.setHeader("Content-Type", "text/event-stream")
            response.contentType = "text/event-stream"
            response.characterEncoding = "UTF-8"
            response.setHeader("Pragma", "no-cache")

            val searchResults = google.search(query)

            writer.write(searchResultsToContexts(searchResults))
            writer.write(LLM_SPLIT)
            writer.flush()
            if(settings.infoMode == InfoModes.DETAIL) {
                writer.write("Waiting to load documents...\n\n")
                writer.flush()
            }

            val related = rag.search(query, searchResults, writer)
            val questions = related.split("\n")
            try {
                val list = mutableListOf<HashMap<String, String>>()
                for (question in questions) {
                    val map = HashMap<String, String>()
                    map["question"] = question.replaceFirst("^\\d+\\.".toRegex(), "").trim()
                    list.add(map)
                }
                writer.write(RELATED_SPLIT)
                writer.write(JSONUtils.toJson(list))
                writer.flush()
            } catch (t: Throwable) {
                logger.error("error when generate related questions: ${t.message}")
            }
        } catch (t: Throwable) {
            logger.error(ExceptionUtils.getStackTrace(t))
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            writer.write(t.message ?: "error")
            writer.flush()
        } finally {
            writer.close()
        }

    }

    private fun searchResultsToContexts(searchResults: List<SearchResult>): String {

        val list = mutableListOf<HashMap<String, String>>()

        for ((index, result) in searchResults.withIndex()) {
            val map = HashMap<String, String>()
            map["id"] = index.toString()
            map["name"] = result.title
            map["url"] = result.link
            list.add(map)
        }


        return JSONUtils.toJson(list.subList(0, settings.relatedQuestionsLimit))
    }


}