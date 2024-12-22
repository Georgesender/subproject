package com.example.sgb.room

import androidx.room.TypeConverter
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.lang.reflect.Type

class Converters {

    @TypeConverter
    fun fromBikeModelMap(value: Map<String, BikeModel>?): String? {
        val gson = Gson()
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBikeModelMap(value: String?): Map<String, BikeModel>? {
        val gson = Gson()
        val type: Type = object : TypeToken<Map<String, BikeModel>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromBikeSubmodelMap(value: Map<String, BikeSubmodel>?): String? {
        val gson = Gson()
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBikeSubmodelMap(value: String?): Map<String, BikeSubmodel>? {
        val gson = Gson()
        val type: Type = object : TypeToken<Map<String, BikeSubmodel>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromStringListMap(value: Map<String, List<String>>?): String? {
        val gson = Gson()
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringListMap(value: String?): Map<String, List<String>>? {
        val gson = Gson()
        val type: Type = object : TypeToken<Map<String, List<String>>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromBikeGeometryMap(value: Map<String, BikeGeometry>?): String? {
        val gson = Gson()
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBikeGeometryMap(value: String?): Map<String, BikeGeometry>? {
        val gson = Gson()
        val type: Type = object : TypeToken<Map<String, BikeGeometry>>() {}.type
        return gson.fromJson(value, type)
    }
}
