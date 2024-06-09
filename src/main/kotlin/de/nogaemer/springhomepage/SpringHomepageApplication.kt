package de.nogaemer.springhomepage

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@SpringBootApplication
@RestController
@EnableScheduling
class SpringHomepageApplication {

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedOrigins("*")
                    .allowedMethods("*")
                    .allowedHeaders("*")
                    .allowCredentials(false).maxAge(3600)
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SpringHomepageApplication>(*args){
        addInitializers(EnvironmentVariablesLogger())
    }
}

class EnvironmentVariablesLogger : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val environment: Environment = applicationContext.environment
        val mongoDatabase = environment.getProperty("env.MONGO_DATABASE")
        val mongoUser = environment.getProperty("env.MONGO_USER")
        val mongoPassword = environment.getProperty("env.MONGO_PASSWORD")
        val mongoCluster = environment.getProperty("env.MONGO_CLUSTER")
        val jwtSecretKey = environment.getProperty("env.JWT_SECRET_KEY")

        println("MONGO_DATABASE: $mongoDatabase")
        println("MONGO_USER: $mongoUser")
        println("MONGO_PASSWORD: $mongoPassword")
        println("MONGO_CLUSTER: $mongoCluster")
        println("JWT_SECRET_KEY: $jwtSecretKey")
    }
}
