package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.math.BigInteger
import java.util.concurrent.TimeUnit

// --- Direct REST API Data Models for Gemini in Plain Kotlin (Gson) ---

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null
)

data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseMimeType: String? = null
)

data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: Content? = null
)

// --- Retrofit Service Interfaces ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiClient {
    private const val TAG = "GeminiClient"

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "PLACEHOLDER_KEY" || key.isEmpty()) "" else key
    }

    /**
     * Attempts to factorize a composite number N into two factors using Gemini's mathematical indexing.
     */
    suspend fun factorizeWithGemini(nVal: BigInteger): Pair<BigInteger, BigInteger>? = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isEmpty()) return@withContext null

        val prompt = """
            You are an advanced computational math solver.
            Your job is to factorize the composite number N into two non-trivial factors p1 and p2 such that p1 * p2 = N.
            N = $nVal
            
            Respond strictly in valid JSON format. Do not use block-backticks or conversational text. Return exactly this JSON structure:
            {
               "success": true,
               "p1": "value_of_p1",
               "p2": "value_of_p2"
            }
            If you cannot find the factors, return {"success": false}. Ensure p1 * p2 = N exactly.
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.0f, responseMimeType = "application/json")
            )
            val response = RetrofitClient.service.generateContent(apiKey = key, request = request)
            var text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            text = text.trim()
            if (text.startsWith("```")) {
                text = text.removePrefix("```json").removePrefix("```")
                if (text.endsWith("```")) {
                    text = text.removeSuffix("```")
                }
                text = text.trim()
            }
            
            val gson = Gson()
            val parsed = gson.fromJson(text, GeminiFactorizationResponse::class.java)
            if (parsed.success) {
                val p1 = BigInteger(parsed.p1.trim())
                val p2 = BigInteger(parsed.p2.trim())
                if (p1.multiply(p2) == nVal) {
                    Pair(p1, p2)
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error factorizing with AI", e)
            null
        }
    }

    /**
     * Generates a comprehensive research article / educational briefing on SFI & MEPA
     */
    suspend fun generateAcademicAnalysis(
        valueN: BigInteger,
        factorsText: String,
        determinant: Double,
        frobeniusNorm: Double,
        b0: Int,
        b1: Int,
        b2: Int
    ): String = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isEmpty()) {
            return@withContext "خطأ: لم يتم تهيئة مفتاح API الخاص بـ Gemini. يرجى توجيهه عبر حقول لوحة الأسرار AI Studio للتثبيت الآمن."
        }

        val prompt = """
            Write a detailed, formal scientific analysis in Arabic of the composite numerical system represented by N:
            
            - Number N: $valueN
            - Prime/Decomposition Factors: $factorsText
            - Matrix Invariants (MEPA Model): Frobenius Norm = $frobeniusNorm, Trace Matrix Sum = $determinant
            - Topological Betti Homology: B0 = $b0, B1 = $b1, B2 = $b2
            
            Incorporate elements of Computational Topology, Semantic Text Analysis, and Betti group maps (B0, B1, B2) as discussed in the Topological Data Analysis (TDA) theoretical framework. Explain how numerical structures correspond to stable semantic manifolds in modern Natural Language Processing (NLP). 
            Write in a rich, academic, and highly professional Arabian scholarly style (اللغة العربية الأكاديمية الرصينة). Do not use conversational filler, write it as a formatted research memo with sections:
            1. التحليل البنيوي الدلالي (Structural Semantic Analysis)
            2. الفضاءات المترية والتحول الذكي للنصوص (Metric Spaces & Smart NLP Homology)
            3. التماثل الهومولوجي للمصفوفات الطيفية (Homological Isomorphism of Spectral Matrices)
            4. التوجيهات الرياضية المستقبلية (Academic Micro-Directives)
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.2f)
            )
            val response = RetrofitClient.service.generateContent(apiKey = key, request = request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "عذراً، لم تنجح الاستجابة الرياضية من المخدم حالياً."
        } catch (e: Exception) {
            "فشل استقصاء التحليل العلمي: ${e.message}"
        }
    }
}

data class GeminiFactorizationResponse(
    val success: Boolean,
    val p1: String = "",
    val p2: String = ""
)
