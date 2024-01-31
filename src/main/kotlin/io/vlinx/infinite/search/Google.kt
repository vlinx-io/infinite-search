package io.vlinx.infinite.search

import io.vlinx.infinite.exception.SearchException
import io.vlinx.infinite.props.Settings
import io.vlinx.infinite.utils.HttpUtils
import io.vlinx.infinite.utils.JSONUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URLEncoder


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2024/1/27
 */

@Component
class Google : SearchEngine {

    @Autowired
    lateinit var settings: Settings

    override fun search(keyword: String): List<SearchResult> {
        val url = makeSearchUrl(keyword)
        val content = HttpUtils.get(url)
        val map = JSONUtils.fromJson<Map<String, String>>(content)!!
        if (map["items"] == null) {
            throw SearchException("Google search error: $content")
        }

        var items = map["items"] as List<Map<String, String>>
        items = items.subList(0, settings.searchItemsLimit)

        val results = mutableListOf<SearchResult>()

        for (item in items) {
            val result = SearchResult(
                item["title"]!!,
                item["link"]!!,
                item["snippet"]!!
            )
            results.add(result)
        }

        return results
    }


    private fun makeSearchUrl(keyword: String): String {
        return "${settings.googleSearchEndpoint}?key=${settings.googleSearchApiKey}&cx=${settings.googleSearchEngineId}&q=${
            URLEncoder.encode(
                keyword, "UTF-8"
            )
        }"

    }


}