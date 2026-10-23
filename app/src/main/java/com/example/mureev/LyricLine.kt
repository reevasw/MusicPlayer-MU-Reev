package com.example.mureev

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LyricLine(val time: Long, val text: String) : Parcelable