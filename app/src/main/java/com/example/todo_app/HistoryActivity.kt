package com.example.todo_app

import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var notesRef: DatabaseReference
    private lateinit var historyList: ArrayList<Note>
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load Theme from Prefs
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("isDarkMode", false)
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val listView = findViewById<ListView>(R.id.historyListView)
        
        notesRef = FirebaseDatabase
            .getInstance("https://todoapp-beaef-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("notes")

        historyList = ArrayList()
        adapter = NoteAdapter(this, historyList, notesRef)
        listView.adapter = adapter

        // Read only completed tasks
        notesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                for (snap in snapshot.children) {
                    val note = snap.getValue(Note::class.java)
                    if (note != null && note.isDone) {
                        historyList.add(note)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HistoryActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })

        // Open details from history
        listView.setOnItemClickListener { _, _, position, _ ->
            val note = historyList[position]
            val intent = android.content.Intent(this, DetailActivity::class.java)
            intent.putExtra("title", note.title)
            intent.putExtra("desc", note.desc)
            startActivity(intent)
        }
    }
}
