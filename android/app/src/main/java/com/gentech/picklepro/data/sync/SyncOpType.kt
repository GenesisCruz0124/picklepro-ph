package com.gentech.picklepro.data.sync

/** [com.gentech.picklepro.data.local.entity.PendingOpEntity.opType] values. */
object SyncOpType {
    const val UPSERT_MATCH = "upsert_match"
    const val PROCESS_MATCH_RESULT = "process_match_result"
    const val ADVANCE_BRACKET = "advance_bracket"
}
