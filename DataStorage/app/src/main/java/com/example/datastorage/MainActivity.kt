package com.example.datastorage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.datastorage.model.Contact
import com.example.datastorage.model.Note
import com.example.datastorage.repository.ContactRepository
import com.example.datastorage.repository.NoteRepository
import com.example.datastorage.ui.theme.DataStorageTheme

import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    private lateinit var contactRepository: ContactRepository
    private lateinit var noteRepository: NoteRepository

    private var contacts by mutableStateOf(emptyList<Contact>())
    private var notes by mutableStateOf(emptyList<Note>())

    private var selectedContactId by mutableStateOf(0L)
    private var isEditDialogVisible by mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactRepository = ContactRepository(this)
        noteRepository = NoteRepository(this)

        importContacts()
        importNotes()

        setContent {
            DataStorageTheme(content = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(context = this)
                }
            }
            )
        }
        // Check and request contacts permission at runtime
        checkAndRequestContactsPermission()
    }

    private var isContactsImported by mutableStateOf(false)
    private var isNotesImported by mutableStateOf(false)

    private fun importContacts() {
        if (!isContactsImported) {
            lifecycleScope.launch {
                contactRepository.importContacts()
                contacts = contactRepository.getAllContacts()
            }
            isContactsImported = true
        }
    }

    private fun importNotes(){
        if(!isNotesImported){
            lifecycleScope.launch {
                notes = noteRepository.getAllNotes()
            }
            isNotesImported = true;
        }
    }

    private val READ_CONTACTS_PERMISSION_REQUEST_CODE = 1001
    private val WRITE_CONTACTS_PERMISSION_REQUEST_CODE = 1002

    private fun checkAndRequestContactsPermission() {
        // Check if the READ_CONTACTS permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the READ_CONTACTS permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                READ_CONTACTS_PERMISSION_REQUEST_CODE
            )
        } else {
            // Check if the WRITE_CONTACTS permission is granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the WRITE_CONTACTS permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_CONTACTS),
                    WRITE_CONTACTS_PERMISSION_REQUEST_CODE
                )
            } else {
                // Both permissions are granted, proceed with accessing the contacts provider
                importContacts()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_CONTACTS_PERMISSION_REQUEST_CODE,
            WRITE_CONTACTS_PERMISSION_REQUEST_CODE -> {
                // Check if the permission is granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with accessing the contacts provider
                    importContacts()
                } else {
                    // Permission denied, handle accordingly (e.g., show a message to the user)
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }


    @Composable
    private fun MainScreen(context: Context) {
        Text(
            text = "Contacts",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Column {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f) // Use weight to allow LazyColumn to take the remaining space
            ) {
                item {
                    // This is a placeholder item that acts as a spacer
                    Spacer(modifier = Modifier.height(50.dp))
                }
                items(contacts.size) { index ->
                    val contact = contacts[index]
                    val note = notes.find { note -> note.contactId == contact.id }
                    ContactItem(
                        contact = contact,
                        note = note,
                        onEditClick = {
                            selectedContactId = contact.id
                            isEditDialogVisible = true
                        },
                        onDeleteClick = {
                            lifecycleScope.launch {
                                val note = noteRepository.getNoteByContactId(contact.id)
                                if(note != null){
                                    noteRepository.deleteNote(note)
                                }
                                notes=noteRepository.getAllNotes()
                            }
                        }
                    )
                }
            }

            if (isEditDialogVisible) {
                val note = notes.find { note -> note.contactId == selectedContactId }
                EditContactDialog(
                    oldNote = note?.description ?: "none",
                    onDismiss = {
                        isEditDialogVisible = false
                    },
                    onSave = {newNote: String ->
                        lifecycleScope.launch {
                            val idNote = noteRepository.getNoteIdByContactId(selectedContactId)
                            if(idNote != null){
                                val editedNote = Note(
                                    id = idNote,
                                    description = newNote,
                                    contactId = selectedContactId
                                )
                                noteRepository.updateNote(editedNote)
                            }
                            else{
                                val note = Note(
                                    description = newNote,
                                    contactId = selectedContactId
                                )
                                noteRepository.insertNote(note)
                            }
                            notes = noteRepository.getAllNotes();
                        }
                        isEditDialogVisible = false
                    }
                )
            }
        }
    }


    @Composable
    private fun ContactItem(
        contact: Contact,
        note: Note?,
        onEditClick: () -> Unit,
        onDeleteClick: () -> Unit
    ) {

        val desc: String = note?.description ?: stringResource(R.string.empty_note_text)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Name: ${contact.name}\nPhone: ${contact.phoneNumber}\nNote: ${desc}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                ContactActionButton(Icons.Default.Edit) {
                    onEditClick()
                }
                ContactActionButton(Icons.Default.Delete) {
                    onDeleteClick()
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }


    @Composable
    private fun ContactActionButton(icon: ImageVector, onClick: () -> Unit) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditContactDialog(
        oldNote: String,
        onDismiss: () -> Unit,
        onSave: (newNote: String) -> Unit

    ) {
        var newNote by remember { mutableStateOf(TextFieldValue(oldNote)) }

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(stringResource(R.string.edit_note_dialog_label)) },
            text = {
                Column {
                    TextField(
                        value = newNote,
                        onValueChange = { newNote = it },
                        label = { Text(stringResource(R.string.new_note_dialog_label)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(newNote.text)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(R.string.save_btn_label))
                }
            },
            dismissButton = {
                Button(
                    onClick = { onDismiss() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(R.string.cancel_btn_label))
                }
            }
        )
    }
}