package dna.zkml.plugins

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import java.io.File

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
            val label = (ResearchRepository.size() + 1).toString().padStart(3, '0')
            val contractName = "zk_ml_dna_${label}_v0.aleo"
            val modelFilePath = "uploads/zk_ml_dna_${label}_v0.ml"

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
                        val modelFile = File(modelFilePath)
                        modelFile.delete()
                        modelFile.parentFile.mkdirs()
                        modelFile.createNewFile()

                        modelFile.writeBytes(fileBytes)
                    }

                    else -> {}
                }
                part.dispose()
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
