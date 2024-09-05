package com.broccolistefanipss.sportstracker.fragment.calendar

import CalendarViewModel
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.broccolistefanipss.sportstracker.databinding.FragmentCalendarBinding
import com.broccolistefanipss.sportstracker.global.DB
import java.time.LocalDate
import java.time.format.DateTimeParseException
import com.prolificinteractive.materialcalendarview.CalendarDay

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        calendarViewModel = ViewModelProvider(this)[CalendarViewModel::class.java]

        val db = DB(requireContext())
        calendarViewModel.init(db, requireContext())  // Pass the context to the ViewModel

        sharedPreferences =
            requireContext().getSharedPreferences("TrainingPrefs", Context.MODE_PRIVATE)

        setupCalendarClickListener()
        setupSaveButtonListener()
        observeCalendarTrainings()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Observe the LiveData from ViewModel to decorate dates with training sessions
    private fun observeCalendarTrainings() {
        calendarViewModel.calendarTrainingSessions.observe(viewLifecycleOwner) { sessions ->
            val calendarDays = sessions.mapNotNull { session ->
                try {
                    // Converti la data in LocalDate
                    val localDate = LocalDate.parse(session.date)
                    // Converte LocalDate in CalendarDay per il MaterialCalendarView
                    CalendarDay.from(localDate.year, localDate.monthValue, localDate.dayOfMonth)
                } catch (e: DateTimeParseException) {
                    null  // Ignora le date non valide
                }
            }.toSet()

            binding.calendarView.removeDecorators()  // Clear any previous decorators
            if (calendarDays.isNotEmpty()) {
                binding.calendarView.addDecorator(EventDecorator(requireContext(), calendarDays))
            }
        }
    }

    // Handles clicks on the calendar dates and displays the session details if available
    private fun setupCalendarClickListener() {
        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            val formattedDate = String.format(
                "%04d-%02d-%02d",
                date.year,
                date.month + 1,
                date.day
            )  // Format date as "YYYY-MM-DD"
            binding.editTextDate.setText(formattedDate)
            checkForExistingTraining(formattedDate)
        }
    }

    // Handles save button click, saves training data via ViewModel
    private fun setupSaveButtonListener() {
        binding.buttonSaveTraining.setOnClickListener {
            val date = binding.editTextDate.text.toString()
            val details = binding.editTextDetails.text.toString()

            if (date.isNotEmpty() && details.isNotEmpty()) {
                calendarViewModel.saveCalendarTraining(date, details)  // Save via ViewModel
                Toast.makeText(requireContext(), "Allenamento salvato", Toast.LENGTH_SHORT).show()
                checkForExistingTraining(date)
            } else {
                Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Check and display the existing training details in the TextView
    private fun checkForExistingTraining(date: String) {
        calendarViewModel.calendarTrainingSessions.value?.let { sessions ->
            val training = sessions.find { it.date == date }
            if (training != null) {
                binding.textViewTrainingDetails.apply {
                    text = "Allenamento per $date: ${training.description}"
                    visibility = View.VISIBLE
                }
            } else {
                binding.textViewTrainingDetails.visibility = View.GONE
            }
        }
    }
}

//class CalendarFragment : Fragment() {
//
//    private var _binding: FragmentCalendarBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var calendarViewModel: CalendarViewModel
//    private lateinit var sharedPreferences: SharedPreferences
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
//        calendarViewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
//
//        val db = DB(requireContext())
//        calendarViewModel.init(db, requireContext())  // Pass the context to the ViewModel
//
//        sharedPreferences = requireContext().getSharedPreferences("TrainingPrefs", Context.MODE_PRIVATE)
//
//        setupCalendarClickListener()
//        setupSaveButtonListener()
//        observeCalendarTrainings()
//
//        return binding.root
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//    private fun observeCalendarTrainings() {
//        calendarViewModel.calendarTrainingSessions.observe(viewLifecycleOwner) { sessions ->
//            val calendarDays = sessions.mapNotNull {
//                try {
//                    LocalDate.parse(it.date)
//                } catch (e: DateTimeParseException) {
//                    null
//                }
//            }.toSet()
//            binding.calendarView.removeDecorators()  // Rimuovi decoratori precedenti
//            binding.calendarView.addDecorator(EventDecorator(calendarDays))  // Applica nuovi decoratori
//        }
//    }
//
//    private fun setupCalendarClickListener() {
//        binding.calendarView.setOnDateChangedListener { widget, date, selected ->
//            val formattedDate = String.format("%02d-%02d-%04d", date.day, date.month + 1, date.year)
//            binding.editTextDate.setText(formattedDate)
//            checkForExistingTraining(formattedDate)
//        }
//    }
//
//    private fun setupSaveButtonListener() {
//        binding.buttonSaveTraining.setOnClickListener {
//            val date = binding.editTextDate.text.toString()
//            val details = binding.editTextDetails.text.toString()
//
//            if (date.isNotEmpty() && details.isNotEmpty()) {
//                calendarViewModel.saveCalendarTraining(date, details)  // Usa ViewModel per salvare i dati
//                Toast.makeText(requireContext(), "Allenamento salvato", Toast.LENGTH_SHORT).show()
//                checkForExistingTraining(date)
//            } else {
//                Toast.makeText(requireContext(), "Compila tutti i campi", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun checkForExistingTraining(date: String) {
//        val details = sharedPreferences.getString("training_$date", null)
//
//        if (details != null) {
//            binding.textViewTrainingDetails.apply {
//                text = "Allenamento per $date: $details"
//                visibility = View.VISIBLE
//            }
//        } else {
//            binding.textViewTrainingDetails.visibility = View.GONE
//        }
//    }
//}