package com.example.sgb.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Bike::class, BikeGeometry::class], version = 1)
@TypeConverters(Converters::class)
abstract class BikeDatabase : RoomDatabase() {
    abstract fun bikeDao(): BikeDao
    abstract fun geometryDao(): GeometryDao

    companion object {
        @Volatile
        private var INSTANCE: BikeDatabase? = null

        fun getDatabase(context: Context): BikeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BikeDatabase::class.java,
                    "bike_database"
                ).fallbackToDestructiveMigration() // Щоб уникнути проблем при зміні схеми
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}