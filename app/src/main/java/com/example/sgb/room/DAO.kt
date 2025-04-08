package com.example.sgb.room

import androidx.lifecycle.LiveData
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
    suspend fun insertComponent(component: Component): Long

    @Query("SELECT * FROM components_table WHERE bikeId = :bikeId")
    suspend fun getComponentsByBikeId(bikeId: Int): List<Component>

    @Update
    suspend fun updateComponent(component: Component)

    @Delete
    suspend fun deleteComponent(component: Component)

    @Query("DELETE FROM components_table WHERE id = :componentId")
    suspend fun deleteComponentById(componentId: Int)
}



@Dao
interface SetupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetup(setup: MarksForSetup)

    @Query("SELECT * FROM setup_table WHERE id = :setupId")
    suspend fun getSetupById(setupId: Int): MarksForSetup?

    @Delete
    suspend fun deleteSetup(setup: MarksForSetup)
}
//BP -скорочено від Bike Park
@Dao
interface BPSetupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBikeParkSetup(setup: BikeParkSetupData): Long

    @Query("SELECT * FROM bikepark_table WHERE bikeId = :bikeId")
    suspend fun getBikeParkSetupById(bikeId: Int): BikeParkSetupData?

    @Query("SELECT * FROM bikepark_table WHERE bikeId = :bikeId")
    fun getBikeParkSetupLive(bikeId: Int): LiveData<BikeParkSetupData>

    @Update
    suspend fun updateBikeParkSetup(setup: BikeParkSetupData)

    @Query("DELETE FROM bikepark_table WHERE id = :id")
    suspend fun deleteBikeParkSetupById(id: Int)
}
// Sus- скорочено від suspension
@Dao
interface BPMarksSuspensionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBpMarksSus(bpMarksFork: BpMarksSuspenshion): Long

    @Query("SELECT * FROM bp_marks_fork_table WHERE bikeId = :bikeId LIMIT 1")
    suspend fun getBpMarksSusByBikeId(bikeId: Int): BpMarksSuspenshion?

    @Update
    suspend fun updateBpMarksSus(bpMarksFork: BpMarksSuspenshion)

    @Query("DELETE FROM bp_marks_fork_table WHERE bikeId = :bikeId")
    suspend fun deleteBpMarksSusByBikeId(bikeId: Int)
}







