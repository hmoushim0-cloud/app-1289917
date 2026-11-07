package sa.factory.rekabtelematicscar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RekabTelematicsDao {
    @Query("SELECT * FROM car_rental_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<RekabTelematicsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<RekabTelematicsEntity>)

    @Query("DELETE FROM car_rental_items_cache")
    suspend fun clearAll()
}
