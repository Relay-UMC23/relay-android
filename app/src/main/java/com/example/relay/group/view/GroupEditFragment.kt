package com.example.relay.group.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.bumptech.glide.Glide
import com.example.relay.R
import com.example.relay.databinding.FragmentGroupCreateBinding
import com.example.relay.group.models.GroupEditRequest
import com.example.relay.group.models.GroupInfoResponse
import com.example.relay.group.service.*
import com.example.relay.ui.MainActivity
import kotlinx.android.synthetic.main.dialog_goal_km.view.*
import kotlinx.android.synthetic.main.dialog_goal_time.view.*
import kotlinx.android.synthetic.main.dialog_goal_time.view.btn_cancel
import kotlinx.android.synthetic.main.dialog_goal_time.view.btn_save
import kotlinx.android.synthetic.main.dialog_goal_type.view.*
import kotlinx.android.synthetic.main.dialog_people_cnt.view.*
import kotlinx.android.synthetic.main.dialog_question.view.*
import java.text.DecimalFormat

class GroupEditFragment: Fragment(), GetClubDetailInterface, PatchClubInterface, PatchClubDeleteInterface {
    private var _binding: FragmentGroupCreateBinding? = null
    private val binding get() = _binding!!

    private var mainActivity: MainActivity? = null

    private var goal: Float = 0F
    private var clubIdx = 0L
    private var curDate = ""

    private val bucketURL = "https://team23-bucket.s3.ap-northeast-2.amazonaws.com/public/club"

    override fun onAttach(context: Context) {
        if (context != null) {
            super.onAttach(context)
        }
        mainActivity = activity as MainActivity?
    }

    override fun onDetach() {
        super.onDetach()
        mainActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ????????? ??????
        binding.swRecruitStatus.setOnCheckedChangeListener { p0, isChecked ->
            if (isChecked) {
                binding.tvRecruitStatus.text = "?????????"
            } else {
                binding.tvRecruitStatus.text = "????????????"
            }
        }

        binding.line3.visibility = View.VISIBLE
        binding.btnDelete.visibility = View.VISIBLE

        binding.tvGroupCreate.text = "?????? ??????"
        binding.btnNext.text = "??????"


        // ????????? ?????? (type)
        binding.btnGoalType.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_goal_type, null)
            val alertDialog = activity?.let { AlertDialog.Builder(it).create() }

            alertDialog?.setView(dialogView)
            alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog?.show()

            val itemView = dialogView.list_dialog_item
            val goalType = arrayOf("?????? ??????", "??????", "??????")

            itemView.adapter =
                activity?.let { it1 -> ArrayAdapter(it1, android.R.layout.simple_list_item_1, goalType) }

