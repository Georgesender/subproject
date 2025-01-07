package com.example.sgb.room

import androidx.room.Entity
import androidx.room.PrimaryKey

// BikeEntity.kt
@Entity(tableName = "bike_table")
data class Bike(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val modelsJson: Map<String, BikeModel>, // Містить моделі та підмоделі
    val selectedSize: String? = null // Обраний розмір байка
)

// BikeModel.kt
data class BikeModel(
    val name: String,
    val submodels: Map<String, BikeSubmodel> // Підмоделі
)

// BikeSubmodel.kt
data class BikeSubmodel(
    val name: String,
    val years: Map<String, List<String>>, // Роки та доступні розміри
    val imageName: String? = null, // Зображення байка
    val geometry: Map<String, BikeGeometry>? = null // Геометрія для кожного розміру
)

// BikeGeometryEntity.kt
@Entity(tableName = "bike_geometry")
data class BikeGeometry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bikeId: Int, // Ідентифікатор байка
    val wheelBase: Int,
    val reach: Int,
    val stack: Int,
    val bottomBracketOffset: Int,
    val standOverHeight: Int,
    val headTubeLength: Int,
    val seatTubeLength: Int,
    val topTubeLength: Int,
    val seatHeight: Int,
    val seatTubeAngle: Int,
    val headTubeAngle: Int,
    val chainstayLength: Int,
    val bodyHeight: Int,
    val wheelSize: Int
)


@Entity(tableName = "components_table")
data class Component(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var bikeId: Int, // Ідентифікатор байка
    var shockBrand: String = "",
    var forkBrand: String = "",
    var frontTyreBrand: String = "",
    var rearTyreBrand: String = "",
    var shockSeries: String = "",
    var forkSeries: String = "",
    var frontTyreSeries: String = "",
    var rearTyreSeries: String = "",
    var sSizeWidth: String = "",
    var sSizeGoes: String = "",
    var fSize2: String = "",
    var frontTyreSize: String = "",
    var rearTyreSize: String = ""
)

// таблиця для MaketSetup.kt
@Entity(tableName = "setup_table")
data class MarksForSetup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bikeId: Int, // ID байка, до якого прив'язаний сетап
    val setupName: String // Назва сетапу
)
// cетап для байкпарку
@Entity(tableName = "bikepark_table")
data class BikeParkSetupData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var bikeId: Int,
    var forkHSR: String = "",
    var forkLSR: String = "",
    var forkHSC: String = "",
    var forkLSC: String = "",
    var shockHSR: String = "",
    var shockLSR: String = "",
    var shockHSC: String = "",
    var shockLSC: String = "",
    var frontTyrePressure: String = "",
    var rearTyrePressure: String = "",
    var forkNotes: String = "",
    var shockNotes: String = "",
    var tyreNotes: String = ""
)


