package de.gathok.pixcount.db

import de.gathok.pixcount.util.Months
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList

class PixListValues() : EmbeddedRealmObject {
    private var janValues: RealmList<PixCategory> = realmListOf()
    private var febValues: RealmList<PixCategory> = realmListOf()
    private var marValues: RealmList<PixCategory> = realmListOf()
    private var aprValues: RealmList<PixCategory> = realmListOf()
    private var mayValues: RealmList<PixCategory> = realmListOf()
    private var junValues: RealmList<PixCategory> = realmListOf()
    private var julValues: RealmList<PixCategory> = realmListOf()
    private var augValues: RealmList<PixCategory> = realmListOf()
    private var sepValues: RealmList<PixCategory> = realmListOf()
    private var octValues: RealmList<PixCategory> = realmListOf()
    private var novValues: RealmList<PixCategory> = realmListOf()
    private var decValues: RealmList<PixCategory> = realmListOf()

    constructor(leapYear: Boolean) : this() {
        val emptyCategory = PixCategory()
        for (i in 0 until Months.FEBRUARY.getDaysCount + if (leapYear) 1 else 0) febValues.add(emptyCategory)
    }

    init {
        val emptyCategory = PixCategory()
        for (i in 0 until Months.JANUARY.getDaysCount) janValues.add(emptyCategory)
        for (i in 0 until Months.FEBRUARY.getDaysCount) febValues.add(emptyCategory)
        for (i in 0 until Months.MARCH.getDaysCount) marValues.add(emptyCategory)
        for (i in 0 until Months.APRIL.getDaysCount) aprValues.add(emptyCategory)
        for (i in 0 until Months.MAY.getDaysCount) mayValues.add(emptyCategory)
        for (i in 0 until Months.JUNE.getDaysCount) junValues.add(emptyCategory)
        for (i in 0 until Months.JULY.getDaysCount) julValues.add(emptyCategory)
        for (i in 0 until Months.AUGUST.getDaysCount) augValues.add(emptyCategory)
        for (i in 0 until Months.SEPTEMBER.getDaysCount) sepValues.add(emptyCategory)
        for (i in 0 until Months.OCTOBER.getDaysCount) octValues.add(emptyCategory)
        for (i in 0 until Months.NOVEMBER.getDaysCount) novValues.add(emptyCategory)
        for (i in 0 until Months.DECEMBER.getDaysCount) decValues.add(emptyCategory)
    }

    fun getEntry(day: Int, month: Months): PixCategory {
        return when(month) {
            Months.JANUARY -> janValues[day - 1]
            Months.FEBRUARY -> febValues[day - 1]
            Months.MARCH -> marValues[day - 1]
            Months.APRIL -> aprValues[day - 1]
            Months.MAY -> mayValues[day - 1]
            Months.JUNE -> junValues[day - 1]
            Months.JULY -> julValues[day - 1]
            Months.AUGUST -> augValues[day - 1]
            Months.SEPTEMBER -> sepValues[day - 1]
            Months.OCTOBER -> octValues[day - 1]
            Months.NOVEMBER -> novValues[day - 1]
            Months.DECEMBER -> decValues[day - 1]
        }
    }

    fun setEntry(day: Int, month: Months, category: PixCategory) {
        when(month) {
            Months.JANUARY -> janValues[day - 1] = category
            Months.FEBRUARY -> febValues[day - 1] = category
            Months.MARCH -> marValues[day - 1] = category
            Months.APRIL -> aprValues[day - 1] = category
            Months.MAY -> mayValues[day - 1] = category
            Months.JUNE -> junValues[day - 1] = category
            Months.JULY -> julValues[day - 1] = category
            Months.AUGUST -> augValues[day - 1] = category
            Months.SEPTEMBER -> sepValues[day - 1] = category
            Months.OCTOBER -> octValues[day - 1] = category
            Months.NOVEMBER -> novValues[day - 1] = category
            Months.DECEMBER -> decValues[day - 1] = category
        }
    }
}