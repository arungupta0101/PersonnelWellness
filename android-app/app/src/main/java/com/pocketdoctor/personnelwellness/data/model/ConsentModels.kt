package com.pocketdoctor.personnelwellness.data.model

import com.google.gson.annotations.SerializedName

data class ConsentUpdateRequest(
    @SerializedName("consent_type") val consentType: String = "data_sharing_wellness"
)

data class ConsentStatusResponse(
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("consent_type") val consentType: String = "data_sharing_wellness",
    val granted: Boolean = false,
    @SerializedName("granted_at") val grantedAt: String? = null,
    val preferences: ConsentPreferences = ConsentPreferences()
)

data class ConsentPreferences(
    @SerializedName("mandatory_accepted") val mandatoryAccepted: Boolean = true,
    @SerializedName("optional_biometrics_accepted") val optionalBiometricsAccepted: Boolean = false,
    @SerializedName("optional_workload_logs_accepted") val optionalWorkloadLogsAccepted: Boolean = false
)
