package io.vlinx.infinite


import io.vlinx.infinite.constants.App
import io.vlinx.infinite.props.Settings
import io.vlinx.infinite.rag.RAG
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


private val logger = LoggerFactory.getLogger(InfiniteApplication::class.java)


@SpringBootApplication
class InfiniteApplication : CommandLineRunner {

    @Autowired
    private lateinit var settings: Settings

    override fun run(vararg args: String?) {
        logger.info("${App.NAME} ${App.VERSION} Started")
        logger.info("Search Info Mode: ${settings.infoMode}")
    }
}

fun main(args: Array<String>) {
    runApplication<InfiniteApplication>(*args)
}
