package io.vlinx.infinite.rag

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel
import dev.langchain4j.model.input.PromptTemplate
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiModelName
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import dev.langchain4j.model.openai.OpenAiTokenizer
import dev.langchain4j.model.output.Response
import dev.langchain4j.store.embedding.EmbeddingMatch
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.vlinx.infinite.constants.AIModels
import io.vlinx.infinite.constants.InfoModes
import io.vlinx.infinite.exception.SearchException
import io.vlinx.infinite.props.Settings
import io.vlinx.infinite.search.Google
import io.vlinx.infinite.search.SearchEngine
import io.vlinx.infinite.search.SearchResult
import io.vlinx.infinite.utils.HttpUtils
import okhttp3.internal.wait
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.StringWriter
import java.io.Writer
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.stream.Collectors.joining


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2024/1/27
 */

@Component
class RAG {

    @Autowired
    lateinit var google: Google

    @Autowired
    lateinit var settings: Settings

    private val logger = LoggerFactory.getLogger(RAG::class.java)

    private val ragText = """
        You are a large language AI assistant built by VLINX Software. You are given a user question, and please write clean, concise and accurate answer to the question. You will be given a set of related contexts to the question.

        Your answer must be correct, accurate and written by an expert using an unbiased and professional tone. Please limit to 1024 tokens. Do not give any information that is not related to the question, and do not repeat. Say "information is missing on" followed by the related topic, if the given context do not provide sufficient information.

        your answer must be written in the same language as the question.

        Here are the set of contexts:
        
        {{context}}
        
        Remember, don't blindly repeat the contexts verbatim. And here is the user question:
    """.trimIndent()

    private val moreQuestionsText = """
        You are a helpful assistant that helps the user to ask related questions, based on user's original question and the related contexts. Please identify worthwhile topics that can be follow-ups, and write questions no longer than 20 words each. Please make sure that specifics, like events, names, locations, are included in follow up questions so they can be asked standalone. For example, if the original question asks about "the Manhattan project", in the follow up question, do not just say "the project", but use the full name "the Manhattan project". Your related questions must be in the same language as the original question.
        Here are the contexts of the question:
            
        Remember, based on the original question and related contexts, suggest three such further questions. Do NOT repeat the original question. Each related question should be no longer than 20 words. Here is the original question:
    """.trimIndent()

    fun search(question: String): String {
        val writer = StringWriter()
        search(question, writer)
        writer.close()
        return writer.toString()
    }

    fun search(question: String, writer: Writer) {
        val results = getSearchEngine().search(question)
        search(question, results, writer)
    }

    fun search(question: String, searchResults: List<SearchResult>, writer: Writer): String {
        val question = question.replace(Regex("\\[/?INST\\]"), "")
        val promptTemplate = PromptTemplate.from(
            ragText
        )

        val context = getContext(question, searchResults)
        val variables: MutableMap<String, Any> = HashMap()
        variables["context"] = context

        val prompt = promptTemplate.apply(variables)
        val chatModel = getStreamModel()

        val messages = listOf(prompt.toSystemMessage(), UserMessage(question))

        var streamDone = false
        val streamHandler = object : StreamingResponseHandler<AiMessage> {
            override fun onNext(token: String) {
                synchronized(writer) {
                    writer.write(token)
                }
            }

            override fun onComplete(response: Response<AiMessage>) {
                synchronized(writer) {
                    writer.flush()
                    streamDone = true
                }
            }

            override fun onError(error: Throwable) {
                synchronized(writer) {
                    writer.write("Something went wrong: ${error.message}")
                    writer.flush()
                    streamDone = true
                }
            }
        }

        chatModel.generate(messages, streamHandler)

        while (!streamDone) {
            synchronized(writer) {
                try {
                    writer.flush()
                } catch (t: Throwable) {
                    //ignore
                }
            }
            Thread.sleep(100)
        }

        return moreQuestions(question, context)
    }

    private fun getContext(question: String, searchResults: List<SearchResult>): String {
        return if (settings.infoMode == InfoModes.DETAIL) {
            getContextDetail(question, searchResults)
        } else {
            getContextBrief(question, searchResults)
        }
    }

    private fun getContextBrief(question: String, searchResults: List<SearchResult>): String {
        val builder = StringBuilder()
        for (result in searchResults) {
            builder.append(result.snippet).append("\n\n")
        }
        return builder.toString()
    }

