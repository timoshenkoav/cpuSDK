package com.tunebrains.cpu.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_log.*
import kotlinx.android.synthetic.main.log_line.view.*
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit


class FileActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        val app = application as App
        val text = app.logFileTree.logFile.readLines(Charset.defaultCharset())

        adapter = Adapter(this)
        content.layoutManager = LinearLayoutManager(this)
        content.adapter = adapter
        adapter.update(text)
        compositeDisposable.add(
            Observable.interval(
                5,
                TimeUnit.SECONDS
            ).observeOn(AndroidSchedulers.mainThread()).subscribe {
                val text = app.logFileTree.logFile.readLines(Charset.defaultCharset())
                adapter.update(text)
            })
    }

    class VH(view: View) : RecyclerView.ViewHolder(view)
    class Adapter(ctx: Context) : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(layoutInflater.inflate(R.layout.log_line, parent, false))
        }

        override fun getItemCount(): Int {
            return lines.size
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.itemView.text.text = lines[position]
        }

        fun update(items: List<String>) {
            lines.clear()
            lines.addAll(items)
            notifyDataSetChanged()
        }

        private val lines = mutableListOf<String>()
        private val layoutInflater = LayoutInflater.from(ctx)

    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.log, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share -> {
                val contentUri = FileProvider.getUriForFile(
                    this,
                    "$packageName.file.provider",
                    (application as App).logFileTree.logFile
                )
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                shareIntent.type = "text/plain"
                startActivity(Intent.createChooser(shareIntent, "Share"))
                true
            }
            else ->
                super.onOptionsItemSelected(item)
        }

    }
}