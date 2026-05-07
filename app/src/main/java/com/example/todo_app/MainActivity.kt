package com.example.todo_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import androidx.appcompat.app.AppCompatDelegate
import android.graphics.Paint

data class Note(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("title") @set:PropertyName("title") var title: String = "",
    @get:PropertyName("desc") @set:PropertyName("desc") var desc: String = "",
    @get:PropertyName("isDone") @set:PropertyName("isDone") var isDone: Boolean = false,
    @get:PropertyName("completedAt") @set:PropertyName("completedAt") var completedAt: Long = 0L
)

class MainActivity : AppCompatActivity() {

    private lateinit var notesRef: DatabaseReference
    private lateinit var noteList: ArrayList<Note>
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load Theme from Prefs before super.onCreate
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("isDarkMode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val titleInput = findViewById<EditText>(R.id.titleInput)
        val descInput = findViewById<EditText>(R.id.descInput)
        val addBtn = findViewById<Button>(R.id.addBtn)
        val listView = findViewById<ListView>(R.id.listView)
        val themeSwitch = findViewById<Switch>(R.id.themeSwitch)
        val historyBtn = findViewById<Button>(R.id.historyBtn)

        themeSwitch.isChecked = isDarkMode

        // Theme Toggle Logic with Persistence
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = prefs.edit()
            editor.putBoolean("isDarkMode", isChecked)
            editor.apply()
            
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        historyBtn.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Firebase — Using the correct region-specific URL
        notesRef = FirebaseDatabase
            .getInstance("https://todoapp-beaef-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("notes")

        noteList = ArrayList()
        adapter = NoteAdapter(this, noteList, notesRef)
        listView.adapter = adapter

        // ADD TODO
        addBtn.setOnClickListener {
            Toast.makeText(this, "Button clicked!", Toast.LENGTH_SHORT).show()

            val title = titleInput.text.toString().trim()
            val desc = descInput.text.toString().trim()

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                val noteId = notesRef.push().key!!
                val note = Note(noteId, title, desc, false, 0L)

                notesRef.child(noteId).setValue(note)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Todo saved successfully!", Toast.LENGTH_SHORT).show()
                            titleInput.text.clear()
                            descInput.text.clear()
                        } else {
                            val error = task.exception?.message ?: "Unknown error"
                            Toast.makeText(this, "Save failed: $error", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // READ & AUTO-CLEANUP
        notesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                noteList.clear()
                val currentTime = System.currentTimeMillis()
                val twentyFourHours = 24 * 60 * 60 * 1000L

                for (snap in snapshot.children) {
                    try {
                        val note = snap.getValue(Note::class.java)
                        if (note != null) {
                            // AUTO-CLEANUP LOGIC: Delete if completed > 24 hours ago
                            if (note.isDone && note.completedAt > 0 && (currentTime - note.completedAt) > twentyFourHours) {
                                notesRef.child(note.id).removeValue()
                            } else if (!note.isDone) {
                                // ONLY ADD TO MAIN LIST IF NOT DONE
                                noteList.add(note)
                            }
                        }
                    } catch (e: Exception) {}
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val note = noteList[position]
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("title", note.title)
            intent.putExtra("desc", note.desc)
            startActivity(intent)
        }
    }
}

class NoteAdapter(
    private val context: android.content.Context,
    private val notes: ArrayList<Note>,
    private val notesRef: DatabaseReference
) : BaseAdapter() {

    override fun getCount(): Int = notes.size
    override fun getItem(position: Int): Any = notes[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_note, parent, false)

        val titleText = view.findViewById<TextView>(R.id.itemTitle)
        val descText = view.findViewById<TextView>(R.id.itemDesc)
        val completeBtn = view.findViewById<ImageButton>(R.id.completeBtn)
        val deleteBtn = view.findViewById<ImageButton>(R.id.deleteBtn)

        val note = notes[position]

        titleText.text = note.title
        descText.text = note.desc

        // Handle Completed Style
        if (note.isDone) {
            titleText.paintFlags = titleText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            titleText.setTextColor(android.graphics.Color.GRAY)
            completeBtn.setImageResource(android.R.drawable.checkbox_on_background)
            completeBtn.setColorFilter(android.graphics.Color.GRAY)
        } else {
            titleText.paintFlags = titleText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            // Removed manual BLACK to allow theme to handle it
            completeBtn.setImageResource(android.R.drawable.checkbox_off_background)
            completeBtn.setColorFilter(android.graphics.Color.parseColor("#4CAF50"))
        }

        // COMPLETE BUTTON CLICK
        completeBtn.setOnClickListener {
            Toast.makeText(context, "Tick Clicked!", Toast.LENGTH_SHORT).show()
            val newStatus = !note.isDone
            val completedTime = if (newStatus) System.currentTimeMillis() else 0L
            
            notesRef.child(note.id).child("isDone").setValue(newStatus)
            notesRef.child(note.id).child("completedAt").setValue(completedTime)
                .addOnSuccessListener {
                    Toast.makeText(context, "Status Updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Update Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // DELETE BUTTON CLICK
        deleteBtn.setOnClickListener {
            Toast.makeText(context, "Delete Clicked!", Toast.LENGTH_SHORT).show()
            notesRef.child(note.id).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(context, "Deleted from Database", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}