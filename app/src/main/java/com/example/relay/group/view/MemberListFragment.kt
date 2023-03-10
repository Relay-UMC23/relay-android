package com.example.relay.group.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.relay.ApplicationClass.Companion.prefs
import com.example.relay.R
import com.example.relay.databinding.FragmentGroupMemberBinding
import com.example.relay.group.view.adapter.GroupMemberRVAdapter
import com.example.relay.group.models.Member
import com.example.relay.group.models.MemberResponse
import com.example.relay.group.service.*
import com.example.relay.ui.MainActivity
import kotlinx.android.synthetic.main.dialog_member_setting.view.*
import kotlinx.android.synthetic.main.dialog_question.view.*
import java.util.*

class MemberListFragment: Fragment(), GetMemberListInterface, PatchHostInterface, PatchMemberInterface {
    private var _binding: FragmentGroupMemberBinding? = null
    private val binding get() = _binding!!

    private var mainActivity: MainActivity? = null

    private var userIdx = prefs.getLong("userIdx", 0L)
    private var hostIdx = 0L
    private var clubIdx = 0L

    private var name = ""
    private var idx = 0L

    private var curDate = ""

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
        _binding = FragmentGroupMemberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val today = GregorianCalendar()
        var year: Int = today.get(Calendar.YEAR)
        var month: Int = today.get(Calendar.MONTH)
        var date: Int = today.get(Calendar.DATE)

        var strY = year.toString()
        var strM = (month+1).toString().padStart(2, '0')
        var strD = date.toString().padStart(2, '0')
        curDate = "${strY}-${strM}-${strD}"

        // ??????, ?????? -> ???????????????
        setFragmentResultListener("go_to_member_list") { requestKey, bundle ->
            clubIdx = bundle.getLong("clubIdx")
            hostIdx = bundle.getLong("hostIdx")

            GetMemberListService(this).tryGetMemberList(clubIdx, curDate)
        }

        // > ??????
            binding.btnRight.setOnClickListener {
                // ?????? -> ??????
                parentFragmentManager.setFragmentResult("go_to_main",
                    bundleOf("clubIdx" to clubIdx))
                mainActivity?.groupFragmentChange(0) // ?????? ???????????? ??????
            }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onGetMemberListSuccess(response: MemberResponse) {
        val res = response.result

        // ??????????????????
        val memberList: ArrayList<Member> = arrayListOf()
        val memberAdapter = GroupMemberRVAdapter(memberList, hostIdx, userIdx)

        memberList.clear()

        binding.rvGroupMember.adapter = memberAdapter
        binding.rvGroupMember.layoutManager = LinearLayoutManager(activity)

        if (res != null) {
            memberList.addAll(res)
        }

        memberAdapter.notifyDataSetChanged()


        // ?????????????????? ????????? ?????? ?????????
        memberAdapter.setItemClickListener( object : GroupMemberRVAdapter.ItemClickListener {
            override fun onMemberClick(view: View, position: Int) {

                val userIdx = memberList[position].profile.userIdx

                // ?????? ????????? -> ?????? ?????????
                parentFragmentManager.setFragmentResult("go_to_member_page",
                    bundleOf("clubIdx" to clubIdx, "hostIdx" to hostIdx, "userIdx" to userIdx))
                mainActivity?.groupFragmentChange(5) // ?????? ???????????? ??????
            }

            override fun onSettingClick(view: View, position: Int) {
                name = memberList[position].profile.nickname
                idx = memberList[position].profile.userIdx

                val dialogView = layoutInflater.inflate(R.layout.dialog_member_setting, null)
                val alertDialog = activity?.let { AlertDialog.Builder(it).create() }

                alertDialog?.setView(dialogView)
                alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog?.show()

                dialogView.btn_m_cancel.setOnClickListener {
                    alertDialog?.dismiss()
                }

                val dv1 = layoutInflater.inflate(R.layout.dialog_question, null)
                val dialog1 = activity?.let { AlertDialog.Builder(it).create() }

                dv1.tv_question.text = "${name}????????? ????????? ??????????????????????"
                dialog1?.setView(dv1)
                dialog1?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                val dv2 = layoutInflater.inflate(R.layout.dialog_question, null)
                val dialog2 = activity?.let { AlertDialog.Builder(it).create() }

                dv2.tv_question.text = "${name}?????? ??????????????????????"
                dialog2?.setView(dv2)
                dialog2?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // ?????? ??????
                dialogView.tv_change_host.setOnClickListener {
                    alertDialog?.dismiss()
                    dialog1?.show()
                }

                dv1.btn_q_cancel.setOnClickListener {
                    dialog1?.dismiss()
                }

                dv1.btn_q_ok.setOnClickListener {
                    dialog1?.dismiss()
                    PatchHostService(this@MemberListFragment).tryPatchHost(clubIdx, idx)
                }

                // ?????? ??????
                dialogView.tv_rm_member.setOnClickListener {
                    alertDialog?.dismiss()
                    dialog2?.show()
                }

                dv2.btn_q_cancel.setOnClickListener {
                    dialog2?.dismiss()
                }

                dv2.btn_q_ok.setOnClickListener {
                    dialog2?.dismiss()
                    PatchMemberService(this@MemberListFragment).tryPatchMember(clubIdx, idx)
                }
            }
        })
    }

    override fun onGetMemberListFailure(message: String) {
        Toast.makeText(activity, "?????? ????????? ???????????? ??? ??????????????????.", Toast.LENGTH_SHORT).show()
    }

    override fun onPatchHostInSuccess() {
        Toast.makeText(activity, "${name}?????? ????????? ???????????????!", Toast.LENGTH_SHORT).show()
        GetMemberListService(this).tryGetMemberList(clubIdx, curDate)
    }

    override fun onPatchHostInFailure(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    override fun onPatchMemberInSuccess() {
        Toast.makeText(activity, "${name}?????? ?????? ???????????????.", Toast.LENGTH_SHORT).show()
        GetMemberListService(this).tryGetMemberList(clubIdx, curDate)
    }

    override fun onPatchMemberInFailure(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}