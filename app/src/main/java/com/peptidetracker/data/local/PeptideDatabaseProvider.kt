package com.peptidetracker.data.local

import android.content.Context
import androidx.room.Room

object PeptideDatabaseProvider {
    @Volatile
    private var instance: PeptideDatabase? = null

    fun get(context: Context): PeptideDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            PeptideDatabase::class.java,
            "peptide-tracker.db"
        ).build().also { instance = it }
    }
}
