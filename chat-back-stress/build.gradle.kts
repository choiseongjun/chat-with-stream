plugins {
    java
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.seongjun"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux") {
        exclude(module = "spring-boot-starter-tomcat")
    }
// https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-reactor-netty
    implementation("org.springframework.boot:spring-boot-starter-reactor-netty:3.4.4")
    // 추가로 Tomcat 의존성 제외
    configurations.all {
        exclude(group = "org.apache.tomcat.embed")
    }
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
// https://mvnrepository.com/artifact/org.projectlombok/lombok
//    compileOnly("org.projectlombok:lombok:1.18.38")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-redis-reactive
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-websocket
    implementation("org.springframework.boot:spring-boot-starter-websocket:3.4.4")
    // https://mvnrepository.com/artifact/jakarta.persistence/jakarta.persistence-api
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    // https://mvnrepository.com/artifact/org.springframework.data/spring-data-r2dbc
    implementation("org.springframework.data:spring-data-r2dbc:3.4.4")
    // https://mvnrepository.com/artifact/io.r2dbc/r2dbc-postgresql
// https://mvnrepository.com/artifact/org.postgresql/r2dbc-postgresql
    implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // 모니터링 의존성 추가
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

//    implementation("ch.qos.logback:logback-classic:1.5.6")
//    implementation("ch.qos.logback:logback-core:1.5.6")
//    implementation("org.slf4j:slf4j-api:2.1.0-alpha1")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