    private fun getContextDetail(question: String, searchResults: List<SearchResult>): String {

        val documents = mutableListOf<Document>()
        val executor = Executors.newVirtualThreadPerTaskExecutor()
        val countDownLatch = CountDownLatch(searchResults.size)

        for ((index, result) in searchResults.withIndex()) {
            executor.submit {
                try {
                    synchronized(documents) {
                        val text = if (index < 2) {
                            getContentFromSearchResultDetail(result)
                        } else {
                            getContentFromSearchResultBrief(result)
                        }
                        if (text.isNotBlank()) {
                            documents.add(Document(text))
                        } else {
                            logger.error("Empty text from ${result.link}")
                        }
                    }
                } catch (t: Throwable) {
                    logger.error("Failed to load document from ${result.link}")
                } finally {
                    countDownLatch.countDown()
                }
            }
        }
        executor.shutdown()
        countDownLatch.await()
        val segments = mutableListOf<TextSegment>()

        for (document in documents) {
            try {
                val splitter = DocumentSplitters.recursive(100, 0, OpenAiTokenizer(OpenAiModelName.GPT_3_5_TURBO))
                val segs = splitter.split(document)
                segments.addAll(segs)
            } catch (t: Throwable) {
                logger.error("Failed to split document: ${document.metadata()}")
            }
        }

        if (segments.isEmpty()) {
            throw SearchException("No segments found for question: $question")
        }

        val embeddingModel = AllMiniLmL6V2EmbeddingModel()
        val embeddings = embeddingModel.embedAll(segments).content()

        val embeddingStore = InMemoryEmbeddingStore<TextSegment>()
        embeddingStore.addAll(embeddings, segments)

        val questionEmbedding = embeddingModel.embed(question).content()
        val maxResults = 30
        val minScore = 0.8
        val relevantEmbeddings = embeddingStore.findRelevant(questionEmbedding, maxResults, minScore)

        val context: String = relevantEmbeddings.stream()
            .map { match: EmbeddingMatch<TextSegment> ->
                match.embedded().text()
            }
            .collect(joining("\n\n"))

        return URLEncoder.encode(context, StandardCharsets.UTF_8)
    }

    private fun getContentFromSearchResultDetail(searchResult: SearchResult): String {
        try {
            val link = searchResult.link
            logger.info("Loading document from $link")
            val html = HttpUtils.get(link)
            val doc = Jsoup.parse(html)
            val text = if (doc.getElementsByTag("main").size > 0) {
                doc.getElementsByTag("main")[0].text()
            } else {
                doc.body().text()
            }
            if (text.trim().isBlank()) {
                logger.error("Empty content from ${searchResult.link}, use snippet instead")
                return searchResult.snippet
            }

            return searchResult.snippet + "\n\n" + text
        } catch (t: Throwable) {
            logger.error("Failed to load document from ${searchResult.link}, use snippet instead")
            return searchResult.snippet
        }

    }

    private fun getContentFromSearchResultBrief(searchResult: SearchResult): String {
        return searchResult.snippet
    }

    private fun moreQuestions(question: String, context: String): String {
        val chatModel = getModel()
        val promptTemplate = PromptTemplate.from(
            moreQuestionsText
        )
        val variables: MutableMap<String, Any> = HashMap()
        variables["context"] = context

        val prompt = promptTemplate.apply(variables)
        val messages = listOf(prompt.toSystemMessage(), UserMessage(question))
        val response = chatModel.generate(messages)
        return response.content().text()
    }

    private fun getModel(): ChatLanguageModel {
        when (settings.aiModel) {
            AIModels.OPENAI -> {
                return OpenAiChatModel.withApiKey(settings.openAiApiKey)
            }

            else -> {
                throw IllegalArgumentException("Unsupported AI model: ${settings.aiModel}")
            }
        }
    }

    private fun getStreamModel(): StreamingChatLanguageModel {
        when (settings.aiModel) {
            AIModels.OPENAI -> {
                return OpenAiStreamingChatModel.withApiKey(settings.openAiApiKey)
            }

            else -> {
                throw IllegalArgumentException("Unsupported AI model: ${settings.aiModel}")
            }
        }
    }

    private fun getSearchEngine(): SearchEngine {
        when (settings.searchEngine) {
            "google" -> {
                return google
            }

            else -> {
                throw IllegalArgumentException("Unsupported search engine: ${settings.searchEngine}")
            }
        }
    }

}