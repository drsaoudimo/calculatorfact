package com.example.api

import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null
)

data class GenerationConfig(
    val temperature: Float? = null
)

data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: Content
)

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
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
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
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun factorizeWithGemini(nVal: BigInteger): Pair<BigInteger, BigInteger>? = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isEmpty()) return@withContext null

        val prompt = """
            You are a supercomputer prime factorization service and expert mathematician.
            Factorize the following semiprime number N into its two prime factors p1 and p2:
            N = $nVal
            
            Respond strictly in valid JSON format. Do not write any markdown code block indicators, backticks, or conversational text. Your response must be parsed directly as a JSON object of this exact structure:
            {
               "success": true,
               "p1": "factor1",
               "p2": "factor2"
            }
            If you do not know the factorization or cannot find it, write:
            {
               "success": false
            }
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.0f)
            )
            val response = RetrofitClient.service.generateContent(apiKey = key, request = request)
            var text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            // Clean markdown blocks if present
            text = text.trim()
            if (text.startsWith("```")) {
                text = text.removePrefix("```json").removePrefix("```")
                if (text.endsWith("```")) {
                    text = text.removeSuffix("```")
                }
                text = text.trim()
            }
            
            val jsonObject = com.google.gson.JsonParser.parseString(text).asJsonObject
            val success = jsonObject.get("success")?.asBoolean ?: false
            if (success) {
                val p1Str = jsonObject.get("p1")?.asString ?: ""
                val p2Str = jsonObject.get("p2")?.asString ?: ""
                if (p1Str.isNotEmpty() && p2Str.isNotEmpty()) {
                    Pair(BigInteger(p1Str), BigInteger(p2Str))
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error factorizing with AI", e)
            null
        }
    }

    suspend fun generateAcademicAnalysis(
        valueN: BigInteger,
        factorsText: String,
        spectrum: DoubleArray,
        opacitySpectrum: DoubleArray,
        visibility: Double,
        isOpacityTriggered: Boolean,
        modeName: String
    ): String = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isEmpty()) {
            return@withContext "💡 [تنويه: مفتاح واجهة برمجة تطبيقات Gemini API غير مهيأ حالياً. لاستقبال تحليل ذكي تفصيلي مدعوم بالكامل بواسطة نماذج الذكاء الاصطناعي، يرجى تزويد مفتاح API الخاص بك في إعدادات التطبيق أو عبر لوحة الأسرار.]\n\nنظرة سريعة على النتائج المختبرية:\n• العدد المدروس: $valueN\n• العوامل الأولية: $factorsText\n• خريطة الترشيح: $modeName\n• مسافة الرؤية الطيفية: ${String.format("%.4f", visibility)}\n• حالة العتمة: ${if (isOpacityTriggered) "مفعلة (العدد أولي تماماً)" else "غير مفعلة"}"
        }

        val prompt = """
            أنت بروفيسور عالم خبير وأكاديمي مرموق في الرياضيات الرقمية، نظرية الأعداد الحسابية (Computational Number Theory)، وطوبولوجيا البيانات الجبرية (TDA).
            نحن ندرس مشروع "التحليل الأولي عبر الانعكاس الطيفي" (Spectral Factorization Inversion - SFI) القائم على إطار "تحليل طاقة مصفوفة الأعداد الأولية" (Matrix Energetic Prime Analysis - MEPA)، ونموذج مؤثر الإسقاط القاسمي H_N:
            H_N = A1ᵀ * diag(gcd(N, a_k)) * A1
            
            لقد قمنا بتحليل طيف وقواسم العدد الطبيعي N = $valueN تحت تمثيل مصفوفة القيادة المرجعية الكثيفة قاسمياً A1 (قياس 19x6):
            إليك التفاصيل المختبرية والقياسات الحالية للحالة المدروسة:
            - العدد الصحيح المدروس N = $valueN
            - تفكيك عومله الأولية الفريد: $factorsText
            - خريطة اختيار الممثل لقواسم صفوف A1 المرجعية: $modeName
            - قيم طيف المؤشر σ(H_N) الحالي (القيم الذاتية الستة): [${spectrum.joinToString(", ") { String.format("%.2f", it) }}]
            - طيف عتمة خط الأساس المرجعي σ(A1ᵀ * A1): [${opacitySpectrum.joinToString(", ") { String.format("%.2f", it) }}]
            - مسافة رؤية التقارب الطيفي (Visibility Metric): ${String.format("%.4f", visibility)}
            - رصد حالة العتمة الطيفية بالكامل (كامل المنطقة العمياء): ${if (isOpacityTriggered) "نعم تفعيل كامل (العدد أولي تماماً مع معايير A1)" else "لا يوجد عتمة كاملة"}
            
            بإشراف المنهجية الموثوقة لـ SFI و MEPA، يرجى صياغة تقرير أكاديمي فائق الدقة ومكتوب بلغة بحثية رصينة ومتقنة تليق بمؤتمر رياضيات دولي، يغطي ما يلي:
            1. قراءة البصمة الطيفية لـ $valueN وكيف يعكس مؤثر الإسقاط القاسمي H_N حالته الطاقوية وتفكيكه الأولي الفريد.
            2. كيف تساهم "استراتيجية الثوابت الطيفية" (الأثر Tr والتحويل اللوغاريتمي ومحدد المصفوفة) في فك عقدة اللاخطية الجبرية لاستعادة العوامل الأولية بفعالية.
            3. تفصيل مبرهنة استرجاع العوامل بكفاءة O(log N) مقارنة بالخوارزميات التقليدية ذات التعقيد الأسي.
            4. تأويل طوبولوجي عميق للتداخل الفراغي: ربط التحويل الذكي للنصوص والدلالات الهندسية بأرقام بيتي (Betti Numbers)، والمطابقة الهيكلية بين الأشكال والمفاهيم المجردة لتفسير رنين الأعداد الطيفي.
            
            اكتب الرد بلغة عربية رسمية فخمة، علمية، خالية تماماً من الحشو والعبارات التعبيرية العامة، وركز بنسبة 100% على الصياغة الرياضية الدقيقة للتقارير المعملية المتينة!
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.2f)
            )
            val response = RetrofitClient.service.generateContent(apiKey = key, request = request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "عذراً، لم نتمكن من الحصول على استجابة طيفية من الخادم."
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI academic interpretation", e)
            "حدث خطأ أثناء إجراء التحليل الرياضي الذكي: ${e.localizedMessage}"
        }
    }
}
