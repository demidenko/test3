package com.example.test3

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.*
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import kotlinx.coroutines.launch


abstract class AccountPanel(
    val activity: MainActivity,
    val manager: AccountManager
){
    val layout = LayoutInflater.from(activity).inflate(R.layout.account_panel, null, false) as RelativeLayout
    val textMain: TextView = layout.findViewById(R.id.account_panel_textMain)
    val textAdditional: TextView = layout.findViewById(R.id.account_panel_textAdditional)
    val expandButton: ImageButton = layout.findViewById<ImageButton>(R.id.account_panel_expand_button).apply {
        setOnClickListener {
            activity.supportFragmentManager.beginTransaction()
                .hide(activity.accountsFragment)
                .add(android.R.id.content, AccountEditFragment().apply {
                    arguments = Bundle().apply { putString("manager", manager.PREFERENCES_FILE_NAME) }
                })
                .addToBackStack(null)
                .commit()
        }
    }
    val reloadButton = layout.findViewById<ImageButton>(R.id.account_panel_reload_button).apply {
        setOnClickListener {
            activity.scope.launch { reload() }
        }
    }


    fun buildAndAdd(textMainSize: Float, textAdditionalSize: Float, view: View){

        textMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, textMainSize)
        textAdditional.setTextSize(TypedValue.COMPLEX_UNIT_SP, textAdditionalSize)

        view.findViewById<LinearLayout>(R.id.panels_layout).addView(layout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            val pix = 30
            setMargins(pix, pix, pix, 0)
        })

        additionalBuild()

        layout.setOnClickListener {
            println("info = " + manager.savedInfo)
            val startDelay = 3000L
            val duration = 2000L
            listOf(reloadButton, expandButton).forEach { button ->
                if(button.isEnabled) {
                    button.clearAnimation()
                    button.animate().setStartDelay(0).setDuration(0).alpha(1f).withEndAction {
                        button.visibility = View.VISIBLE
                        button.animate().setStartDelay(startDelay).setDuration(duration).alpha(0f)
                            .withEndAction {
                                button.visibility = View.GONE
                            }.start()
                    }.start()
                }
            }
        }


    }

    open fun additionalBuild(){ }

    fun isEmpty() = manager.savedInfo.userID.isBlank()

    protected abstract fun show(info: UserInfo)

    fun show(){
        if(isEmpty()){
            layout.visibility = View.GONE
            return
        }
        layout.visibility = View.VISIBLE
        show(manager.savedInfo)
    }

    suspend fun reload(){
        if(isEmpty()) return

        expandButton.isEnabled = false
        reloadButton.isEnabled = false
        activity.accountsFragment.sharedReloadButton.startReload(manager.PREFERENCES_FILE_NAME)


        expandButton.animate().setStartDelay(0).alpha(0f).setDuration(0).withEndAction {
            expandButton.visibility = View.GONE
        }.start()

        reloadButton.animate().setStartDelay(0).alpha(1f).setDuration(0).withStartAction {
            reloadButton.setColorFilter(activity.defaultTextColor)
            reloadButton.visibility = View.VISIBLE
        }.start()
        reloadButton.startAnimation(rotateAnimation)

        //textAdditional.text = "..."
        val savedInfo = manager.savedInfo
        val info = manager.loadInfo(savedInfo.userID)

        if(info.status != STATUS.FAILED){
            manager.savedInfo = info
            show(info)
            reloadButton.animate().setStartDelay(0).setDuration(1000).alpha(0f).withEndAction {
                reloadButton.clearAnimation()
                reloadButton.visibility = View.GONE
            }.start()
        }else{
            show(savedInfo)
            Toast.makeText(activity, "${manager.PREFERENCES_FILE_NAME} load error", Toast.LENGTH_LONG).show()
            reloadButton.clearAnimation()
            reloadButton.setColorFilter(activity.resources.getColor(R.color.reload_fail, null))
        }


        activity.accountsFragment.sharedReloadButton.stopReload(manager.PREFERENCES_FILE_NAME)
        reloadButton.isEnabled = true
        expandButton.isEnabled = true
    }
}

val rotateAnimation = RotateAnimation(
    0f,
    360f,
    Animation.RELATIVE_TO_SELF,
    0.5f,
    Animation.RELATIVE_TO_SELF,
    0.5f
).apply {
    duration = 1000
    repeatCount = Animation.INFINITE
    repeatMode = Animation.INFINITE
    interpolator = LinearInterpolator()
}