package com.gentech.picklepro.data.remote

import android.content.Context
import com.gentech.picklepro.BuildConfig
import com.gentech.picklepro.data.session.DataStoreSessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Single Supabase client for the app. Session tokens persist via
 * [DataStoreSessionManager] rather than the SDK's default store, so the rest
 * of the app can share the same DataStore-based persistence pattern.
 */
object SupabaseModule {

    @Volatile
    private var client: SupabaseClient? = null

    fun get(context: Context): SupabaseClient =
        client ?: synchronized(this) {
            client ?: buildClient(context.applicationContext).also { client = it }
        }

    private fun buildClient(appContext: Context): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth) {
                sessionManager = DataStoreSessionManager(appContext)
            }
            install(Postgrest)
            install(Storage)
        }
}
