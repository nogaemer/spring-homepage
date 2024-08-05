package de.nogaemer.springhomepage.logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async

open class LogService {
    private val logger = LoggerFactory.getLogger(LogService::class.java)

    @Async
    open fun logAsync(message: String) {
        logger.info(message)
    }
}