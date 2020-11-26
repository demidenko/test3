package com.example.test3

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.*
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.utils.SharedReloadButton
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.navigation_accounts.*
import kotlinx.coroutines.launch

class AccountsFragment: Fragment() {

    lateinit var codeforcesAccountManager: CodeforcesAccountManager
    lateinit var atcoderAccountManager: AtCoderAccountManager
    lateinit var topcoderAccountManager: TopCoderAccountManager
    lateinit var acmpAccountManager: ACMPAccountManager
    lateinit var timusAccountManager: TimusAccountManager

    lateinit var codeforcesPanel: AccountPanel
    lateinit var atcoderPanel: AccountPanel
    lateinit var topcoderPanel: AccountPanel
    lateinit var acmpPanel: AccountPanel
    lateinit var timusPanel: AccountPanel
    private val panels: List<AccountPanel> by lazy {
        listOf(
            codeforcesPanel,
            atcoderPanel,
            topcoderPanel,
            acmpPanel,
            timusPanel
        )
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        println("fragment accounts onCreateView $savedInstanceState")
        val view = inflater.inflate(R.layout.fragment_accounts, container, false)
        val activity = requireActivity() as MainActivity


        codeforcesAccountManager = CodeforcesAccountManager(activity)
        codeforcesPanel = object : AccountPanel(activity, codeforcesAccountManager){
            override fun show(info: UserInfo) { info as CodeforcesAccountManager.CodeforcesUserInfo
                val color = manager.getColor(info)
                textMain.text = CodeforcesUtils.makeSpan(info)
                textAdditional.text = ""
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
                if(info.status == STATUS.OK){
                    textMain.typeface = Typeface.DEFAULT_BOLD
                    textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
                }else{
                    textMain.typeface = Typeface.DEFAULT
                }
                activity.window.statusBarColor = color ?: Color.TRANSPARENT
            }
        }

        atcoderAccountManager = AtCoderAccountManager(activity)
        atcoderPanel = object : AccountPanel(activity, atcoderAccountManager){
            override fun show(info: UserInfo) { info as AtCoderAccountManager.AtCoderUserInfo
                val color = manager.getColor(info)
                textMain.text = info.handle
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.text = ""
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
                if(info.status == STATUS.OK){
                    textMain.typeface = Typeface.DEFAULT_BOLD
                    textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
                }else{
                    textMain.typeface = Typeface.DEFAULT
                }
            }
        }

        topcoderAccountManager = TopCoderAccountManager(activity)
        topcoderPanel = object : AccountPanel(activity, topcoderAccountManager){
            override fun show(info: UserInfo) { info as TopCoderAccountManager.TopCoderUserInfo
                val color = manager.getColor(info)
                textMain.text = info.handle
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.text = ""
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
                if(info.status == STATUS.OK){
                    textMain.typeface = Typeface.DEFAULT_BOLD
                    textAdditional.text = if(info.rating_algorithm == NOT_RATED) "[not rated]" else "${info.rating_algorithm}"
                }else{
                    textMain.typeface = Typeface.DEFAULT
                }
            }
        }

        acmpAccountManager = ACMPAccountManager(activity)
        acmpPanel = object : AccountPanel(activity, acmpAccountManager){
            override fun show(info: UserInfo) { info as ACMPAccountManager.ACMPUserInfo
                with(info){
                    if (status == STATUS.OK) {
                        textMain.text = userName
                        textAdditional.text = "Solved: $solvedTasks  Rank: $place  Rating: $rating"
                    }else{
                        textMain.text = id
                        textAdditional.text = ""
                    }
                }
                textMain.setTextColor(activity.defaultTextColor)
                textAdditional.setTextColor(activity.defaultTextColor)
            }
        }

        timusAccountManager = TimusAccountManager(activity)
        timusPanel = object : AccountPanel(activity, timusAccountManager){
            override fun show(info: UserInfo) { info as TimusAccountManager.TimusUserInfo
                with(info){
                    if (status == STATUS.OK) {
                        textMain.text = userName
                        textAdditional.text = "Solved: $solvedTasks  Rank: $placeTasks"
                    }else{
                        textMain.text = id
                        textAdditional.text = ""
                    }
                }
                textMain.setTextColor(activity.defaultTextColor)
                textAdditional.setTextColor(activity.defaultTextColor)
            }
        }


        if(panels.map { it.manager.PREFERENCES_FILE_NAME }.distinct().size != panels.size) throw Exception("not different file names in panels managers")

        codeforcesPanel.buildAndAdd(30F, 25F, view)
        atcoderPanel.buildAndAdd(30F, 25F, view)
        topcoderPanel.buildAndAdd(30F, 25F, view)
        acmpPanel.buildAndAdd(17F, 14F, view)
        timusPanel.buildAndAdd(17F, 14F, view)




        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("fragment accounts onViewCreated "+savedInstanceState)
        super.onViewCreated(view, savedInstanceState)

        panels.forEach { it.show() }

        with(requireActivity() as MainActivity){
            navigation_accounts_reload.setOnClickListener { reloadAccounts() }
            navigation_accounts_add.setOnClickListener { addAccount() }
        }
    }

    fun addAccount() {
        val emptyPanels = panels.filter { it.isEmpty() }

        if(emptyPanels.isEmpty()) Toast.makeText(requireActivity(), "Nothing to add", Toast.LENGTH_SHORT).show()
        else{
            val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_item)
            emptyPanels.forEach {
                adapter.add(it.manager.PREFERENCES_FILE_NAME)
            }

            AlertDialog.Builder(activity)
                .setTitle("Add account")
                .setAdapter(adapter) { _, index ->
                    emptyPanels[index].expandButton.callOnClick()
                }.create().show()
        }
    }

    fun showAccounts() {
        panels.forEach { it.show() }
    }

    fun reloadAccounts() {
        (requireActivity() as MainActivity).scope.launch {
            panels.forEach {
                launch { it.reload() }
            }
        }
    }

    fun getPanel(managerType: String): AccountPanel {
        return panels.find { panel ->
            panel.manager.PREFERENCES_FILE_NAME == managerType
        } ?: throw Exception("Unknown type of manager: $managerType")
    }

    val sharedReloadButton by lazy { SharedReloadButton(requireActivity().navigation_accounts_reload) }

    override fun onHiddenChanged(hidden: Boolean) {
        if(!hidden){
            with(requireActivity() as MainActivity){
                navigation.visibility = View.VISIBLE
            }
        }
        super.onHiddenChanged(hidden)
    }

    override fun onResume() {
        super.onResume()
        println("AccountsFragment onResume")
    }

    override fun onDestroyView() {
        println("fragment accounts onDestroyView")
        super.onDestroyView()
    }

}