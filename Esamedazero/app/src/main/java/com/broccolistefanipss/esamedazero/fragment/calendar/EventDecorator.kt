package com.broccolistefanipss.esamedazero.fragment.calendar

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.time.LocalDate

class EventDecorator(private val dates: Set<LocalDate>) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        val localDate = LocalDate.of(day.year, day.month + 1, day.day)
        return dates.contains(localDate)
    }


    override fun decorate(view: DayViewFacade) {
        view.addSpan(ColorDrawable(Color.BLUE)) // Customize the highlight color as needed
    }
}