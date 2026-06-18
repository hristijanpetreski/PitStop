package uk.hristijan.pitstop.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import uk.hristijan.pitstop.core.model.FavoritePlaceType
import uk.hristijan.pitstop.core.model.FuelType
import uk.hristijan.pitstop.core.model.ServiceCategory
import uk.hristijan.pitstop.core.model.ServicePerformer
import uk.hristijan.pitstop.data.local.dao.FavoritePlaceDao
import uk.hristijan.pitstop.data.local.dao.RefillDao
import uk.hristijan.pitstop.data.local.dao.ServiceRecordDao
import uk.hristijan.pitstop.data.local.dao.VehicleDao
import uk.hristijan.pitstop.data.local.entity.FavoritePlace
import uk.hristijan.pitstop.data.local.entity.Refill
import uk.hristijan.pitstop.data.local.entity.ServiceItem
import uk.hristijan.pitstop.data.local.entity.ServiceRecord
import uk.hristijan.pitstop.data.local.entity.Vehicle

@Database(
    entities = [Vehicle::class, Refill::class, ServiceRecord::class, ServiceItem::class, FavoritePlace::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(PitStopTypeConverters::class)
abstract class PitStopDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun refillDao(): RefillDao
    abstract fun serviceRecordDao(): ServiceRecordDao
    abstract fun favoritePlaceDao(): FavoritePlaceDao

    companion object {
        const val DATABASE_NAME = "pitstop.db"

        @Volatile private var instance: PitStopDatabase? = null

        fun getInstance(context: Context): PitStopDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PitStopDatabase::class.java,
                DATABASE_NAME,
            ).build().also { instance = it }
        }
    }
}

class PitStopTypeConverters {
    @TypeConverter fun fuelTypeToString(value: FuelType): String = value.name
    @TypeConverter fun stringToFuelType(value: String): FuelType = enumValueOf(value)
    @TypeConverter fun serviceCategoryToString(value: ServiceCategory): String = value.name
    @TypeConverter fun stringToServiceCategory(value: String): ServiceCategory = enumValueOf(value)
    @TypeConverter fun servicePerformerToString(value: ServicePerformer): String = value.name
    @TypeConverter fun stringToServicePerformer(value: String): ServicePerformer = enumValueOf(value)
    @TypeConverter fun placeTypeToString(value: FavoritePlaceType): String = value.name
    @TypeConverter fun stringToPlaceType(value: String): FavoritePlaceType = enumValueOf(value)
}
