package com.gentech.picklepro.data.repository

import android.content.Context
import com.gentech.picklepro.data.remote.SupabaseModule
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class RedeemCodeRequest(val code: String)

@Serializable
data class RedeemCodeResponse(
    val ok: Boolean,
    @SerialName("code_id") val codeId: String? = null,
    val credits: Int? = null,
    val error: String? = null,
)

@Serializable
private data class IdOnly(val id: String)

/** Activation-code redemption + credit tracking (spec §5.1). */
class OrganizerRepository(context: Context) {

    private val client = SupabaseModule.get(context)

    suspend fun redeemActivationCode(code: String): RedeemCodeResponse =
        client.functions.invoke("redeem-code", body = RedeemCodeRequest(code.trim().uppercase()))
            .body()

    /** Unredeemed credits: codes this organizer redeemed but hasn't spent on a tournament yet. */
    suspend fun getUnconsumedCreditCount(organizerId: String): Int =
        client.postgrest.from("activation_codes")
            .select(Columns.list("id")) {
                filter {
                    eq("redeemed_by", organizerId)
                    eq("status", "redeemed")
                    isNull("consumed_by_tournament")
                }
            }
            .decodeList<IdOnly>()
            .size
}
