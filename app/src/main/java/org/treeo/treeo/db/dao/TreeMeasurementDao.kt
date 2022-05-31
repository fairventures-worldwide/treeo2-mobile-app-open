package org.treeo.treeo.db.dao

import androidx.room.*
import org.treeo.treeo.db.models.*
import org.treeo.treeo.db.models.relations.TMWithPhotos

@Dao
interface TreeMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeMeasurement(tree: TMEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreePhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createAdHocTreeMeasurementActivity(activityEntity: ActivityEntity): Long

    @Transaction
    @Query("SELECT * FROM TMEntity WHERE activityId=:activityId")
    suspend fun getTreeMeasurementWithPhotosByActivity(activityId: Long): List<TMWithPhotos?>

    @Transaction
    @Query("SELECT COUNT(measurementUUID) FROM TMEntity WHERE activityId=:activityId")
    suspend fun getTreeMeasurementActivityCount(activityId: Long): Int

    @Query("SELECT * FROM TMEntity WHERE forestInventoryId=:inventoryId")
    suspend fun getTreeMeasurements(inventoryId: Long): List<TMEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForestInventory(forestInventoryEntity: ForestInventoryEntity): Long

    @Query("SELECT * FROM ForestInventoryEntity WHERE forestInventoryId=:inventoryId")
    suspend fun getForestInventory(inventoryId: Long): ForestInventoryEntity

    @Query("SELECT * FROM ForestInventoryEntity WHERE status=0 LIMIT 1")
    suspend fun getIncompleteForestInventory(): ForestInventoryEntity?

    @Query("SELECT * FROM ForestInventoryEntity WHERE status=0 AND activityId=:id LIMIT 1")
    suspend fun getIncompleteForestInventoryByActivityId(id: Long): ForestInventoryEntity?

    @Query("UPDATE ForestInventoryEntity SET status=:status WHERE forestInventoryId=:inventoryId")
    suspend fun updateForestInventoryStatus(inventoryId: Long, status: Int)

    @Query("DELETE FROM TMEntity WHERE forestInventoryId=:inventoryId")
    suspend fun deleteInventoryMeasurements(inventoryId: Long)

    @Query("DELETE FROM ForestInventoryEntity WHERE forestInventoryId=:inventoryId")
    suspend fun deleteIncompleteInventory(inventoryId: Long)

    @Query("SELECT * FROM  ForestInventoryEntity WHERE forestInventoryId = (SELECT MAX(forestInventoryId)  FROM ForestInventoryEntity)")
    suspend fun getLatestForestInventory(): ForestInventoryEntity?

    @Query("SELECT COUNT(measurementUUID) FROM TMEntity WHERE activityId=:activityId")
    suspend fun countTreeMeasurementsForActivity(activityId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreeSpecie(treeSpecie: TreeSpecieEntity): Long

    @Query("SELECT * FROM TreeSpecieEntity")
    suspend fun getTreeSpecies(): List<TreeSpecieEntity>
}