            itemView.onItemClickListener = AdapterView.OnItemClickListener {
                    parent,
                    view,
                    position,
                    id ->
                binding.tvGoalType.text = itemView.adapter.getItem(position).toString()
                if (position == 0) {
                    binding.tvGoalValue.text = "----"
                } else if (position == 1) {
                    binding.tvGoalValue.text = "00 : 00 : 00"
                } else {
                    binding.tvGoalValue.text = "00.00km"
                }
                alertDialog?.dismiss()
            }
        }

        // ????????? ?????? (value)
        binding.btnGoalValue.setOnClickListener {
            if (binding.tvGoalType.text.equals("??????")) {
                val dialogView = layoutInflater.inflate(R.layout.dialog_goal_time, null)
                val alertDialog = activity?.let { AlertDialog.Builder(it).create() }

                alertDialog?.setView(dialogView)
                alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog?.show()

                // ??????, ????????? ??????
                dialogView.np_hour.minValue = 0
                dialogView.np_hour.maxValue = 3
                dialogView.np_min.minValue = 0
                dialogView.np_min.maxValue = 59
                dialogView.np_sec.minValue = 0
                dialogView.np_sec.maxValue = 59

                // ????????? ??????
                dialogView.np_hour.value = Integer.parseInt(binding.tvGoalValue.text.substring(0, 2))
                dialogView.np_min.value = Integer.parseInt(binding.tvGoalValue.text.substring(5, 7))
                dialogView.np_sec.value = Integer.parseInt(binding.tvGoalValue.text.substring(10, 12))

                // ?????? ??????
                dialogView.btn_save.setOnClickListener {
                    var hour = dialogView.np_hour.value.toString().padStart(2, '0')
                    var min = dialogView.np_min.value.toString().padStart(2, '0')
                    var sec = dialogView.np_sec.value.toString().padStart(2, '0')

                    if ((hour == "00" && min == "00" && sec == "00")) {
                        Toast.makeText(activity, "????????? ??????????????????!", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.tvGoalValue.text = "${hour} : ${min} : ${sec}"
                        alertDialog?.dismiss()
                    }
                }

                // ?????? ??????
                dialogView.btn_cancel.setOnClickListener {
                    alertDialog?.dismiss()
                }
            } else if ((binding.tvGoalType.text.equals("??????"))) {
                val dialogView = layoutInflater.inflate(R.layout.dialog_goal_km, null)
                val alertDialog = activity?.let { AlertDialog.Builder(it).create() }

                alertDialog?.setView(dialogView)
                alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog?.show()

                dialogView.tv_title.text = "?????? / km"

                // ??????, ????????? ??????
                dialogView.np1.minValue = 0
                dialogView.np1.maxValue = 40
                dialogView.np2.minValue = 0
                dialogView.np2.maxValue = 99

                // ????????? ??????
                dialogView.np1.value = Integer.parseInt(binding.tvGoalValue.text.substring(0, 2))
                dialogView.np2.value = Integer.parseInt(binding.tvGoalValue.text.substring(3, 5))

                // ?????? ??????
                dialogView.btn_save.setOnClickListener {
                    var n1 = dialogView.np1.value.toString().padStart(2, '0')
                    var n2 = dialogView.np2.value.toString().padStart(2, '0')

                    if ((n1 == "00" && n2 == "00")) {
                        Toast.makeText(activity, "????????? ??????????????????!", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.tvGoalValue.text = "${n1}.${n2}km"
                        alertDialog?.dismiss()
                    }
                }

                // ?????? ??????
                dialogView.btn_cancel.setOnClickListener {
                    alertDialog?.dismiss()
                }
            }
        }

        // ????????? ??????
        binding.btnPeopleCnt.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_people_cnt, null)
            val alertDialog = activity?.let { AlertDialog.Builder(it).create() }

            alertDialog?.setView(dialogView)
            alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog?.show()

            // ??????, ????????? ??????
            dialogView.np_people.minValue = 1
            dialogView.np_people.maxValue = 8

            // ????????? ??????
            dialogView.np_people.value = Integer.parseInt(binding.tvPeopleCnt.text.toString())

            // ?????? ??????
            dialogView.btn_save.setOnClickListener {
                binding.tvPeopleCnt.text = dialogView.np_people.value.toString()
                alertDialog?.dismiss()
            }

            // ?????? ??????
            dialogView.btn_cancel.setOnClickListener {
                alertDialog?.dismiss()
            }
        }

        // ?????? ?????? ??????
        binding.btnRunnerLevel.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_people_cnt, null)
            val alertDialog = activity?.let { AlertDialog.Builder(it).create() }

            alertDialog?.setView(dialogView)
            alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog?.show()

            dialogView.tv_people.text = "??????"

            // ??????, ?????????, ????????? ??????
            val levelList = arrayOf("?????? ??????", "?????? ??????", "?????? ??????", "?????? ??????")

            dialogView.np_people.displayedValues = levelList
            dialogView.np_people.minValue = 0
            dialogView.np_people.maxValue = 3

            val value = binding.tvRunnerLevel.text.toString()
            val index = levelList.indexOf(value)
            dialogView.np_people.value = index

            // ?????? ??????
            dialogView.btn_save.setOnClickListener {
                binding.tvRunnerLevel.text = levelList[dialogView.np_people.value]
                alertDialog?.dismiss()
            }

            // ?????? ??????
            dialogView.btn_cancel.setOnClickListener {
                alertDialog?.dismiss()
            }
        }

        // ?????? ??????
        binding.btnNext.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_question, null)
            val alertDialog = activity?.let { AlertDialog.Builder(it).create() }

            dialogView.tv_question.text = "?????? ????????? ?????????????????????????"
            alertDialog?.setView(dialogView)
            alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog?.show()

            dialogView.btn_q_cancel.setOnClickListener {
                alertDialog?.dismiss()
            }

            dialogView.btn_q_ok.setOnClickListener {
                alertDialog?.dismiss()
                val content = binding.etInfo.text.toString()
                val name = binding.etName.text.toString()
                val imgURL = bucketURL + "/yellow.png"
                val maxNum = Integer.parseInt(binding.tvPeopleCnt.text.toString())

                val goalText = binding.tvGoalValue.text.toString()
                var goalType = ""
                when (binding.tvGoalType.text) {
                    "??????" -> {
                        goalType = "TIME"
                        val h = goalText.substring(0, 2).toFloat()
                        val m = goalText.substring(5, 7).toFloat()
                        val s = goalText.substring(10, 12).toFloat()
                        goal = h * 60 * 60 + m * 60 + s
                    }
                    "??????" -> {
                        goalType = "DISTANCE"
                        goal = goalText.substring(0, 5).toFloat()
                    }
                    else -> {
                        goalType = "NOGOAL"
                    }
                }

                var level = 0
                level = when (binding.tvRunnerLevel.text) {
                    "?????? ??????" -> 1
                    "?????? ??????" -> 2
                    "?????? ??????" -> 3
                    else -> 0
                }

                var recruitingStatus = ""
                recruitingStatus = if (binding.tvRecruitStatus.text == "?????????") {
                    "recruiting"
                } else {
                    "finished"
                }

                val req = GroupEditRequest(content, goal, goalType, imgURL, level, maxNum, name, recruitingStatus)
                PatchClubService(this).tryPatchClub(clubIdx, req)
            }
        }

        // ?????? ??????
        binding.btnBack.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                "go_to_main",
                bundleOf("clubIdx" to clubIdx))
            mainActivity?.groupFragmentChange(0) // ?????? ?????????
        }

        setFragmentResultListener("go_to_edit") { requestKey, bundle ->
            clubIdx = bundle.getLong("clubIdx")
            curDate = bundle.getString("curDate", "") // "yyyy-mm-dd"

            GetClubDetailService(this).tryGetClubDetail(clubIdx, curDate)
        }

        // ?????? ????????????
        binding.btnDelete.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_question, null)
            val alertDialog = activity?.let { AlertDialog.Builder(it).create() }

            dialogView.tv_question.text = "????????? ?????????????????????????"
            alertDialog?.setView(dialogView)
            alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog?.show()

            dialogView.btn_q_cancel.setOnClickListener {
                alertDialog?.dismiss()
            }

            dialogView.btn_q_ok.setOnClickListener {
                alertDialog?.dismiss()
                PatchClubDeleteService(this).tryPatchClubDelete(clubIdx)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onGetClubDetailSuccess(response: GroupInfoResponse) {
        if (response.isSuccess) {
            val res = response.result

            binding.etName.setText(res.name)
            binding.etInfo.setText(res.content)
            binding.swRecruitStatus.isChecked = res.recruitStatus == "recruiting"
            binding.tvPeopleCnt.text = res.maxNum.toString()

            Glide.with(binding.imgGroup.context)
                .load(res.imgUrl)
                .into(binding.imgGroup)

            when (res.goalType) {
                "TIME" -> {
                    var sec = res.goal
                    var min = sec / 60
                    val hour = min / 60
                    min %= 60
                    sec %= 60

                    val hh = hour.toInt().toString().padStart(2, '0')
                    val mm = min.toInt().toString().padStart(2, '0')
                    val ss = sec.toInt().toString().padStart(2, '0')

                    binding.tvGoalType.text = "??????"
                    binding.tvGoalValue.text = "$hh : $mm : $ss"
                }
                "DISTANCE" -> {
                    val dis = DecimalFormat("00.00").format(res.goal).toString()

                    binding.tvGoalType.text = "??????"
                    binding.tvGoalValue.text = "${dis}km"
                }
            }

            when (res.level) {
                1 -> binding.tvRunnerLevel.text = "?????? ??????"
                2 -> binding.tvRunnerLevel.text = "?????? ??????"
                3 -> binding.tvRunnerLevel.text = "?????? ??????"
                else -> binding.tvRunnerLevel.text = "?????? ??????"
            }
        } else {
            Toast.makeText(activity, "?????? ????????? ???????????? ??? ??????????????????.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onGetClubDetailFailure(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPatchClubInSuccess() {
        Toast.makeText(activity, "?????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
    }

    override fun onPatchClubInFailure(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPatchClubDeleteInSuccess() {
        Toast.makeText(activity, "?????? ????????? ?????????????????????.", Toast.LENGTH_SHORT).show()
        mainActivity?.groupFragmentChange(1) // ?????? ??????
    }

    override fun onPatchClubDeleteInFailure(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}