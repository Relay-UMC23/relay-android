package com.example.relay.running.models

import com.example.relay.BaseResponse
import com.google.gson.annotations.SerializedName

data class RunStrResponse(
    @SerializedName("result") val result: RunStrResult
) : BaseResponse()
