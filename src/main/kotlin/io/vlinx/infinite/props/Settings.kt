package io.vlinx.infinite.props

import io.vlinx.infinite.constants.InfoModes
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2024/1/27
 */

@Component
@ConfigurationProperties("search-settings")
class Settings {
    var googleSearchApiKey = ""
    var googleSearchEngineId = ""
    var googleSearchEndpoint = "https://www.googleapis.com/customsearch/v1"

    var bingSearchApiKey = ""
    var bingSearchEndpoint = "https://api.bing.microsoft.com/v7.0/search"

    var openAiApiKey = ""

    var searchItemsLimit = 8
    var relatedQuestionsLimit = 5

    var aiModel = "openai"
    var searchEngine = "google"

    var infoMode = InfoModes.BRIEF

}