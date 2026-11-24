package com.example.quran

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream

object AyahCardRenderer {
    fun shareAyahAsImage(
        context: Context,
        ayah: Ayah,
        tafsirSnippet: String?,
        useEmeraldStyle: Boolean
    ) {
        val bitmap = renderCardBitmap(context, ayah, tafsirSnippet, useEmeraldStyle)
        val cardsDir = File(context.cacheDir, "cards").apply { mkdirs() }
        val file = File(cardsDir, "noor_ayah_${ayah.surahNumber}_${ayah.ayahNumber}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "﴿${ayah.textUthmani}﴾\n[${ayah.surahNameAr} - الآية ${ayah.ayahNumber}] — تطبيق نور القرآن"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الآية كصورة"))
    }

    private fun renderCardBitmap(
        context: Context,
        ayah: Ayah,
        tafsirSnippet: String?,
        useEmeraldStyle: Boolean
    ): Bitmap {
        val width = 1200
        val height = 900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgRes = if (useEmeraldStyle) R.drawable.img_ayah_card_emerald else R.drawable.img_ayah_card_parchment
        val bgBitmap = BitmapFactory.decodeResource(context.resources, bgRes)
        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap, null, Rect(0, 0, width, height), null)
        } else {
            canvas.drawColor(if (useEmeraldStyle) Color.parseColor("#0A3D2E") else Color.parseColor("#FAF6EE"))
        }

        val primaryTextColor = if (useEmeraldStyle) Color.parseColor("#FAF6EE") else Color.parseColor("#17221E")
        val accentColor = if (useEmeraldStyle) Color.parseColor("#E6CA82") else Color.parseColor("#0A3D2E")
        val quranTypeface = ResourcesCompat.getFont(context, R.font.amiri_quran)
        val uiTypeface = ResourcesCompat.getFont(context, R.font.cairo)

        val ayahPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            textSize = 46f
            typeface = quranTypeface
        }
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 30f
            typeface = uiTypeface
            textAlign = Paint.Align.CENTER
        }
        val tafsirPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryTextColor
            alpha = 215
            textSize = 24f
            typeface = uiTypeface
        }

        canvas.drawText(
            "سُورَةُ ${ayah.surahNameAr} • الآية ${ArabicNormalizer.toArabicIndicDigits(ayah.ayahNumber)}",
            width / 2f,
            155f,
            headerPaint
        )

        val contentWidth = width - 260
        val verseText = "﴿ ${ayah.textUthmani} ﴾"
        val verseLayout = StaticLayout.Builder.obtain(verseText, 0, verseText.length, ayahPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(12f, 1.35f)
            .build()

        canvas.save()
        canvas.translate(130f, 220f)
        verseLayout.draw(canvas)
        canvas.restore()

        if (!tafsirSnippet.isNullOrBlank()) {
            val truncated = if (tafsirSnippet.length > 180) tafsirSnippet.take(177) + "..." else tafsirSnippet
            val tafsirLayout = StaticLayout.Builder.obtain(
                "التفسير الميسر: $truncated",
                0,
                "التفسير الميسر: $truncated".length,
                tafsirPaint,
                contentWidth
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            val yPos = (250f + verseLayout.height + 24f).coerceAtMost(670f)
            canvas.save()
            canvas.translate(130f, yPos)
            tafsirLayout.draw(canvas)
            canvas.restore()
        }

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 24f
            typeface = uiTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("نور القرآن", width / 2f, height - 110f, footerPaint)
        return bitmap
    }
}
