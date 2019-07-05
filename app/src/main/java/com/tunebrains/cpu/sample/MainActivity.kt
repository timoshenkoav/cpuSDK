package com.tunebrains.cpu.sample

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import com.tunebrains.cpu.library.cmd.DbHelper
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.io.FilenameFilter


class MainActivity : AppCompatActivity() {

    companion object {
        const val FILE_REQUEST_CODE = 0x100
    }

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
            if (dexFile.text.toString().isNotEmpty()) {
                DbHelper.insertCommand(this, 1, dexFile.text.toString()).subscribe {
                    Timber.d("Command inserted")
                }
            } else {
                showMessage("Select dex file")
            }
        }
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
