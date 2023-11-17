package dna.zkml.plugins

import com.google.gson.Gson
import dna.zkml.createContractProjectFromTemplate
import dna.zkml.snarkOsDeploy
import dna.zkml.leoBuild
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class Storage(
    val researches: List<Research>
)
data class Research(
    val id: Long,
    val contractName: String,
    val title: String,
    val description: String,
    val modelFilePath: String
)

object ResearchRepository {

    private val researchesInMemory = mutableListOf<Research>()


    private val STORAGE_FILE = "storage.json"
    init {
        val storageFile = File(STORAGE_FILE)
        if (storageFile.exists()) {
            val json = storageFile.readText()
            val storageObject = Gson().fromJson(json, Storage::class.java)
            researchesInMemory.addAll(storageObject.researches)
        }
    }
    fun add(research: Research) {
        researchesInMemory.add(research)

        val json = Gson().toJson(Storage(researchesInMemory))
        val storageFile = File(STORAGE_FILE)
        storageFile.writeText(json)
    }

    fun loadAll() = researchesInMemory

}

fun Application.configureRouting() {
    routing {
        get("/researches") {
            call.respond(ResearchRepository.loadAll())
        }
    }

    routing {
        get("/researches/{id}") {
            call.parameters["id"]?.toLongOrNull()?.let { id ->
                call.respond(ResearchRepository.loadAll().first { it.id == id })
            }

        }
    }

    routing {
        post("/upload") {
            val multipartData = call.receiveMultipart()

            var title = ""
            var description = ""
            val numLabel = (System.currentTimeMillis() / 1000L).toString()
            val appName = "zk_ml_dna_${numLabel}_v0"
            val fullContractName = "${appName}.aleo"
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

                    val projectDir = createContractProjectFromTemplate(
                        "gen/$appName",
                        appName
                    )
                    leoBuild(projectDir)
                    snarkOsDeploy(projectDir, fullContractName, "APrivateKey1zkpDfeQmjkwMeCeZVKgi3wRbey59V7b3q4gmWVw4wEUAwrY")
                } else {
                    println("Project generation error: $exitVal")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val research = Research(
                id = numLabel.toLong(),
                contractName = fullContractName,
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
