package com.smartmail.data.local.database

import androidx.room.TypeConverter
import com.smartmail.domain.models.AccountType
import java.util.Date

class Converters {

    // Date <-> Long
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // AccountType <-> String
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)
}
