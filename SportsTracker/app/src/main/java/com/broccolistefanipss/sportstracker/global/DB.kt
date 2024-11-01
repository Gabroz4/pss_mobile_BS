package com.broccolistefanipss.sportstracker.global

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.broccolistefanipss.sportstracker.model.CalendarTraining
import com.broccolistefanipss.sportstracker.model.TrainingSession
import com.broccolistefanipss.sportstracker.model.User
import com.google.android.gms.maps.model.LatLng

// Classe DB che estende SQLiteOpenHelper
class DB(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    // quando il database viene creato, vengono create le tabelle.
    override fun onCreate(db: SQLiteDatabase?) {
        createTables(db)
    }

    // chiamata quando il database deve essere aggiornato
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // rimuove le tabelle esistenti
        dropTables(db)
        onCreate(db) // ricrea le tabelle
    }

    private fun dropTables(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS TrainingSessions")
        db.execSQL("DROP TABLE IF EXISTS CalendarTraining")
        db.execSQL("DROP TABLE IF EXISTS User")
        db.execSQL("DROP TABLE IF EXISTS Location")
    }

    // Esegue SQL per creare le tabelle User e TrainingSessions.
    private fun createTables(db: SQLiteDatabase?){
        db?.execSQL(SqlTable.User)
        db?.execSQL(SqlTable.TrainingSessions)
        db?.execSQL(SqlTable.CalendarTraining)
        db?.execSQL(SqlTable.Location)
    }

    // inserisce un nuovo utente nel database
    fun insertUser(userName: String, password: String, sesso: String, eta: Int, altezza: Int, peso: Int, obiettivo: String): Boolean {
        // verifica se l'utente esiste già.
        return if (!isNameExists(userName)) {
            // prepara la query SQL e inserisce i dati.
            val sqlQuery = "INSERT INTO User(userName, password, sesso, eta, altezza, peso, obiettivo) VALUES(?, ?, ?, ?, ?, ?, ?)"
            val db = this.writableDatabase
            db.execSQL(sqlQuery, arrayOf(userName, password, sesso, eta, altezza, peso, obiettivo))
            db.close()
            true
        } else {
            Log.d("DB", "Username '$userName' esiste già nel database")
            false
        }

    }

    fun getAllTrainingsByUserId(userId: String): List<TrainingSession> {
        val trainings = mutableListOf<TrainingSession>()
        val db = this.readableDatabase

        val cursor = db.rawQuery("SELECT * FROM TrainingSessions WHERE userName = ?", arrayOf(userId))
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("sessionId"))
                val sessionDate = it.getString(it.getColumnIndexOrThrow("sessionDate"))
                val duration = it.getInt(it.getColumnIndexOrThrow("duration"))
                val trainingType = it.getString(it.getColumnIndexOrThrow("trainingType"))
                val distance = it.getFloat(it.getColumnIndexOrThrow("distance"))
                val burntCalories = it.getInt(it.getColumnIndexOrThrow("burntCalories"))

                val training = TrainingSession(id, userId, sessionDate, duration, distance, trainingType, burntCalories)
                trainings.add(training)
            }
        }
        db.close()
        return trainings
    }


    // funzione per ottenere tutte le posizioni di un allenamento
    fun getLocationsByTrainingId(trainingId: Long): List<LatLng> {
        val db = readableDatabase
        val locations = mutableListOf<LatLng>()

        val cursor = db.rawQuery("SELECT latitude, longitude FROM Location WHERE trainingId = ?", arrayOf(trainingId.toString()))
        cursor?.use {
            while (it.moveToNext()) {
                val latitude = it.getDouble(it.getColumnIndexOrThrow("latitude"))
                val longitude = it.getDouble(it.getColumnIndexOrThrow("longitude"))
                locations.add(LatLng(latitude, longitude))
            }
        }
        db.close()
        return locations
    }


        fun insertTrainingLocation(sessionId: Long, latitude: Double, longitude: Double, timestamp: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("trainingId", sessionId)
            put("latitude", latitude)
            put("longitude", longitude)
            put("timestamp", timestamp)
        }
        db.insert("Location", null, values)
        db.close()
    }

    // controlla se un nome utente esiste già
    private fun isNameExists(userName: String): Boolean {
        val database = readableDatabase
        database.rawQuery("SELECT * FROM User WHERE userName = ?", arrayOf(userName)).use { cursor ->
            return cursor.count > 0 // true se esiste già
        }
    }

    fun insertTrainingSession(userName: String, sessionDate: String, duration: Int, distance: Float, trainingType: String, burntCalories: Int): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("userName", userName)
            put("sessionDate", sessionDate)
            put("duration", duration)
            put("distance", distance)
            put("trainingType", trainingType)
            put("burntCalories", burntCalories)
        }
        val sessionId = db.insert("TrainingSessions", null, contentValues)
        db.close()
        return sessionId
    }

    fun insertCalendarTraining(userName: String, date: String, description: String) {
        val database = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("userName", userName)
            put("date", date)
            put("description", description)
        }
        database.insert("CalendarTraining", null, contentValues)
        database.close()
    }

    fun getCalendarTrainingsUser(userName: String): MutableList<CalendarTraining> {
        val calendarTrainingsList = mutableListOf<CalendarTraining>()
        val db = this.readableDatabase

        db.rawQuery("SELECT * FROM CalendarTraining WHERE userName = ?", arrayOf(userName)).use { cursor ->
            while (cursor.moveToNext()) {

                // estrai i dati da ogni colonna
                val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))

                // crea un oggetto TrainingSession con i dati estratti
                val calendarTraining = CalendarTraining(userName, date, description)

                // aggiungi l'oggetto alla lista
                calendarTrainingsList.add(calendarTraining)
            }
        }
        return calendarTrainingsList
    }

    // recupera le sessioni di allenamento per un utente specifico
    fun getUserTrainingSessions(userName: String): List<TrainingSession> {
        val trainingSessionsList = mutableListOf<TrainingSession>()
        val db = this.readableDatabase

        db.rawQuery("SELECT * FROM TrainingSessions WHERE userName = ?", arrayOf(userName)).use { cursor ->
            while (cursor.moveToNext()) {
                // estrai i dati da ogni colonna
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("sessionId"))
                val sessionDate = cursor.getString(cursor.getColumnIndexOrThrow("sessionDate"))
                val duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration"))
                val trainingType = cursor.getString(cursor.getColumnIndexOrThrow("trainingType"))
                val distance = cursor.getFloat(cursor.getColumnIndexOrThrow("distance"))
                val burntCalories = cursor.getInt(cursor.getColumnIndexOrThrow("burntCalories"))

                // crea un oggetto TrainingSession con i dati estratti
                val trainingSession = TrainingSession(id, userName, sessionDate, duration,distance, trainingType,  burntCalories)

                // aggiungi l'oggetto alla lista
                trainingSessionsList.add(trainingSession)
            }
        }
        return trainingSessionsList
    }

    fun deleteTrainingSession(sessionId: Int): Boolean {
        val database = this.writableDatabase
        // restituisce true se una riga è stata effettivamente eliminata
        val affectedRows = database.delete("TrainingSessions", "sessionId = ?", arrayOf(sessionId.toString()))
        database.close()
        return affectedRows > 0
    }

    fun getUserData(username: String): User? {
        val db = this.readableDatabase
        var user: User? = null

        val cursor = db.rawQuery("SELECT * FROM User WHERE userName = ?", arrayOf(username))
        cursor?.use {
            if (it.moveToFirst()) {
                val userName = it.getString(it.getColumnIndexOrThrow("userName"))
                val password = it.getString(it.getColumnIndexOrThrow("password"))
                val sesso = it.getString(it.getColumnIndexOrThrow("sesso"))
                val eta = it.getInt(it.getColumnIndexOrThrow("eta"))
                val altezza = it.getInt(it.getColumnIndexOrThrow("altezza"))
                val peso = it.getDouble(it.getColumnIndexOrThrow("peso"))
                val obiettivo = it.getString(it.getColumnIndexOrThrow("obiettivo"))
                user = User(userName, password, sesso, eta, altezza, peso, obiettivo)
            }
        }
        cursor?.close()
        db.close()

        return user
    }

    fun userLogin(userName: String, password: String): Boolean? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM User WHERE userName = ? AND password = ?", arrayOf(userName, password))

        val userExists = cursor?.use { // cursor viene chiuso automaticamente
            it.count > 0
        }

        db.close()
        return userExists
    }

    fun updateUser(
        currentUserName: String,
        newEta: Int? = null,
        newAltezza: Int? = null,
        newPeso: Double? = null,
        newObiettivo: String? = null,
        newSesso: String? = null
    ): Boolean {

        val db = this.writableDatabase

        val contentValues = ContentValues().apply { // incapsula i dati
            newEta?.let { put("eta", it) }
            newAltezza?.let { put("altezza", it) }
            newPeso?.let { put("peso", it) }
            newObiettivo?.let { put("obiettivo", it) }
            newSesso?.let { put("sesso", it) }
        }

        // verifica se ci sono valori da aggiornare
        return if (contentValues.size() > 0) {
            val modifiedRows =
                db.update("User", contentValues, "userName = ?", arrayOf(currentUserName))

            Log.d("EditUserActivity", "Valori aggiornati: $contentValues")

            db.close()
            modifiedRows > 0 // true se almeno una riga è stata aggiornata
        } else {
            db.close() // chiudi il database anche se non ci sono modifiche
            false // nessun valore da aggiornare
        }
    }

    companion object {
        private const val DB_VERSION = 18
        private const val DB_NAME = "SportsTracker.db"
    }
}
