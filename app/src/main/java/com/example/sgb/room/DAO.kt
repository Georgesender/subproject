package com.example.sgb.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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

    // Видалити всі велосипеди (якщо потрібно)
    @Query("DELETE FROM bike_table")
    suspend fun deleteAllBikes()

    @Query("UPDATE bike_table SET elapsedHoursValue = :newValue WHERE id = :bikeId")
    suspend fun updateElapsedHours(bikeId: Int, newValue: Int)

    @Query("SELECT * FROM bike_table WHERE id = :id")
    fun getBikeByIdFlow(id: Int): Flow<Bike?>
}

@Dao
interface GeometryDao {
    @Insert
    suspend fun insertGeometry(geometry: BikeGeometry): Long

    @Query("SELECT * FROM bike_geometry WHERE bikeId = :bikeId")
    suspend fun getGeometryByBikeId(bikeId: Int): BikeGeometry?

    @Query("DELETE FROM bike_geometry WHERE bikeId = :bikeId")
    suspend fun deleteGeometryByBikeId(bikeId: Int)

    // Видалити всі геометрії
    @Query("DELETE FROM bike_geometry")
    suspend fun deleteAllGeometry()
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

    @Query("DELETE FROM components_table WHERE bikeId = :bikeId")
    suspend fun deleteComponentsByBikeId(bikeId: Int)

    @Query("SELECT photoUri FROM components_table WHERE bikeId = :bikeId")
    fun getPhotoUrisByBikeId(bikeId: Int): List<String?>

    // Видалити всі компоненти
    @Query("DELETE FROM components_table")
    suspend fun deleteAllComponents()
}

@Dao
interface SetupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetup(setup: MarksForSetup)

    @Query("SELECT * FROM setup_table WHERE id = :setupId")
    suspend fun getSetupById(setupId: Int): MarksForSetup?

    @Delete
    suspend fun deleteSetup(setup: MarksForSetup)

    @Query("DELETE FROM setup_table WHERE bikeId = :bikeId")
    suspend fun deleteSetupsByBikeId(bikeId: Int)

    @Query("DELETE FROM setup_table")
    suspend fun deleteAllSetups()
}

// BP - скорочено від Bike Park
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

    @Query("DELETE FROM bikepark_table WHERE bikeId = :bikeId")
    suspend fun deleteBikeParkSetupByBikeId(bikeId: Int)

    @Query("DELETE FROM bikepark_table")
    suspend fun deleteAllBikeParkSetups()
}

// Sus - скорочено від suspension
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

    @Query("DELETE FROM bp_marks_fork_table")
    suspend fun deleteAllBpMarksSus()
}

@Dao
interface ServiceRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ServiceRecord)

    @Update
    suspend fun update(record: ServiceRecord)

    @Query("SELECT * FROM service_records WHERE date = :date AND bikeId = :bikeId")
    suspend fun getRecordByDate(date: Long, bikeId: Int): ServiceRecord?

    @Query("SELECT * FROM service_records WHERE bikeId = :bikeId ORDER BY date DESC")
    fun getRecordsForBike(bikeId: Int): Flow<List<ServiceRecord>>

    @Query("DELETE FROM service_records WHERE bikeId = :bikeId")
    suspend fun deleteRecordsByBikeId(bikeId: Int)

    @Query("DELETE FROM service_records")
    suspend fun deleteAllServiceRecords()
}