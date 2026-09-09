package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ViolationDao {
    // --- VIOLATIONS ---
    @Query("SELECT * FROM violations ORDER BY timestamp DESC")
    fun getAllViolations(): Flow<List<ViolationRecord>>

    @Query("SELECT * FROM violations WHERE isPaid = 0 ORDER BY timestamp DESC")
    fun getUnpaidViolations(): Flow<List<ViolationRecord>>

    @Query("SELECT * FROM violations WHERE isPaid = 1 ORDER BY timestamp DESC")
    fun getPaidViolations(): Flow<List<ViolationRecord>>

    @Query("SELECT * FROM violations WHERE id = :id LIMIT 1")
    suspend fun getViolationById(id: Long): ViolationRecord?

    @Query("SELECT * FROM violations WHERE numberPlate = :plate ORDER BY timestamp DESC")
    fun getViolationsByPlate(plate: String): Flow<List<ViolationRecord>>

    @Query("SELECT * FROM violations WHERE ownerProfileId = :ownerId ORDER BY timestamp DESC")
    fun getViolationsByOwner(ownerId: Long): Flow<List<ViolationRecord>>

    @Query("SELECT COUNT(*) FROM violations")
    fun getTotalViolationsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM violations WHERE isPaid = 0")
    fun getUnpaidCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM violations WHERE isPaid = 1")
    fun getPaidCount(): Flow<Int>

    @Query("SELECT SUM(fineAmount) FROM violations WHERE isPaid = 0")
    fun getTotalUnpaidAmount(): Flow<Int?>

    @Query("SELECT SUM(fineAmount) FROM violations WHERE isPaid = 1")
    fun getTotalPaidAmount(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM violations WHERE noiseDb >= 85 OR violationType LIKE '%Honking%'")
    fun getExcessiveNoiseViolationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViolation(violation: ViolationRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViolations(violations: List<ViolationRecord>)

    @Update
    suspend fun updateViolation(violation: ViolationRecord)

    @Query("UPDATE violations SET isPaid = :isPaid, daysOverdue = 0 WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, isPaid: Boolean)

    @Query("DELETE FROM violations WHERE id = :id")
    suspend fun deleteViolationById(id: Long)

    @Query("DELETE FROM violations")
    suspend fun clearAll()

    // --- OWNER PROFILES ---
    @Query("SELECT * FROM owner_profiles ORDER BY id ASC")
    fun getAllOwners(): Flow<List<OwnerProfile>>

    @Query("SELECT * FROM owner_profiles WHERE id = :id LIMIT 1")
    suspend fun getOwnerById(id: Long): OwnerProfile?

    @Query("SELECT * FROM owner_profiles WHERE vehicleRegistrationNumber = :plate LIMIT 1")
    suspend fun getOwnerByPlate(plate: String): OwnerProfile?

    @Query("""
        SELECT * FROM owner_profiles 
        WHERE REPLACE(REPLACE(LOWER(vehicleRegistrationNumber), ' ', ''), '-', '') = LOWER(:cleanPlate) 
        LIMIT 1
    """)
    suspend fun getOwnerByNormalizedPlate(cleanPlate: String): OwnerProfile?

    @Query("SELECT * FROM owner_profiles WHERE mobileNumber LIKE '%' || :phone || '%' LIMIT 1")
    suspend fun getOwnerByPhone(phone: String): OwnerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwner(owner: OwnerProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwners(owners: List<OwnerProfile>)

    @Update
    suspend fun updateOwner(owner: OwnerProfile)

    // --- VEHICLES ---
    @Query("SELECT * FROM vehicles ORDER BY id ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE numberPlate = :plate LIMIT 1")
    suspend fun getVehicleByPlate(plate: String): Vehicle?

    @Query("SELECT * FROM vehicles WHERE ownerProfileId = :ownerId")
    fun getVehiclesByOwner(ownerId: Long): Flow<List<Vehicle>>

    @Query("SELECT COUNT(*) FROM vehicles")
    fun getTotalVehiclesCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<Vehicle>)

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    // --- USER ACCOUNTS ---
    @Query("""
        SELECT * FROM user_accounts 
        WHERE LOWER(TRIM(username)) = LOWER(TRIM(:query)) 
           OR LOWER(TRIM(email)) = LOWER(TRIM(:query)) 
           OR LOWER(TRIM(badgeOrVehicleNo)) = LOWER(TRIM(:query))
           OR REPLACE(REPLACE(LOWER(TRIM(badgeOrVehicleNo)), ' ', ''), '-', '') = REPLACE(REPLACE(LOWER(TRIM(:query)), ' ', ''), '-', '')
        LIMIT 1
    """)
    suspend fun findUserByUsernameOrEmail(query: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE LOWER(role) = 'admin_police' LIMIT 1")
    suspend fun getFirstAdminUser(): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE LOWER(role) = 'vehicle_owner' LIMIT 1")
    suspend fun getFirstCitizenUser(): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE ownerProfileId = :ownerId LIMIT 1")
    suspend fun getUserByOwnerProfileId(ownerId: Long): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserAccount?

    @Query("SELECT * FROM user_accounts ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserAccount>)

    @Update
    suspend fun updateUser(user: UserAccount)

    @Query("DELETE FROM user_accounts WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    // --- AUDIT LOGS ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long
}
