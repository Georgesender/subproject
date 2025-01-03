package com.example.sgb.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BikeDao {
    @Insert
    suspend fun insertBike(bike: Bike): Long

    @Query("SELECT * FROM bike_table WHERE id = :id")
    suspend fun getBikeById(id: Int): Bike?


    @Query("UPDATE bike_table SET selectedSize = :size WHERE id = :bikeId")
    suspend fun updateBikeSize(bikeId: Int, size: String)

    @Query("DELETE FROM bike_table WHERE id = :bikeId")
    suspend fun deleteBikeById(bikeId: Int)
}

@Dao
interface GeometryDao {
    @Insert
    suspend fun insertGeometry(geometry: BikeGeometry): Long

    @Query("SELECT * FROM bike_geometry WHERE bikeId = :bikeId")
    suspend fun getGeometryByBikeId(bikeId: Int): BikeGeometry?

    @Query("DELETE FROM bike_geometry WHERE bikeId = :bikeId")
    suspend fun deleteGeometryByBikeId(bikeId: Int)
}

@Dao
interface ComponentsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponent(component: Component)

    @Query("SELECT * FROM components_table WHERE bikeId = :bikeId LIMIT 1")
    suspend fun getComponentsByBikeId(bikeId: Int): Component?

    @Update
    suspend fun updateComponent(component: Component)
    // Додати метод для видалення компонентів
    @Query("DELETE FROM components_table WHERE bikeId = :bikeId")
    suspend fun deleteComponentsByBikeId(bikeId: Int)
}

@Dao
interface SetupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetup(setup: MarksForSetup)

    @Query("SELECT * FROM setup_table WHERE bikeId = :bikeId")
    suspend fun getSetupsByBikeId(bikeId: Int): List<MarksForSetup>

    @Query("SELECT * FROM setup_table WHERE id = :setupId")
    suspend fun getSetupById(setupId: Int): MarksForSetup?

    @Delete
    suspend fun deleteSetup(setup: MarksForSetup)
}





