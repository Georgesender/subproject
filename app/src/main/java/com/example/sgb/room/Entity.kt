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
    var componentBrand1: String = "",
    var componentBrand2: String = "",
    var componentBrand3: String = "",
    var componentBrand4: String = "",
    var series1: String = "",
    var series2: String = "",
    var series3: String = "",
    var series4: String = "",
    var sSizeWidth: String = "",
    var sSizeGoes: String = "",
    var fSize2: String = "",
    var size3: String = "",
    var size4: String = ""
)

