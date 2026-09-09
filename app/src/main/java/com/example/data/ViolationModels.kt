package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val email: String,
    val password: String,
    val role: String = "ADMIN_POLICE", // "ADMIN_POLICE" or "VEHICLE_OWNER"
    val fullName: String = "",
    val badgeOrVehicleNo: String = "",
    val ownerProfileId: Long? = null,
    val rememberMe: Boolean = true,
    val firebaseUid: String = "",
    val isActive: Boolean = true,
    val lastLoginTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "owner_profiles")
data class OwnerProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val photoUrl: String = "", // If blank or empty -> "Owner Photo Not Available"
    val mobileNumber: String = "",
    val email: String = "",
    val vehicleRegistrationNumber: String = "",
    val vehicleType: String = "SUV",
    val vehicleModel: String = "",
    val vehicleColor: String = "",
    val drivingLicense: String = "",
    val address: String = "",
    val emergencyContact: String = "",
    val registeredDate: String = "15 Jan 2022"
)

@Entity(
    tableName = "vehicles",
    indices = [Index(value = ["numberPlate"], unique = true)]
)
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerProfileId: Long,
    val numberPlate: String,
    val vehicleType: String = "Car",
    val vehicleModel: String = "",
    val vehicleColor: String = "",
    val engineNumber: String = "ENG-849201",
    val chassisNumber: String = "CHS-5829104",
    val insuranceValidUntil: String = "15 Dec 2027",
    val puccValidUntil: String = "20 Oct 2026",
    val registeredDate: String = "2022-01-15"
)

@Entity(tableName = "violations")
data class ViolationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numberPlate: String,
    val vehicleType: String = "Car",
    val vehicleModel: String = "Mahindra XUV700",
    val vehicleColor: String = "Midnight Black",
    val ownerProfileId: Long? = null,
    val ownerName: String = "Rahul Kumar",
    val ownerPhotoUrl: String = "", // Empty means "Owner Photo Not Available"
    val ownerMobile: String = "+91 98765 43210",
    val drivingLicense: String = "OD02-20180048291",
    val violationType: String,
    val fineAmount: Int,
    val isPaid: Boolean = false,
    val daysOverdue: Int = 0,
    val location: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = "20:42:18 IST",
    val dateFormatted: String = "Today",
    val noiseDb: Int = 0,
    val speedKmh: Int = 0,
    val evidenceImageUrl: String = "",
    val croppedPlateUrl: String = "",
    val isBillboardBroadcasted: Boolean = true,
    val challanNumber: String = "89410",
    val verifiedByAdmin: Boolean = true
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val adminUser: String = "OFFICER_PATIL_04",
    val details: String
)

data class VideoTimestampMarker(
    val timeSeconds: Int,
    val timeLabel: String,
    val eventDescription: String,
    val iconName: String
)

data class ScanResultData(
    val mediaType: String = "IMAGE", // IMAGE or VIDEO
    val mediaUri: String = "",
    val vehiclesDetected: Int = 7,
    val platesDetected: Int = 5,
    val violationsDetected: Int = 2,
    val noiseLevelDb: Int = 91,
    val confidencePercent: Int = 97,
    val vehicleType: String = "Car",
    val plateNumber: String = "OD 02 AB 1234",
    val vehicleModel: String = "Mahindra XUV700",
    val vehicleColor: String = "Midnight Black",
    val matchedOwner: OwnerProfile? = null,
    val violationDescription: String = "Excessive Honking",
    val location: String = "Intersection 04 • AIIMS Perimeter",
    val timeString: String = "08:42 PM",
    val speedKmh: Int = 48,
    val videoProgressPercent: Int = 100,
    val videoMarkers: List<VideoTimestampMarker> = listOf(
        VideoTimestampMarker(12, "00:12", "Vehicle detected (Black SUV)", "directions_car"),
        VideoTimestampMarker(18, "00:18", "Number plate recognized (OD 02 AB 1234)", "pin"),
        VideoTimestampMarker(24, "00:24", "Excessive honking detected (91 dB)", "volume_up"),
        VideoTimestampMarker(25, "00:25", "Violation recorded & fine generated", "gavel")
    )
)
