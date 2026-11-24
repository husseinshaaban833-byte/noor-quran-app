package com.example.audio

import android.content.Context
import com.example.data.local.DownloadedItemEntity
import com.example.data.local.NoorDao
import com.example.quran.QuranRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class ActiveDownloadProgress(
    val id: String,
    val title: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val status: String // "DOWNLOADING", "PAUSED", "ERROR"
)

class DownloadRepository(
    private val context: Context,
    private val dao: NoorDao,
    private val quranRepository: QuranRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeJobs = mutableMapOf<String, Job>()

    private val _activeDownloads = MutableStateFlow<Map<String, ActiveDownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, ActiveDownloadProgress>> = _activeDownloads.asStateFlow()

    val downloadedItems: Flow<List<DownloadedItemEntity>> = dao.observeDownloads()

    suspend fun downloadSurahAudio(reciter: Reciter, surahNumber: Int, surahName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val id = "audio_${reciter.id}_$surahNumber"
            val url = "${reciter.surahBaseUrl}/$surahNumber.mp3"
            val dir = File(context.filesDir, "downloads/audio/${reciter.id}").apply { mkdirs() }
            val tempFile = File(dir, "surah_${surahNumber}.mp3.part")
            val finalFile = File(dir, "surah_${surahNumber}.mp3")
            val title = "سورة $surahName — ${reciter.name}"

            try {
                val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
                val requestBuilder = Request.Builder().url(url)
                if (existingBytes > 0L) {
                    requestBuilder.addHeader("Range", "bytes=$existingBytes-")
                }
                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful && response.code != 206) {
                    return@withContext Result.failure(IllegalStateException("فشل التنزيل: رمز ${response.code}"))
                }

                val isResuming = response.code == 206
                val contentLen = response.body?.contentLength() ?: -1L
                val totalExpected = if (contentLen > 0) (if (isResuming) existingBytes + contentLen else contentLen) else -1L

                _activeDownloads.update {
                    it + (id to ActiveDownloadProgress(id, title, existingBytes, totalExpected, "DOWNLOADING"))
                }

                val input = response.body?.byteStream()
                    ?: return@withContext Result.failure(IllegalStateException("الملف الصوتي فارغ"))

                FileOutputStream(tempFile, isResuming).use { fos ->
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = if (isResuming) existingBytes else 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                        downloaded += read
                        _activeDownloads.update { map ->
                            map + (id to ActiveDownloadProgress(id, title, downloaded, totalExpected, "DOWNLOADING"))
                        }
                    }
                }

                if (finalFile.exists()) finalFile.delete()
                tempFile.renameTo(finalFile)
                val sha256 = computeFileSha256(finalFile)
                dao.insertDownload(
                    DownloadedItemEntity(
                        id = id,
                        type = "AUDIO_SURAH",
                        title = title,
                        reciterOrSourceId = reciter.id,
                        surahOrPage = surahNumber,
                        localPath = finalFile.absolutePath,
                        sizeBytes = finalFile.length(),
                        sha256 = sha256
                    )
                )
                _activeDownloads.update { it - id }
                Result.success(Unit)
            } catch (e: CancellationException) {
                _activeDownloads.update { map ->
                    val prev = map[id]
                    if (prev != null) map + (id to prev.copy(status = "PAUSED")) else map
                }
                throw e
            } catch (e: Exception) {
                _activeDownloads.update { map ->
                    val prev = map[id]
                    if (prev != null) map + (id to prev.copy(status = "ERROR")) else map - id
                }
                Result.failure(e)
            }
        }

    suspend fun downloadExtraTafsir(editionId: String, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val id = "tafsir_$editionId"
            val url = "https://api.alquran.cloud/v1/quran/$editionId"
            _activeDownloads.update {
                it + (id to ActiveDownloadProgress(id, title, 0L, -1L, "DOWNLOADING"))
            }
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    _activeDownloads.update { it - id }
                    return@withContext Result.failure(IllegalStateException("فشل تنزيل التفسير: ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val root = JSONObject(bodyStr).getJSONObject("data").getJSONArray("surahs")
                val compactMap = JSONObject()
                for (i in 0 until root.length()) {
                    val s = root.getJSONObject(i)
                    val sNum = s.getInt("number")
                    val ayahs = s.getJSONArray("ayahs")
                    for (j in 0 until ayahs.length()) {
                        val a = ayahs.getJSONObject(j)
                        val aNum = a.getInt("numberInSurah")
                        compactMap.put("$sNum:$aNum", a.getString("text"))
                    }
                }
                val dir = File(context.filesDir, "downloads/tafsir").apply { mkdirs() }
                val file = File(dir, "$editionId.json")
                file.writeText(compactMap.toString())
                val sha256 = computeFileSha256(file)
                dao.insertDownload(
                    DownloadedItemEntity(
                        id = id,
                        type = "TAFSIR",
                        title = title,
                        reciterOrSourceId = editionId,
                        surahOrPage = 0,
                        localPath = file.absolutePath,
                        sizeBytes = file.length(),
                        sha256 = sha256
                    )
                )
                quranRepository.loadDownloadedTafsirs()
                _activeDownloads.update { it - id }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _activeDownloads.update { it - id }
                Result.failure(e)
            }
        }

    fun registerJob(id: String, job: Job) {
        activeJobs[id] = job
    }

    fun pauseDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
    }

    suspend fun deleteDownload(item: DownloadedItemEntity) = withContext(Dispatchers.IO) {
        val file = File(item.localPath)
        if (file.exists()) file.delete()
        val partFile = File(item.localPath + ".part")
        if (partFile.exists()) partFile.delete()
        dao.deleteDownloadById(item.id)
        quranRepository.loadDownloadedTafsirs()
    }

    fun getAvailableInternalStorageBytes(): Long = context.filesDir.usableSpace

    private fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var len: Int
            while (input.read(buf).also { len = it } != -1) {
                digest.update(buf, 0, len)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
