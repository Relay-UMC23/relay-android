package com.example.relay.group

import android.util.Log
import com.example.relay.ApplicationClass
import com.example.relay.group.models.GroupAcceptedResponse
import com.example.relay.group.models.GroupInfoResponse
import com.example.relay.group.models.GroupListResponse
import com.example.relay.group.models.MemberResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GetUserClubService(val mainInterface: GetUserClubInterface) {

    private val retrofit: GroupRetrofit = ApplicationClass.sRetrofit.create(GroupRetrofit::class.java)

    fun tryGetUserClub(id: Long){
        retrofit.getUserProfileClubRes(id).enqueue(object : Callback<GroupAcceptedResponse>{
            override fun onResponse(call: Call<GroupAcceptedResponse>, response: Response<GroupAcceptedResponse>) {
                Log.d("GroupAcceptedResponse", "success")

                if (response.code() == 200) {
                    mainInterface.onGetUserClubSuccess(response.body() as GroupAcceptedResponse)
                } else {
                    Log.d("GroupAcceptedResponse", "4xx error")
                    // 서버 통신은 성공했으나 오류 코드 받았을 때
                }
            }

            override fun onFailure(call: Call<GroupAcceptedResponse>, t: Throwable) {
                Log.d("GroupAcceptedResponse", "fail")
                t.printStackTrace()
                mainInterface.onGetUserClubFailure(t.message ?: "통신 오류")
            }
        })
    }
}

class GetClubListService(val listInterface: GetClubListInterface) {

    private val retrofit: GroupRetrofit = ApplicationClass.sRetrofit.create(GroupRetrofit::class.java)

    fun tryGetClubList(search: String){
        retrofit.getClubListRes(search).enqueue(object : Callback<GroupListResponse> {
            override fun onResponse(call: Call<GroupListResponse>, response: Response<GroupListResponse>) {
                Log.d("GroupListResponse", "success")

                if (response.code() == 200) {
                    listInterface.onGetClubListSuccess(response.body() as GroupListResponse)
                } else {
                    Log.d("GroupListResponse", "4xx error")
                    // 서버 통신은 성공했으나 오류 코드 받았을 때
                }
            }

            override fun onFailure(call: Call<GroupListResponse>, t: Throwable) {
                Log.d("GroupListResponse", "fail")
                t.printStackTrace()
                listInterface.onGetClubListFailure(t.message ?: "통신 오류")
            }
        })
    }
}

class GetClubDetailService(val detailInterface: GetClubDetailInterface) {

    private val retrofit: GroupRetrofit = ApplicationClass.sRetrofit.create(GroupRetrofit::class.java)

    fun tryGetClubDetail(clubIdx: Long, date: String){
        retrofit.getClubDetailRes(clubIdx, date).enqueue(object : Callback<GroupInfoResponse>{
            override fun onResponse(call: Call<GroupInfoResponse>, response: Response<GroupInfoResponse>) {
                Log.d("GroupInfoResponse", "success")

                if (response.code() == 200) {
                    detailInterface.onGetClubDetailSuccess(response.body() as GroupInfoResponse)
                } else {
                    Log.d("GroupInfoResponse", "4xx error")
                    // 서버 통신은 성공했으나 오류 코드 받았을 때
                }
            }

            override fun onFailure(call: Call<GroupInfoResponse>, t: Throwable) {
                Log.d("GroupInfoResponse", "fail")
                t.printStackTrace()
                detailInterface.onGetClubDetailFailure(t.message ?: "통신 오류")
            }
        })
    }
}

class GetMemberListService(val memberInterface: GetMemberListInterface) {

    private val retrofit: GroupRetrofit = ApplicationClass.sRetrofit.create(GroupRetrofit::class.java)

    fun tryGetMemberList(clubIdx: Long, date: String){
        retrofit.getClubMemberRes(clubIdx, date).enqueue(object : Callback<MemberResponse>{
            override fun onResponse(call: Call<MemberResponse>, response: Response<MemberResponse>) {
                Log.d("MemberResponse", "success")

                if (response.code() == 200) {
                    memberInterface.onGetMemberListSuccess(response.body() as MemberResponse)
                } else {
                    Log.d("MemberResponse", "4xx error")
                    // 서버 통신은 성공했으나 오류 코드 받았을 때
                }
            }

            override fun onFailure(call: Call<MemberResponse>, t: Throwable) {
                Log.d("MemberResponse", "fail")
                t.printStackTrace()
                memberInterface.onGetMemberListFailure(t.message ?: "통신 오류")
            }
        })
    }
}