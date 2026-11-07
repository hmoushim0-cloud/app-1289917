package sa.factory.rekabtelematicscar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RekabTelematicsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun car_rentalDao(): RekabTelematicsDao
}
