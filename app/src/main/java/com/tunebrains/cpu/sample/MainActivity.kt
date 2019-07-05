package com.tunebrains.cpu.sample

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.tbruyelle.rxpermissions2.RxPermissions
import com.tunebrains.cpu.library.cmd.DbHelper
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_add_param.view.*
import timber.log.Timber
import java.io.FilenameFilter
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val FILE_REQUEST_CODE = 0x100
    }

    private val params = mutableMapOf<String, Any>()

    private val dbHelper = DbHelper(this, Gson())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rxPermissions = RxPermissions(this)
        pickDex.setOnClickListener {
            rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe {
                if (it) {
                    listDex()
                }
            }
        }
        btnExecute.setOnClickListener {
            if (dexFile.text.toString().isNotEmpty() && edCommandClass.text.isNotEmpty()) {
                dbHelper.insertCommand(
                    UUID.randomUUID().toString(),
                    dexFile.text.toString(),
                    edCommandClass.text.toString(),
                    params
                )
                    .subscribe {
                        Timber.d("Command inserted")
                    }
            } else {
                showMessage("Select dex file and fill class name to run")
            }
        }
        addParam.setOnClickListener {
            AlertDialog.Builder(this).apply {
                val root = LayoutInflater.from(this@MainActivity).inflate(R.layout.view_add_param, null)
                setView(root)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    params[root.edKey.text.toString()] = root.edValue.text.toString()
                    updateParams()

                }
            }.show()
        }
        btnClear.setOnClickListener {
            params.clear()
            updateParams()
        }
        btnExecuteId.setOnClickListener {
            if (edCmdId.text.isNotEmpty()) {
                dbHelper.insertCommand(edCmdId.text.toString()).subscribe({
                    Timber.d("Command created")
                }, {
                    Timber.e(it)
                })
            } else {
                showMessage("Enter command id")
            }
        }
    }

    private fun updateParams() {
        commandParams.text = Html.fromHtml(params.map { "<b>${it.key}</b>: ${it.value}" }.joinToString("\n"))
    }

    private fun showMessage(mes: String) {
        Toast.makeText(this, mes, Toast.LENGTH_LONG).show()
    }

    private fun listDex() {
        val f = Environment.getExternalStorageDirectory()
        val files = f.listFiles(FilenameFilter { dir, name ->
            name.contains(".dex")
        })
        if (files.isNotEmpty()) {
            val items = files.map {
                it.name as CharSequence
            }
            AlertDialog.Builder(this).apply {
                setItems(items.toTypedArray()) { _, which ->
                    dexFile.text = files[which].path
                }
            }.show()
        } else {
            AlertDialog.Builder(this).apply {
                setMessage("No *.dex file found on sdcard")
                setPositiveButton(android.R.string.ok) { _, _ ->

                }
            }.show()
        }
    }
}
