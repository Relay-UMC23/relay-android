package com.example.relay.timetable.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResultListener
import com.example.relay.ApplicationClass.Companion.prefs
import com.example.relay.databinding.FragmentTimetableEditMainBinding
import com.example.relay.timetable.models.GroupTimetableRes
import com.example.relay.timetable.models.MyTimetableRes
import com.example.relay.timetable.service.TimetableGetInterface
import com.example.relay.timetable.service.TimetableGetService
import com.example.relay.ui.MainActivity
import com.islandparadise14.mintable.model.ScheduleEntity

class TimetableEditMainFragment : Fragment(), TimetableGetInterface {
    private var _binding: FragmentTimetableEditMainBinding ?= null
    private val binding get() = _binding!!

    private val day = arrayOf("일", "월", "화", "수", "목", "금", "토")
    private val colorCode =  arrayOf("#FE0000", "#01A6EA", "#FFAD01", "#FFDD00", "#BBDA00", "#F71873", "#6DD0E7", "#84C743")
    private var userIdx = prefs.getLong("userIdx", 0L)
    private var clubIdx = -1L
    private var clubName = "팀 시간표"
    private var hasGroup = false

    private var mainActivity: MainActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = activity as MainActivity?
    }

    override fun onDetach() {
        super.onDetach()
        mainActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimetableEditMainBinding.inflate(inflater, container, false)
        with(binding.timetable){
            baseSetting(35, 40, 40)
            initTable(day)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnMyTimetable.setOnClickListener{
            if (binding.tvTitle.text != "개인 시간표") {
                binding.tvTitle.text = "개인 시간표"
                TimetableGetService(this).tryGetMySchedules(userIdx)
            } else {
                binding.tvTitle.text = clubName
                TimetableGetService(this).tryGetGroupSchedules(clubIdx)
            }
        }

        binding.btnBack.setOnClickListener{
            if (clubIdx == -1L) // 새로 만들다가 뒤로가기
                (activity as MainActivity).groupFragmentChange(3)
            else if (!hasGroup) // 그룹 가입하다가 뒤로가기
                (activity as MainActivity).groupFragmentChange(1)
            else
                (activity as MainActivity).timetableFragmentChange(0)
        }

        clubIdxSetting()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun clubIdxSetting(){
        setFragmentResultListener("go_to_edit_main_timetable") {requestKey, bundle ->
            clubIdx = bundle.getLong("clubIdx", 0L)
            hasGroup = bundle.getBoolean("hasGroup", false)
            childFragmentManager.setFragmentResult("forJoin",
                bundleOf("clubIdx" to clubIdx)
            )
            clubName = bundle.getString("clubName", "오류") + " 팀"
            binding.tvTitle.text = clubName
            TimetableGetService(this).tryGetGroupSchedules(clubIdx)
        }

        setFragmentResultListener("go_to_edit_for_new_group") {requestKey, bundle ->
            childFragmentManager.setFragmentResult("forNewGroup",
                bundleOf(
                    "clubIdx" to -2L,
                    "content" to bundle.getString("content", "러너들!"),
                    "goal" to bundle.getFloat("goal", 0F),
                    "goalType" to bundle.getString("goalType", "TIME"),
                    "level" to bundle.getInt("level", 0),
                    "name" to bundle.getString("name", "디폴트"),
                    "maxNum" to bundle.getInt("maxNum", 8)
                )
            )
            binding.btnMyTimetable.visibility = View.GONE
        }
    }

    override fun onGetGroupTimetableSuccess(response: GroupTimetableRes) {
        if (response.code == 1000){
            val clubMemList = response.result
            val sList: ArrayList<ScheduleEntity> = ArrayList()
            for ((index, mem) in clubMemList.withIndex()){
                val color = colorCode[index]
                for (s in mem.timeTables){
                    val schedule = ScheduleEntity(
                        1,
                        mem.nickName,
                        s.day,
                        s.start,
                        s.end,
                        color,
                        "#FFFFFF"
                    )
                    sList.add(schedule)
                }
            }
            binding.timetable.updateSchedules(sList)
        } else
            Log.d("Timetable", "onGetGroupTimetableSuccess: code-${response.code}")
    }

    override fun onGetGroupTimetableFailure(message: String) {
        TODO("Not yet implemented")
    }

    override fun onGetMyTimetableSuccess(response: MyTimetableRes) {
        if (response.code == 1000){
            val sList: ArrayList<ScheduleEntity> = ArrayList()
            val name = prefs.getString("name", "오류")
            for (item in response.result){
                val schedule = ScheduleEntity(
                    1,
                    name!!,
                    item.day,
                    item.start,
                    item.end,
                    colorCode[0],
                    "#FFFFFF"
                )
                sList.add(schedule)
            }
            binding.timetable.updateSchedules(sList)
        } else
            binding.timetable.initTable(day)
    }

    override fun onGetMyTimetableFailure(message: String) {
        TODO("Not yet implemented")
    }
}