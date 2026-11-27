package org.mtransit.android.commons.data

@JvmOverloads
fun makeBikeStation(
    authority: String,
    id: Int,
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int = DataSourceTypeId.BIKE,
    lat: Double,
    lng: Double,
    name: String,
    score: Int? = null,
    @Accessibility.AccessibilityType accessible: Int = Accessibility.DEFAULT
) = makeDefaultPOI(
    authority = authority,
    id = id,
    dataSourceTypeId = dataSourceTypeId,
    type = POI.ITEM_VIEW_TYPE_BASIC_POI,
    statusType = POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT,
    actionsType = POI.ITEM_ACTION_TYPE_FAVORITABLE,
    lat = lat,
    lng = lng,
    name = name,
    score = score,
    accessible = accessible,
)
