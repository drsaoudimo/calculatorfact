package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(val contents: List<Content>)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generateAcademicAnalysis(
        valueN: java.math.BigInteger,
        factorsText: String,
        spectrum: DoubleArray,
        opacitySpectrum: DoubleArray,
        visibility: Double,
        isOpacityTriggered: Boolean,
        modeName: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "عذراً، لم يتم ضبط مفتاح Gemini API في لوحة الأسرار (Secrets panel). يرجى تكوينه في الإعدادات لتفعيل التحليل الطيفي الطوبولوجي التلقائي."
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

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "عذراً، فشل استخراج نص التقرير العلمي من استجابة الذكاء الاصطناعي."
        } catch (e: Exception) {
            "فشل الاتصال بخدمة تحليل الذكاء الاصطناعي: ${e.localizedMessage}"
        }
    }
}
