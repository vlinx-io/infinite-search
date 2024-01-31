package io.vlinx.infinite

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import dev.langchain4j.model.output.Response
import io.vlinx.infinite.props.Settings
import io.vlinx.infinite.rag.RAG
import io.vlinx.infinite.search.Google
import okhttp3.internal.wait
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.StringWriter


@SpringBootTest
class InfiniteApplicationTests {

    @Autowired
    private lateinit var google: Google

    @Autowired
    private lateinit var rag: RAG

    @Autowired
    private lateinit var settings: Settings

    @Test
    fun contextLoads() {
    }

    @Test
    fun googleSearch() {
        val result = google.search("How to protect java code with Protector4J")
        println(result)
    }

}
