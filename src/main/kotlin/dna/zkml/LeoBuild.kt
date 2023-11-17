package dna.zkml

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


fun leoBuild(projectDir: File) {
    /**
     * leo build --path ${projectDir}
     */
    val buildCli = listOf(
        "leo", "build",
        "--path", projectDir.absolutePath
    )

    val processBuilder = ProcessBuilder()
        .command(buildCli)
    try {
        val process: Process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val line = reader.readText();
        println(line)

        val exitVal = process.waitFor()
        if (exitVal == 0) {
            println("Leo build success")
        } else {
            println("Leo build error: $exitVal")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}