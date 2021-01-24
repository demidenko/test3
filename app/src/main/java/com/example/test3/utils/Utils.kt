package com.example.test3.utils

import android.content.Context
import android.text.Html
import android.text.Spanned
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.datastore.preferences.createDataStore
import com.example.test3.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import java.util.*
import java.util.concurrent.TimeUnit


val httpClient = OkHttpClient
    .Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

val jsonCPS = Json{ ignoreUnknownKeys = true }
val jsonConverterFactory = jsonCPS.asConverterFactory(MediaType.get("application/json"))

fun fromHTML(s: String): Spanned {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Html.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY)
    } else {
        Html.fromHtml(s)
    }
}

fun signedToString(x: Int): String = if(x>0) "+$x" else "$x"


class SharedReloadButton(private val button: ImageButton) {
    private val current = TreeSet<String>()

    fun startReload(tag: String) {
        if(current.contains(tag)) throw Exception("$tag already started")
        if(current.isEmpty()) button.isEnabled = false
        current.add(tag);
    }

    fun stopReload(tag: String) {
        if(!current.contains(tag)) throw Exception("$tag not started to be stopped")
        current.remove(tag)
        if(current.isEmpty()) button.isEnabled = true
    }
}

open class SettingsDataStore(context: Context, name: String) {
    protected val dataStore by lazy { context.createDataStore(name = name) }
}


private fun setupDescription(
    view: ConstraintLayout,
    description: String
){
    if(description.isNotBlank()){
        view.findViewById<TextView>(R.id.settings_item_description).apply {
            text = description
            visibility = View.VISIBLE
        }
    }
}

fun setupSwitch(
    view: ConstraintLayout,
    title: String,
    checked: Boolean,
    description: String = "",
    onChangeCallback: (buttonView: CompoundButton, isChecked: Boolean) -> Unit
){
    view.apply {
        findViewById<TextView>(R.id.settings_item_title).text = title
        findViewById<SwitchMaterial>(R.id.settings_switcher_button).apply {
            isChecked = checked
            this.setOnCheckedChangeListener { buttonView, isChecked -> onChangeCallback(buttonView,isChecked) }
        }
        setupDescription(this, description)
    }
}

fun setupSelect(
    view: ConstraintLayout,
    title: String,
    options: Array<CharSequence>,
    initSelected: Int,
    description: String = "",
    onChangeCallback: (buttonView: View, optionSelected: Int) -> Unit
){
    view.apply {
        findViewById<TextView>(R.id.settings_item_title).text = title
        val selectedTextView = findViewById<TextView>(R.id.settings_select_button)
        selectedTextView.text = options[initSelected]
        setOnClickListener {
            var selected = initSelected
            AlertDialog.Builder(context)
                .setTitle(title)
                .setSingleChoiceItems(options, selected){ d, index ->
                    selected = index
                    onChangeCallback(it, index)
                    selectedTextView.text = options[index]
                    d.dismiss()
                }
                .create().show()
        }
        setupDescription(this, description)
    }
}

fun setupMultiSelect(
    view: ConstraintLayout,
    title: String,
    options: Array<CharSequence>,
    initSelected: BooleanArray,
    description: String = "",
    onChangeCallback: (buttonView: View, optionsSelected: BooleanArray) -> Unit
){
    view.apply {
        findViewById<TextView>(R.id.settings_item_title).text = title
        val selectedTextView = findViewById<TextView>(R.id.settings_multiselect_button)
        selectedTextView.text = initSelected.count { it }.toString()
        setOnClickListener {
            val selected = initSelected.clone()
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMultiChoiceItems(options, selected){ _, i, isChecked ->
                    selected[i] = isChecked
                }
                .setPositiveButton("Save"){ d, _ ->
                    onChangeCallback(it, selected.clone())
                    selectedTextView.text = selected.count { it }.toString()
                    d.dismiss()
                }
                .create().show()
        }
        setupDescription(this, description)
    }
}
