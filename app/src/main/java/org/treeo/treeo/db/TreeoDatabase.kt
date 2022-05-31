package org.treeo.treeo.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.treeo.treeo.db.converters.JSONConverter
import org.treeo.treeo.db.converters.MapConverter
import org.treeo.treeo.db.converters.UUIDConverter
import org.treeo.treeo.db.converters.UserLocationConverter
import org.treeo.treeo.db.dao.ActivityDao
import org.treeo.treeo.db.dao.LandSurveyDao
import org.treeo.treeo.db.dao.TreeMeasurementDao
import org.treeo.treeo.db.models.*

@Database(
    entities = [
        ActivityEntity::class,
        OptionEntity::class,
        PageEntity::class,
        UserInputEntity::class,
        QuestionnaireEntity::class,
        LandSurveyEntity::class,
        PhotoEntity::class,
        UploadQueueEntity::class,
        TMEntity::class,
        ForestInventoryEntity::class,
        TreeSpecieEntity::class
    ],
    version = 16,
    exportSchema = true,
//    autoMigrations = [
//        AutoMigration(from = 6, to = 7),
//        AutoMigration(from = 7, to = 8),
//        AutoMigration(from = 8, to = 9),
//        AutoMigration(from = 9, to = 10),
//        AutoMigration(from = 10, to = 11),
//    ]
)
@TypeConverters(
    UUIDConverter::class,
    JSONConverter::class,
    UserLocationConverter::class,
    MapConverter::class
)
abstract class TreeoDatabase : RoomDatabase() {

    abstract fun getActivityDao(): ActivityDao

    abstract fun getLandSurveyDao(): LandSurveyDao

    abstract fun getTreeMeasurementDao(): TreeMeasurementDao
}
