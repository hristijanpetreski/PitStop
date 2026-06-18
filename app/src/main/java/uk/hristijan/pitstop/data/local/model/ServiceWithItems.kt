package uk.hristijan.pitstop.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import uk.hristijan.pitstop.data.local.entity.ServiceItem
import uk.hristijan.pitstop.data.local.entity.ServiceRecord

data class ServiceWithItems(
    @Embedded val service: ServiceRecord,
    @Relation(parentColumn = "id", entityColumn = "serviceRecordId")
    val items: List<ServiceItem>,
)
