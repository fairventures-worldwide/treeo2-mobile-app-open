package org.treeo.treeo.db.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.treeo.treeo.models.UserLocation
import org.treeo.treeo.network.models.ActivityUploadDTO
import java.util.*

class UUIDConverter {
    @TypeConverter
    fun fromUUID(uuid: UUID): String {
        return uuid.toString()
    }

    @TypeConverter
    fun toUUID(uuid: String): UUID {
        return UUID.fromString(uuid)
    }
}

class ListConverter {
    private val gson = Gson()

    @TypeConverter
    fun stringToList(data: String?): List<String> {
        if (data == null) {
            return Collections.emptyList()
        }

        val listType = object : TypeToken<List<String>>() {

        }.type

        return try {
            gson.fromJson(data, listType)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    @TypeConverter
    fun listToString(someObjects: List<String>?): String? {
        return someObjects?.let { gson.toJson(someObjects) }
    }
}

class JSONConverter {
    @TypeConverter
    fun toJson(obj: ActivityUploadDTO): String {
        return Gson().toJson(obj)
    }

    @TypeConverter
    fun fromJson(string: String): ActivityUploadDTO {
        return Gson().fromJson(string, ActivityUploadDTO::class.java)
    }
}

class UserLocationConverter {
    @TypeConverter
    fun fromUserLocation(obj: UserLocation?): String {
        return Gson().toJson(listOf(obj?.lat, obj?.lng))
    }

    @TypeConverter
    fun toUserLocation(string: String?): UserLocation? {
        if (string != null) {
            val list = Gson().fromJson(string, List::class.java)
            return UserLocation(list[0]?.toString(), list[1]?.toString())
        }
        return null
    }
}

class MapConverter {
    @TypeConverter
    fun fromMap(map: Map<String, Any>): String {
        return Gson().toJson(map)
    }

    @TypeConverter
    fun toMap(string: String): Map<String, Any> {
        val map = Gson().fromJson(string, Map::class.java)
        return map as Map<String, Any>
    }
}
