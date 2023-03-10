package com.example.relay.group.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.relay.databinding.ItemRvGroupMemberBinding
import com.example.relay.group.models.Member

class GroupMemberRVAdapter(private val dataList: ArrayList<Member>, private val hostIdx: Long, private val userIdx: Long): RecyclerView.Adapter<GroupMemberRVAdapter.DataViewHolder>() {

    // 아이템 클릭 인터페이스
    interface ItemClickListener {
        fun onMemberClick(view: View, position: Int)
        fun onSettingClick(view: View, position: Int)
    }

    // 아이템 클릭 리스너
    private lateinit var itemClickListner: ItemClickListener

    // 아이템 클릭 리스너 등록
    fun setItemClickListener(itemClickListener: ItemClickListener) {
        this.itemClickListner = itemClickListener
    }

    // ViewHolder 객체
    inner class DataViewHolder(val binding: ItemRvGroupMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Member, hostIdx: Long, userIdx: Long) {
            binding.memberName.text = data.profile.nickname
            binding.memberIntro.text = data.profile.statusMsg

            Glide.with(binding.memberImg.context)
                .load(data.profile.imgUrl)
                .into(binding.memberImg)

            if (data.profile.userIdx == hostIdx) {
                binding.icClubHost.visibility = View.VISIBLE
            } else {
                binding.icClubHost.visibility = View.INVISIBLE
            }

            if (userIdx == hostIdx) {
                if (userIdx == data.profile.userIdx) {
                    binding.btnSetting.visibility = View.INVISIBLE
                } else {
                    binding.btnSetting.visibility = View.VISIBLE
                }
            } else {
                binding.btnSetting.visibility = View.INVISIBLE
            }

        }
    }

    // ViewHolder 만들어질 때 실행할 동작
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val binding =
            ItemRvGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DataViewHolder(binding)
    }

    // ViewHolder가 실제로 데이터를 표시해야 할 때 호출되는 함수
    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        holder.bind(dataList[position], hostIdx, userIdx)

        // > 버튼 클릭 리스너
        holder.binding.btnRight.setOnClickListener {
            itemClickListner.onMemberClick(it, position)
        }

        // 설정 버튼
        holder.binding.btnSetting.setOnClickListener {
            itemClickListner.onSettingClick(it, position)
        }
    }

    // 표현할 Item의 총 개수
    override fun getItemCount(): Int = dataList.size
}