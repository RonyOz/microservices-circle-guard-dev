plugins {
    id("org.springframework.boot") version "3.2.4" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("org.sonarqube") version "5.1.0.4882" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
}

allprojects {
    group = "com.circleguard"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}


subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.sonarqube")
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "implementation"(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("com.h2database:h2")
        // Observability: Prometheus metrics, distributed tracing, JSON logging
        "runtimeOnly"("io.micrometer:micrometer-registry-prometheus")
        "implementation"("io.micrometer:micrometer-tracing-bridge-brave")
        "implementation"("io.zipkin.reporter2:zipkin-reporter-brave")
        "implementation"("net.logstash.logback:logstash-logback-encoder:7.4")
        // Resilience: Circuit Breaker (Resilience4j) + AOP for @CircuitBreaker aspect
        "implementation"("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
        "implementation"("org.springframework.boot:spring-boot-starter-aop")
        // Integration tests: WireMock for HTTP-level inter-service stubbing
        "testImplementation"("org.wiremock:wiremock-standalone:3.3.1")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoReport> {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
