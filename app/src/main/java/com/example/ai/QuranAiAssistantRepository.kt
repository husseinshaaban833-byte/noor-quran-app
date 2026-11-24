package com.example.ai

import com.example.quran.QuranRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

data class RagCitation(
    val title: String,
    val source: String,
    val surahNumber: Int?,
    val ayahNumber: Int?
)

data class AiStreamChunk(
    val accumulatedText: String,
    val citations: List<RagCitation>
)

class QuranAiAssistantRepository(private val quranRepository: QuranRepository) {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    أنت المساعد القرآني الذكي في تطبيق «نور القرآن». يجب الالتزام الصارم بجميع الضوابط الشرعية والعلمية التالية دون استثناء:
                    1. لا تُنشئ أو تؤلف آيات قرآنية من عندك إطلاقًا، ولا تعدل نص القرآن الكريم أو تشكيله؛ استخدم فقط النصوص القرآنية الموثقة المرفقة في سياق البحث (RAG).
                    2. لا تنسب أي حديث إلى النبي ﷺ إلا إذا كان ثابتًا في الصحيحين أو السنن الصحيحة مع ذكر اسم الكتاب ورقم الحديث أو الباب بدقة، وإذا لم تتأكد قل بوضوح: لا أملك مصدرًا موثقًا.
                    3. لا تخترع أسباب نزول؛ اعرض فقط ما ورد في سياق أسباب النزول الموثقة أو في كتب التفسير المعتمدة كابن كثير والطبري والواحدي والسيوطي.
                    4. لا تُصدر فتاوى شرعية مستقلة؛ وجّه السائل إلى أهل العلم المعتبرين عند طلب الأحكام الفقهية الخاصة.
                    5. ميّز بوضوح تام بين النص القرآني، وتفسير العلماء المعتمد، والفائدة اللغوية أو التدبرية.
                    6. اذكر اسم المصدر والمؤلف والموضع عند كل نقل علمي.
                    7. إذا لم تجد معلومات موثقة كافية في المراجع المرفقة، صرّح بذلك بوضوح وأمانة.
                    أجب باللغة العربية الفصحى الواضحة وبأسلوب هادئ منظم.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun askWithRag(userQuery: String): Flow<AiStreamChunk> = flow {
        quranRepository.ensureLoaded()
        val matchedAyahs = quranRepository.searchQuran(userQuery, searchTafsir = true, limit = 5)
        val citations = mutableListOf<RagCitation>()
        val contextBuilder = StringBuilder()

        if (matchedAyahs.isNotEmpty()) {
            contextBuilder.appendLine("=== مراجع مسترجعة من قاعدة بيانات نور القرآن (RAG) ===")
            matchedAyahs.forEach { ayah ->
                val tafsir = quranRepository.getTafsirMuyassar(ayah.surahNumber, ayah.ayahNumber)
                val asbab = quranRepository.getAsbabForAyah(ayah.surahNumber, ayah.ayahNumber)
                val words = quranRepository.getWordMeanings(ayah.surahNumber, ayah.ayahNumber)

                contextBuilder.appendLine("• سورة ${ayah.surahNameAr} (${ayah.surahNumber}:${ayah.ayahNumber}) [صفحة ${ayah.pageNumber}]:")
                contextBuilder.appendLine("  النص العثماني: ﴿${ayah.textUthmani}﴾")
                if (tafsir.isNotBlank()) {
                    contextBuilder.appendLine("  التفسير الميسر (مجمع الملك فهد لطباعة المصحف الشريف): $tafsir")
                }
                if (words.isNotEmpty()) {
                    contextBuilder.appendLine("  معاني الكلمات: " + words.joinToString("؛ ") { "${it.word}: ${it.meaning}" })
                }
                if (asbab != null) {
                    contextBuilder.appendLine("  سبب النزول (${asbab.source} - ${asbab.authenticity}): ${asbab.reason}")
                }
                citations.add(
                    RagCitation(
                        title = "${ayah.surahNameAr}: ${ayah.ayahNumber}",
                        source = "المصحف الشريف والتفسير الميسر",
                        surahNumber = ayah.surahNumber,
                        ayahNumber = ayah.ayahNumber
                    )
                )
            }
        } else {
            contextBuilder.appendLine("لم تُطابق كلمات البحث آيات محددة في الفهرس المحلي. التزم بالضوابط الشرعية واذكر عدم توفر مرجع محلي مباشر إذا استدعى الأمر.")
        }

        val fullPrompt = """
            $contextBuilder
            
            سؤال المستخدم: $userQuery
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            model.generateContentStream(fullPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(AiStreamChunk(accumulated.toString(), citations))
                }
            }
        }
        if (accumulated.isEmpty()) {
            throw IllegalStateException("لم يُرجع المساعد استجابة. يرجى المحاولة مرة أخرى.")
        }
    }
}
