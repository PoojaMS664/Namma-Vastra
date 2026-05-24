package com.nammavastra.app

import android.app.Application
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

class NammaVastraApp : Application() {

    companion object {
        var supabaseClient: SupabaseClient? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        try {
            supabaseClient = createSupabaseClient(
                supabaseUrl = "https://mebsexhmogmnjunedoig.supabase.co",
                supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1lYnNleGhtb2dtbmp1bmVkb2lnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk0MzA4OTMsImV4cCI6MjA5NTAwNjg5M30.dsy6ZvSt3yhsTAvKjvxgLj0S811-nS8ENpoe5Jd4ko0"
            ) {
                install(Auth) {
                    alwaysAutoRefresh = true
                }
                install(Postgrest)
                install(Storage)
            }
        } catch (e: Exception) {
            Log.e("NammaVastraApp", "Supabase initialization failed", e)
        }
    }
}
