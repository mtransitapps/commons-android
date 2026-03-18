package org.mtransit.android.commons.provider.serviceupdate

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val ServiceUpdateProviderContract.serviceUpdateMaxValidity: Duration get() = this.serviceUpdateMaxValidityInMs.milliseconds

fun ServiceUpdateProviderContract.getServiceUpdateValidity(inFocus: Boolean): Duration = this.getServiceUpdateValidityInMs(inFocus).milliseconds
