package dna.zkml

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


fun snarkOsDeploy(projectDir: File, fullContractName: String, prv: String) {
    /**
     * snarkos developer deploy --private-key prv --priority-fee 0
     * --query https://api.explorer.aleo.org/v1
     * --broadcast https://api.explorer.aleo.org/v1/testnet3/transaction/broadcast --path ./build  zk_ml_dna_v0.aleo
     */

    val deployCli = listOf(
        "snarkos", "developer", "deploy",
        "--private-key", prv,
        "--priority-fee", "0",
        "--query", "https://api.explorer.aleo.org/v1",
        "--broadcast", "https://api.explorer.aleo.org/v1/testnet3/transaction/broadcast",
        "--path", "$projectDir/build",
        fullContractName


    )
    val processBuilder = ProcessBuilder()
        .command(deployCli)
    try {
        val process: Process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val line = reader.readText();
        println(line)

        val exitVal = process.waitFor()
        if (exitVal == 0) {
            println("Deploy success")
        } else {
            println("Deploy error: $exitVal")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}