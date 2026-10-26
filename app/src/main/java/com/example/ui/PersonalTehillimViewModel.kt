package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.tehillim.PersonalTehillimState
import com.example.data.tehillim.PersonalTehillimStore
import com.example.domain.tehillim.PersonalTehillimEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class PersonalTehillimViewModel(app: Application) : AndroidViewModel(app) {
    private val store = PersonalTehillimStore(app)
    val state = store.state.stateIn(viewModelScope, SharingStarted.Eagerly, PersonalTehillimState())
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private fun write(action: suspend () -> Unit) { viewModelScope.launch {
        try { action(); mutableError.value = null }
        catch (e: java.io.IOException) { mutableError.value = "השינוי לא נשמר. נסו שוב." }
    } }
    fun save(entry: PersonalTehillimEntry) = write { store.save(entry) }
    fun delete(id: String) = write { store.delete(id) }
    fun move(id: String, delta: Int) = write { store.move(id, delta) }
    fun complete(entry: PersonalTehillimEntry, date: LocalDate, checked: Boolean) = write { store.complete(entry, date, checked) }
}
