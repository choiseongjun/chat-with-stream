package com.seongjun.chatback

import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChatBackApplication{
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
//            SpringApplication.run(ChatBackApplication::class.java, *args)
            val app = SpringApplication(ChatBackApplication::class.java)
            app.webApplicationType = WebApplicationType.REACTIVE
            app.run(*args)
        }
    }
}
//fun main(args: Array<String>) {
////    runApplication<ChatBackApplication>(*args)
//}
