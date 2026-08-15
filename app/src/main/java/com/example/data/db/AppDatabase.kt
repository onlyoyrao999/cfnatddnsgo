package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CfDnsRuleEntity
import com.example.data.model.ScanHistoryEntity
import com.example.data.model.ScannedIpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedIpDao {
    @Query("SELECT * FROM saved_ips ORDER BY latencyMs ASC")
    fun getAllSavedIps(): Flow<List<ScannedIpEntity>>

    @Query("SELECT * FROM saved_ips WHERE isFavorite = 1 ORDER BY latencyMs ASC")
    fun getFavoriteIps(): Flow<List<ScannedIpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIp(ip: ScannedIpEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIps(ips: List<ScannedIpEntity>)

    @Query("UPDATE saved_ips SET isFavorite = :isFav WHERE ip = :ip")
    suspend fun updateFavorite(ip: String, isFav: Boolean)

    @Delete
    suspend fun deleteIp(ip: ScannedIpEntity)

    @Query("DELETE FROM saved_ips WHERE ip = :ip")
    suspend fun deleteIpByAddress(ip: String)

    @Query("DELETE FROM saved_ips")
    suspend fun clearAll()

    @Query("DELETE FROM saved_ips WHERE ip NOT IN (SELECT ip FROM saved_ips ORDER BY testedAt DESC LIMIT 100)")
    suspend fun trimToTop100Newest()
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 50")
    fun getScanHistory(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun clearHistory()
}

@Dao
interface CfDnsRuleDao {
    @Query("SELECT * FROM cf_dns_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<CfDnsRuleEntity>>

    @Query("SELECT * FROM cf_dns_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<CfDnsRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CfDnsRuleEntity): Long

    @Update
    suspend fun updateRule(rule: CfDnsRuleEntity)

    @Query("DELETE FROM cf_dns_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("UPDATE cf_dns_rules SET lastSyncStatus = :status, lastSyncedIp = :ip, lastSyncTime = :time WHERE id = :id")
    suspend fun updateSyncResult(id: Long, status: String, ip: String, time: Long)
}

@Database(
    entities = [ScannedIpEntity::class, ScanHistoryEntity::class, CfDnsRuleEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedIpDao(): ScannedIpDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun cfDnsRuleDao(): CfDnsRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE cf_dns_rules ADD COLUMN maxIpCount INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {
                    // Column might already exist
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cf_ip_scanner.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
