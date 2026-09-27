package com.pocketdoctor.personnelwellness.data.model

import com.google.gson.annotations.SerializedName

data class HealthResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("database") val database: String? = null
)
