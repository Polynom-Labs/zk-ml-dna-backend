package dna.zkml

import dna.zkml.plugins.configureRouting
import dna.zkml.plugins.configureSecurity
import dna.zkml.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyStore
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.writeLines


fun main() {
    val environment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        connector {
            port = 8080
        }
        sslConnector(
            keyStore = KeyStore.getInstance(File("keystore.jks"), "123456".toCharArray()),
            keyAlias = "zkml",
            keyStorePassword = { "123456".toCharArray() },
            privateKeyPassword = { "123456".toCharArray() }) {
            port = 8443
            keyStorePath = File("keystore.jks")
        }
        module(Application::module)
    }

    embeddedServer(Netty, environment)
        .start(wait = true)

}

fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureRouting()
}
