@file:OptIn(ExperimentalPathApi::class)

package dna.zkml

import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import kotlin.io.path.writeLines


private const val PROGRAM_NAME_PLACEHOLDER = "program zk_ml_dna_v0.aleo {"
private const val ML_FUNCTION_PLACEHOLDER = "// *** ML_FUNCTION_PLACEHOLDER ***"
private const val ML_INPUT_PLACEHOLDER = "// *** ML_INPUT_PLACEHOLDER ***"


fun createContractProjectFromTemplate(zkMLOutputProject: String, contractName: String): File {
    val mainLeo = File(zkMLOutputProject + "/src", "main.leo")

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

    val templateDir = File("leo/template")
    val projectDir = File("leo/${contractName}").apply { mkdirs() }
    templateDir.toPath().copyToRecursively(projectDir.toPath(), overwrite = true, followLinks = false)

    val programJson = File(projectDir, "program.json")
    programJson.writeText("{\n" +
            "    \"program\": \"${contractName}.aleo\",\n" +
            "    \"version\": \"0.0.0\",\n" +
            "    \"description\": \"\",\n" +
            "    \"license\": \"MIT\"\n" +
            "}")
    val contractFile = File(projectDir, "src/main.leo")

    val templateLines = contractFile.readLines()
    val contractLines = mutableListOf<String>()
    templateLines.forEach { templateLine ->
        if (templateLine.contains(PROGRAM_NAME_PLACEHOLDER)) {
            contractLines.add("program ${contractName}.aleo {")
        } else if (templateLine.contains(ML_INPUT_PLACEHOLDER)) {
            contractLines.add(mlInputs)
        } else if (templateLine.contains(ML_FUNCTION_PLACEHOLDER)) {
            functionLines.forEach { functionLine ->
                contractLines.add(functionLine)
            }
        } else {
            contractLines.add(templateLine)
        }
    }
    contractFile.toPath().writeLines(contractLines)

    return projectDir
}