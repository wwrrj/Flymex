package com.flymex.sample

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.flymex.theme.FlymeAccent
import com.flymex.theme.FlymeTheme
import com.flymex.widget.FlymeActionSheet
import com.flymex.widget.FlymeCell
import com.flymex.widget.FlymeCircleProgressBar
import com.flymex.widget.FlymeCircularProgressButton
import com.flymex.widget.FlymeDialog
import com.flymex.widget.FlymePageIndicator
import com.flymex.widget.FlymePopupMenu
import com.flymex.widget.FlymeSearchBar
import com.flymex.widget.FlymeSeekBar
import com.flymex.widget.FlymeTabBar
import com.flymex.widget.FlymeToast

class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("flymex_sample", Context.MODE_PRIVATE) }
    private val accentNames = mapOf(
        FlymeAccent.BLUE to "蓝色", FlymeAccent.GREEN to "绿色", FlymeAccent.ORANGE to "橙色", FlymeAccent.PURPLE to "紫色",
        FlymeAccent.RED to "红色", FlymeAccent.YELLOW to "黄色", FlymeAccent.CYAN to "青色", FlymeAccent.POLESTAR to "极星橙",
    )

    private fun currentAccent(): FlymeAccent =
        FlymeAccent.values().getOrElse(prefs.getInt("accent", 0)) { FlymeAccent.BLUE }

    override fun onCreate(savedInstanceState: Bundle?) {
        FlymeTheme.apply(this, currentAccent())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tabBar = findViewById<FlymeTabBar>(R.id.tabBar)
        tabBar.setOnTabSelectedListener { index, _ -> FlymeToast.show(this, "Tab ${index + 1}") }

        findViewById<FlymeSearchBar>(R.id.searchBar).apply {
            onSearch = { FlymeToast.show(this@MainActivity, "搜索：$it") }
        }

        // ---- 外观
        val night = findViewById<FlymeCell>(R.id.cellNight)
        night.isChecked = FlymeTheme.isNight(this)
        night.onCheckedChange = { _, on ->
            AppCompatDelegate.setDefaultNightMode(if (on) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }
        val accentCell = findViewById<FlymeCell>(R.id.cellAccent)
        accentCell.value = accentNames[currentAccent()]
        accentCell.setOnClickListener {
            val names = accentNames.values.toTypedArray<CharSequence>()
            FlymeDialog.Builder(this)
                .setTitle("主题色")
                .setSingleChoiceItems(names, currentAccent().ordinal) { d, which ->
                    prefs.edit().putInt("accent", which).apply()
                    d.dismiss()
                    recreate()
                }
                .setNegativeButton("取消")
                .show()
        }

        // ---- 滑杆与进度
        val circle = findViewById<FlymeCircleProgressBar>(R.id.circleProgress)
        findViewById<FlymeSeekBar>(R.id.seekBar).setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                circle.progress = progress
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })
        val cir = findViewById<FlymeCircularProgressButton>(R.id.cirButton)
        cir.setOnClickListener {
            when (cir.state) {
                FlymeCircularProgressButton.State.IDLE, FlymeCircularProgressButton.State.ERROR -> {
                    cir.state = FlymeCircularProgressButton.State.PROGRESS
                    cir.progress = 0
                    val tick = object : Runnable {
                        override fun run() {
                            if (cir.state != FlymeCircularProgressButton.State.PROGRESS) return
                            cir.progress += 4
                            if (cir.progress >= 100) cir.state = FlymeCircularProgressButton.State.COMPLETE else cir.postDelayed(this, 60)
                        }
                    }
                    cir.postDelayed(tick, 60)
                }
                FlymeCircularProgressButton.State.PROGRESS -> cir.state = FlymeCircularProgressButton.State.IDLE
                FlymeCircularProgressButton.State.COMPLETE -> FlymeToast.complete(this, "已打开")
            }
        }

        // ---- 弹层
        findViewById<FlymeCell>(R.id.cellDialog).setOnClickListener {
            FlymeDialog.Builder(this)
                .setTitle("清除应用数据？")
                .setMessage("将删除该应用的全部数据，包括文件、设置、账户、数据库等，此操作不可撤销。")
                .setNegativeButton("取消")
                .setPositiveButton("清除") { FlymeToast.show(this, "已清除") }
                .setDestructive(true)
                .show()
        }
        findViewById<FlymeCell>(R.id.cellDialogList).setOnClickListener {
            val items = arrayOf<CharSequence>("自动", "60 Hz", "120 Hz", "动态刷新")
            FlymeDialog.Builder(this)
                .setTitle("屏幕刷新率")
                .setSingleChoiceItems(items, 2) { d, which ->
                    FlymeToast.show(this, items[which])
                    d.dismiss()
                }
                .show()
        }
        findViewById<FlymeCell>(R.id.cellSheet).setOnClickListener {
            FlymeActionSheet.Builder(this)
                .setTitle("分享到")
                .addItem("微信") { FlymeToast.show(this, "微信") }
                .addItem("QQ") { FlymeToast.show(this, "QQ") }
                .addItem("复制链接") { FlymeToast.show(this, "已复制") }
                .addItem("删除", destructive = true) { FlymeToast.show(this, "已删除") }
                .show()
        }
        findViewById<FlymeCell>(R.id.cellToast).setOnClickListener { FlymeToast.show(this, "这是一条 Flyme 风格的 Toast") }
        findViewById<FlymeCell>(R.id.cellCompleteToast).setOnClickListener { FlymeToast.complete(this, "已添加到桌面") }
        findViewById<FlymeCell>(R.id.cellPopup).setOnClickListener { v ->
            FlymePopupMenu(v)
                .addItem("刷新") { FlymeToast.show(this, "刷新") }
                .addItem("排序方式") { FlymeToast.show(this, "排序") }
                .addItem("设置") { FlymeToast.show(this, "设置") }
                .show()
        }

        // ---- 其他
        val indicator = findViewById<FlymePageIndicator>(R.id.pageIndicator)
        indicator.useOnSurfaceColors()
        findViewById<FlymeTabBar>(R.id.capsuleTabs).setOnTabSelectedListener { index, _ -> indicator.current = index }

        findViewById<com.flymex.widget.FlymeAppBar>(R.id.appBar).addAction(com.flymex.R.drawable.mz_ic_sb_more, "更多") { v ->
            FlymePopupMenu(v).addItem("关于 Flymex") { Toast.makeText(this, "Flymex 0.1.0 · Flyme 12.6 tokens", Toast.LENGTH_SHORT).show() }.show()
        }
    }
}
