package org.mtransit.android.commons.data

interface Targetable {

    val uUID: String
    val uuid: String get() = this.uUID

}
