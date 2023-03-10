package com.example.relay.login.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.relay.ApplicationClass.Companion.prefs
import com.example.relay.databinding.ActivitySignupBinding
import com.example.relay.login.service.LogInSNSInterface
import com.example.relay.login.service.LogInSnsService

class SignupActivity : AppCompatActivity(), LogInSNSInterface {
    private val viewBinding: ActivitySignupBinding by lazy {
        ActivitySignupBinding.inflate(layoutInflater)
    }

    private lateinit var getResultText: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        // 회원가입 실패
        getResultText = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val mString = result.data?.getStringExtra("sign-up-fail")
                Toast.makeText(this, mString, Toast.LENGTH_SHORT).show()
            }
        }

        // 개인정보이용동의 약관 액티비티로 전송 ( or sharedPreferences 사용 )
        viewBinding.btnNext.setOnClickListener{
            val name = viewBinding.etName.text.toString()
            val email = viewBinding.etEmail.text.toString()
            val pw = viewBinding.etPw.text.toString()
            val emailPattern = android.util.Patterns.EMAIL_ADDRESS;

            if (name.isBlank() || email.isBlank() || pw.isBlank() || viewBinding.etCheckPw.text.toString().isBlank())
                Toast.makeText(this, "입력되지 않은 칸이 존재합니다.", Toast.LENGTH_SHORT).show()
            else if (!emailPattern.matcher(email).matches())
                Toast.makeText(this, "이메일 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            else if (pw.length < 8)
                Toast.makeText(this, "비밀번호는 8글자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
            else if (pw != viewBinding.etCheckPw.text.toString()){
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, PrivacyConditionActivity::class.java)
                val editor = prefs.edit()

                editor.putString("name", name)
                editor.putString("email", email)
                editor.putString("pw", pw)
                editor.apply()

                // startActivity(intent)
                getResultText.launch(intent)
            }
        }

        viewBinding.btnSnsKakao.setOnClickListener{
            // 카카오와 연결, 서버 연동x
            LogInSnsService(this).tryKakaoLogIn(this)
        }
    }
}