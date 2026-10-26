package com.peptidetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PeptideStockEntity::class, ActiveVialEntity::class, DoseLogEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(EpochConverters::class)
abstract class PeptideDatabase : RoomDatabase() {
    abstract fun peptideStockDao(): PeptideStockDao
    abstract fun activeVialDao(): ActiveVialDao
    abstract fun doseLogDao(): DoseLogDao
}
