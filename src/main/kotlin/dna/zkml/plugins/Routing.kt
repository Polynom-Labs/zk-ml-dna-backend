package dna.zkml.plugins

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class Research(
    val contractName: String,
    val title: String,
    val description: String,
    val modelFilePath: String
)

object ResearchRepository {

    private val researches = mutableListOf<Research>()

    fun add(research: Research) {
        researches.add(research)
    }

    fun size() = researches.size

    fun loadAll() = researches

}

fun Application.configureRouting() {
    routing {
        get("/researches") {
            call.respond(ResearchRepository.loadAll())
        }
    }

    routing {
        post("/upload") {
            val multipartData = call.receiveMultipart()

            var title = ""
            var description = ""
            val numLabel = (ResearchRepository.size() + 1).toString().padStart(3, '0')
            val appName = "zk_ml_dna_${numLabel}_v0"
            val contractName = "${appName}.aleo"
            val modelFilePath = "uploads/zk_ml_dna_${numLabel}_v0.ml"
            val modelFile = File(modelFilePath)
            modelFile.delete()
            modelFile.parentFile.mkdirs()
            modelFile.createNewFile()

            multipartData.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "title" -> title = part.value
                            "description" -> description = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        val fileBytes = part.streamProvider().readBytes()

                        modelFile.writeBytes(fileBytes)
                    }

                    else -> {}
                }
                part.dispose()
            }

            val processBuilder = ProcessBuilder()
                .command("python3", "python/ml_model_processor.py", modelFile.absolutePath, "gen/", appName)
            try {
                val process: Process = processBuilder.start()
                val reader = BufferedReader(InputStreamReader(process.errorStream))
                val line = reader.readText();
                println(line)

                val exitVal = process.waitFor()
                if (exitVal == 0) {
                    println("Project generation success")
                } else {
                    println("Project generation error: $exitVal")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val research = Research(
                contractName = contractName,
                title = title,
                description = description,
                modelFilePath = modelFilePath
            )
            ResearchRepository.add(
                research
            )

            call.respond(research)
        }
    }

}
