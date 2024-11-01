package com.broccolistefanipss.sportstracker.fragment.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.broccolistefanipss.sportstracker.global.DB
import com.broccolistefanipss.sportstracker.model.TrainingSession
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private lateinit var db: DB
    private lateinit var userName: String

    private val _trainingSessions = MutableLiveData<List<TrainingSession>>()
    val trainingSessions: LiveData<List<TrainingSession>> get() = _trainingSessions

    fun initialize(context: Context, userName: String) {
        this.db = DB(context)
        this.userName = userName
        loadTrainingSessionsUser()
    }

    private fun loadTrainingSessionsUser() {
        viewModelScope.launch {
            val sessions = db.getUserTrainingSessions(userName)
            _trainingSessions.postValue(sessions)
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            val success = db.deleteTrainingSession(sessionId)
            if (success) {
                loadTrainingSessionsUser()
            }
        }
    }
}
