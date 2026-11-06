package com.attendanceradar.app.core.common

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
}
