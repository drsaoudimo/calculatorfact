package com.example

import org.junit.Test
import java.io.File
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection

class FactorizationTest {

    @Test
    fun testAskGemini() {
        val outFile = File("factors.txt")
        outFile.writeText("Querying Gemini for factorization...\n")

        // Retrieve GEMINI_API_KEY using reflection on BuildConfig
        var apiKey = ""
        try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            apiKey = field.get(null) as? String ?: ""
        } catch (e: Exception) {
            outFile.appendText("Could not load GEMINI_API_KEY: ${e.message}\n")
        }

        if (apiKey.isEmpty()) {
            outFile.appendText("GEMINI_API_KEY is empty.\n")
            return
        }

        val nStr = "909249334023664880156636559210554814588660304327071547163563"
        // Formulate prompt
        val prompt = "Factorize the following 60-digit semiprime number into its two prime factors: $nStr. Please return ONLY the factors in format: p1 = [factor1], p2 = [factor2], nothing else."

        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val jsonBody = """
                {
                    "contents": [{
                        "parts": [{
                            "text": "$prompt"
                        }]
                    }]
                }
            """.trimIndent()

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody)
            writer.flush()
            writer.close()

            connection.connectTimeout = 20000
            connection.readTimeout = 20000

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }
            outFile.appendText("GEMINI RESPONSE:\n$response\n")
        } catch (e: Exception) {
            outFile.appendText("Error calling Gemini API: ${e.message}\n")
        }
    }
}
