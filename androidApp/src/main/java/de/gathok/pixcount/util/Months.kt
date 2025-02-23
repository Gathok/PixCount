package de.gathok.pixcount.util

import de.gathok.pixcount.R

enum class Months {
    JANUARY,
    FEBRUARY,
    MARCH,
    APRIL,
    MAY,
    JUNE,
    JULY,
    AUGUST,
    SEPTEMBER,
    OCTOBER,
    NOVEMBER,
    DECEMBER;

    val getShortStringId : Int
        get() = when (this) {
            JANUARY -> R.string.january_short
            FEBRUARY -> R.string.february_short
            MARCH -> R.string.march_short
            APRIL -> R.string.april_short
            MAY -> R.string.may_short
            JUNE -> R.string.june_short
            JULY -> R.string.july_short
            AUGUST -> R.string.august_short
            SEPTEMBER -> R.string.september_short
            OCTOBER -> R.string.october_short
            NOVEMBER -> R.string.november_short
            DECEMBER -> R.string.december_short
        }

    val getDaysCount : Int
        get() = when (this) {
            JANUARY -> 31
            FEBRUARY -> 28
            MARCH -> 31
            APRIL -> 30
            MAY -> 31
            JUNE -> 30
            JULY -> 31
            AUGUST -> 31
            SEPTEMBER -> 30
            OCTOBER -> 31
            NOVEMBER -> 30
            DECEMBER -> 31
        }

    val getIndex : Int
        get() = when (this) {
            JANUARY -> 1
            FEBRUARY -> 2
            MARCH -> 3
            APRIL -> 4
            MAY -> 5
            JUNE -> 6
            JULY -> 7
            AUGUST -> 8
            SEPTEMBER -> 9
            OCTOBER -> 10
            NOVEMBER -> 11
            DECEMBER -> 12
        }

    companion object {
        fun getByIndex(index: Int): Months {
            return when (index) {
                1 -> JANUARY
                2 -> FEBRUARY
                3 -> MARCH
                4 -> APRIL
                5 -> MAY
                6 -> JUNE
                7 -> JULY
                8 -> AUGUST
                9 -> SEPTEMBER
                10 -> OCTOBER
                11 -> NOVEMBER
                12 -> DECEMBER
                else -> throw IllegalArgumentException("Invalid index")
            }
        }
    }
}