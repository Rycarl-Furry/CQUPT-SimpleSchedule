package com.example.myapplication.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainActivity
import com.example.myapplication.databinding.FragmentNoticeBinding
import com.example.myapplication.model.Notice
import com.example.myapplication.network.NetworkService
import kotlinx.coroutines.launch

class NoticeFragment : Fragment() {

    private var _binding: FragmentNoticeBinding? = null
    private val binding get() = _binding!!
    
    private val networkService = NetworkService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnBack.setOnClickListener {
            navigateBack()
        }
        
        loadNotices()
    }

    private fun loadNotices() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        binding.noticeContainer.removeAllViews()
        
        lifecycleScope.launch {
            val result = networkService.fetchNotices()
            binding.progressBar.visibility = View.GONE
            
            result.fold(
                onSuccess = { notices ->
                    if (notices.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        displayNotices(notices)
                    }
                },
                onFailure = { error ->
                    Toast.makeText(
                        requireContext(),
                        "加载失败: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.emptyView.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun displayNotices(notices: List<Notice>) {
        notices.forEach { notice ->
            val noticeView = createNoticeView(notice)
            binding.noticeContainer.addView(noticeView)
        }
    }

    private fun createNoticeView(notice: Notice): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(android.graphics.Color.parseColor("#FFFFFF"))
                setStroke(1, android.graphics.Color.parseColor("#E8E8E8"))
            }
            background = drawable
            setOnClickListener {
                openNoticeUrl(notice.url)
            }
            
            val titleView = TextView(context).apply {
                text = notice.title
                textSize = 16f
                setTextColor(android.graphics.Color.parseColor("#333333"))
                setPadding(0, 0, 0, dpToPx(8))
            }
            addView(titleView)
            
            val dateView = TextView(context).apply {
                text = notice.date
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#888888"))
                setPadding(0, 0, 0, dpToPx(8))
            }
            addView(dateView)
        }
    }

    private fun openNoticeUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun navigateBack() {
        parentFragmentManager.popBackStack()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): NoticeFragment {
            return NoticeFragment()
        }
    }
}
