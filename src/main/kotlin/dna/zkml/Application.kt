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
import kotlin.io.path.writeLines

private const val ML_FUNCTION_PLACEHOLDER = "// *** ML_FUNCTION_PLACEHOLDER ***"
private const val ML_INPUT_PLACEHOLDER = "// *** ML_INPUT_PLACEHOLDER ***"

fun main() {
    modifyLeoProjectTemplate("gen/zk_ml_dna_001_v0")
}
fun modifyLeoProjectTemplate(generatedProjectPath: String) {
    val mainLeo = File(generatedProjectPath + "/src", "main.leo")

    val functionLines = mutableListOf<String>()
    var mlInputs = ""

    var transitionStarted = false
    mainLeo.readLines().map { it.trim() }.forEach {line ->
        if (line.startsWith("transition main")) {
            transitionStarted = true
            val paramsStart = line.indexOfFirst { it == '(' }
            val paramsEnd = line.indexOfFirst { it == ')'}
            mlInputs = line.substring(paramsStart + 1, paramsEnd) // params w/o brackets

            val postfix = line.substring(paramsEnd + 1)
            val rtStart = postfix.indexOf('(')
            val rtEnd = postfix.indexOf(')')
            val returnType = postfix.substring(rtStart + 1, rtEnd)
            functionLines.add("function ml ($mlInputs) -> $returnType {")
        } else if (transitionStarted) {
            if (line.isNotBlank()) {
                functionLines.add(line)
            }
        }
    }
    functionLines.removeAt(functionLines.size - 1) // remove last bracket
    println(mlInputs)
    functionLines.forEach {
        println(it)
    }

    val file = File("leo/template/src/main.leo")
    val templateLines = file.readLines()
    val contractLines = mutableListOf<String>()
    templateLines.forEach { templateLine ->
        if (templateLine.contains(ML_INPUT_PLACEHOLDER)) {
            contractLines.add(mlInputs)
        } else if (templateLine.contains(ML_FUNCTION_PLACEHOLDER)) {
            functionLines.forEach { functionLine ->
                contractLines.add(functionLine)
            }
        } else {
            contractLines.add(templateLine)
        }
    }
    file.toPath().writeLines(contractLines)
}
fun main2() {
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
