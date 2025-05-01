package com.example.sgb.room

import androidx.annotation.DrawableRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// BikeEntity.kt
@Entity(tableName = "bike_table")
data class Bike(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val modelsJson: Map<String, BikeModel>, // Містить моделі та підмоделі
    val selectedSize: String? = null,
    val addedImgBikeUri: String? = null,
    val elapsedHoursValue: Int = 0 // Додаємо поле для годин

)

// BikeModel.kt
data class BikeModel(
    val name: String,
    val submodels: Map<String, BikeSubmodel> // Підмоделі
)

// BikeSubmodel.kt
data class BikeSubmodel(
    val name: String ,
    val years: Map<String, List<String>> , // Роки та доступні розміри
    @DrawableRes val imageRes: Int? = null ,
    val geometry: Map<String, BikeGeometry>? = null // Геометрія для кожного розміру
)
@Entity(tableName = "bike_geometry")
data class BikeGeometry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var bikeId: Int, // Змінено на var для оновлення після створення байка
    val wheelBase: Int,
    val reach: Int,
    val stack: Int,
    val bottomBracketOffset: Int,
    val standOverHeight: Int,
    val headTubeLength: Int,
    val seatTubeAngle: Int,
    val seatTubeLength: Int,
    val topTubeLength: Int,
    val seatHeight: Int,
    val headTubeAngle: Int,
    val chainstayLength: Int,
    val bodyHeight: Int,
    val wheelSize: Int
)


@Entity(tableName = "components_table")
data class Component(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bikeId: Int,           // Ідентифікатор байка, до якого належить компонент
    val compType: String,      // Тип компонента (Fork, Shock, Tyre, і т.д.)
    val compBrand: String,
    val compYear: String,
    val compModel: String,
    val compBrandExtra: String,
    val compAdaptive: String,
    val compSize: String,
    val compWeight: String,
    val compNotes: String,
    val photoUri: String? = null
)


// таблиця для MaketSetup.kt
@Entity(tableName = "setup_table")
data class MarksForSetup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bikeId: Int, // ID байка, до якого прив'язаний сетап
    val setupName: String // Назва сетапу
)
// cетап для байкпарку
@Entity(
    tableName = "bikepark_table",
    indices = [Index(value = ["bikeId"], unique = true)]
)
data class BikeParkSetupData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var bikeId: Int,
    var forkHSR: Int = 0,
    var forkHSRDelta: String = "",
    var forkLSR: Int = 0,
    var forkLSRDelta: String = "",
    var forkHSC: Int = 0,
    var forkHSCDelta: String = "",
    var forkLSC: Int = 0,
    var forkLSCDelta: String = "",
    var shockHSR: Int = 0,
    var shockHSRDelta: String = "",
    var shockLSR: Int = 0,
    var shockLSRDelta: String = "",
    var shockHSC: Int = 0,
    var shockHSCDelta: String = "",
    var shockLSC: Int = 0,
    var shockLSCDelta: String = "",
    var frontTyrePressure: String = "",
    var rearTyrePressure: String = "",
    var forkNotes: String = "",
    var shockNotes: String = "",
    var tyreNotes: String = "",
    var shockSag: String = "",
    var forkSag: String = "",
    var forkPressure: String = "",
    var shockPressure: String = ""
)

@Entity(tableName = "bp_marks_fork_table")
data class BpMarksSuspenshion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bikeId: Int,           // Додано для зв’язку з байком
    var gOut: String = "",
    var numbHands: String = "",
    var squareEdgedHits: String = "",
    var riderShifts: String = "",
    var bottomOutSus: String = "",
    var susSwinging: String = "",
    var stability: String = "",
    var tyresPlussiness: String = "",
    var pulling: String = "",
    var corners: String = "",
    var tiredFeet: String = "",
    var averageMark: Int
)
@Entity(tableName = "service_records", indices = [Index(value = ["date", "bikeId"], unique = true),
Index(value = ["bikeId"])
]
)
data class ServiceRecord(
    @PrimaryKey val date: Long ,
    @ColumnInfo(name = "bikeId") val bikeId: Int ,
    @ColumnInfo(name = "title") var title: String = "" ,
    @ColumnInfo(name = "notes") var notes: String = "" ,
    @ColumnInfo(name = "price") var price: String = ""
)