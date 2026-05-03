package com.example.todo_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

// 🔹 Data Model
data class Note(
    val title: String,
    val desc: String
)

// 🔹 MAIN ACTIVITY
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val titleInput = findViewById<EditText>(R.id.titleInput)
        val descInput = findViewById<EditText>(R.id.descInput)
        val addBtn = findViewById<Button>(R.id.addBtn)
        val listView = findViewById<ListView>(R.id.listView)

        val noteList = ArrayList<Note>()
        val adapter = NoteAdapter(this, noteList)
        listView.adapter = adapter

        addBtn.setOnClickListener {
            val title = titleInput.text.toString()
            val desc = descInput.text.toString()

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                noteList.add(Note(title, desc))
                adapter.notifyDataSetChanged()

                titleInput.text.clear()
                descInput.text.clear()
            }
        }

        // 🔹 Click → open detail screen
        listView.setOnItemClickListener { _, _, position, _ ->
            val note = noteList[position]

            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("title", note.title)
            intent.putExtra("desc", note.desc)

            startActivity(intent)
        }
    }
}

//////////////////////////////////////////////////////////

// 🔹 ADAPTER (Only Title)
class NoteAdapter(
    private val context: android.content.Context,
    private val notes: ArrayList<Note>
) : BaseAdapter() {

    override fun getCount(): Int = notes.size

    override fun getItem(position: Int): Any = notes[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        val titleText = view.findViewById<TextView>(android.R.id.text1)

        val note = notes[position]
        titleText.text = note.title

        return view
    }
}

//////////////////////////////////////////////////////////

// 🔹 SECOND SCREEN (INSIDE SAME FILE)
class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val title = intent.getStringExtra("title")
        val desc = intent.getStringExtra("desc")

        val titleView = findViewById<TextView>(R.id.titleView)
        val descView = findViewById<TextView>(R.id.descView)

        titleView.text = title
        descView.text = desc
    }
}