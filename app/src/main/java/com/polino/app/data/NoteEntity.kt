package com.polino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * یادداشت مالی: عنوان و متن آزاد با زمان ساخت و آخرین ویرایش.
 * برخلاف برنامه، موعد و نشان انجام‌شدن ندارد؛ برخلاف هدف، مبلغ ندارد.
 */
@Entity(tableName = "notes")
internal data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String = "",
    val createdMillis: Long,
    val updatedMillis: Long
)
