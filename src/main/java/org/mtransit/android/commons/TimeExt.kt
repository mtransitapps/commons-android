package org.mtransit.android.commons

import java.util.Date

fun Date.equalOrAfter(otherDate: Date) = this == otherDate || this.after(otherDate)