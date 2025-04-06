//package com.seongjun.chatback.config
//
//import org.springframework.cache.annotation.EnableCaching
//import org.springframework.context.annotation.Configuration
//import java.util.concurrent.TimeUnit
//import com.github.benmanes.caffeine.cache.Caffeine
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.cache.CacheManager
//import org.springframework.cache.caffeine.CaffeineCacheManager
//import org.springframework.context.annotation.Bean
//
//@Configuration
//@EnableCaching
//class CacheConfig {
//
//    @Value("\${app.cache.room-ttl}")
//    private val roomCacheTtl: Long = 3600
//
//    @Value("\${app.cache.user-ttl}")
//    private val userCacheTtl: Long = 3600
//
//    @Bean
//    fun roomCacheManager(): CacheManager {
//        val cacheManager = CaffeineCacheManager("rooms", "roomMembers")
//        cacheManager.setCaffeine(
//            Caffeine.newBuilder()
//                .maximumSize(1000)
//                .expireAfterAccess(roomCacheTtl, TimeUnit.SECONDS)
//        )
//        return cacheManager
//    }
//
//    @Bean
//    fun userCacheManager(): CacheManager {
//        val cacheManager = CaffeineCacheManager("users", "userPresence")
//        cacheManager.setCaffeine(
//            Caffeine.newBuilder()
//                .maximumSize(5000)
//                .expireAfterAccess(userCacheTtl, TimeUnit.SECONDS)
//        )
//        return cacheManager
//    }
//}
