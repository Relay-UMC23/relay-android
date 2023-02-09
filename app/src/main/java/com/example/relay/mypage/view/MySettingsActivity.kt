package com.example.relay.mypage.view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.relay.ApplicationClass.Companion.prefs
import com.example.relay.R
import com.example.relay.databinding.ActivityMySettingsBinding
import com.example.relay.login.view.LoginMainActivity
import com.example.relay.mypage.service.MySettingInterface
import com.example.relay.mypage.service.MySettingService
import com.example.relay.mypage.models.ChangeMsgResponse
import com.example.relay.mypage.models.ChangePwdResponse
import kotlinx.android.synthetic.main.dialog_change_msg.view.*
import kotlinx.android.synthetic.main.dialog_change_pw.view.*

class MySettingsActivity : AppCompatActivity(), MySettingInterface {
    private val viewBinding: ActivityMySettingsBinding by lazy{
        ActivityMySettingsBinding.inflate(layoutInflater)
    }

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.btnBack.setOnClickListener {
            finish()
        }

        viewBinding.btnLogout.setOnClickListener {
            // 저장된 계정 내용 초기화
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginMainActivity::class.java)
            finishAffinity()        // 스택에 쌓인 액티비티 비우기
            startActivity(intent)
        }

        // 비밀번호 변경하기
        viewBinding.btnChangePwd.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_change_pw, null)
            val alertDialog = AlertDialog.Builder(this).create()

            alertDialog?.setView(dialogView)
            alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog?.show()

            dialogView.btn_pw_cancel.setOnClickListener {
                alertDialog?.dismiss()
            }

            dialogView.btn_pw_save.setOnClickListener {
                val pw = prefs.getString("pw", "")
                val beforePw = dialogView.et_before_pw.text.toString()
                val newPw = dialogView.et_new_pw.text.toString()
                val checkPw = dialogView.et_check_pw.text.toString()

                if (beforePw != pw) {
                    Toast.makeText(this, "기존 비밀번호를 확인해주세요!", Toast.LENGTH_SHORT).show()
                } else {
                    if (newPw != checkPw) {
                        Toast.makeText(this, "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    } else { // 비밀번호 변경 값 전송
                        MySettingService(this).tryPatchUserPwd(newPw, checkPw)
                        alertDialog?.dismiss()
                    }
                }
            }
        }

        var bundle = intent.extras
        if (bundle != null) {
            val imgUrl = bundle.getString("imgUrl", "")
            val name = bundle.getString("name", "")
            val email = bundle.getString("email", "")
            val statusMsg = bundle.getString("statusMsg", "")
            val isAlarmOn = bundle.getString("isAlarmOn", "")

            Glide.with(viewBinding.imgUser.context)
                .load(imgUrl)
                .into(viewBinding.imgUser)
            viewBinding.tvUserName.text = name
            viewBinding.tvEmail.text = email
            if (!statusMsg.equals("")) {
                viewBinding.tvInfo.text = statusMsg
            }
            viewBinding.swAlarm.isChecked = isAlarmOn.equals("y")

            // 자기소개 변경하기
            viewBinding.btnChangeInfo.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.dialog_change_msg, null)
                val alertDialog = AlertDialog.Builder(this).create()

                alertDialog?.setView(dialogView)
                alertDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog?.show()

                dialogView.et_msg.setText(statusMsg) // 현재 상태메시지
                dialogView.et_msg.setSelection(dialogView.et_msg.length()) // 커서 끝에 설정

                dialogView.btn_msg_cancel.setOnClickListener {
                    alertDialog?.dismiss()
                }

                dialogView.btn_msg_save.setOnClickListener {
                    val msg = dialogView.et_msg.text.toString()
                    MySettingService(this).tryPatchUserMsg(msg)
                    alertDialog?.dismiss()
                }
            }
        }
    }

    override fun onPatchUserMsgSuccess(response: ChangeMsgResponse) {
        Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
    }

    override fun onPatchUserMsgFailure(message: String) {
        Toast.makeText(this, "자기소개 변경 요청에 실패하였습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onPatchUserPwdSuccess(response: ChangePwdResponse) {
        Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
    }

    override fun onPatchUserPwdFailure(message: String) {
        Toast.makeText(this, "비밀번호 변경 요청에 실패하였습니다.", Toast.LENGTH_SHORT).show()
    }
}