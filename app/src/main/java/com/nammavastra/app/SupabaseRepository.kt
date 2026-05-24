package com.nammavastra.app

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import java.util.UUID

// Helper class to pass data and debug info to the UI
data class SareeResult(val items: List<Saree>, val rawJson: String, val error: String? = null)

class SupabaseRepository(private val client: SupabaseClient) {

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        isLenient = true
        explicitNulls = false 
    }

    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun signInAnonymously() {
        try {
            client.auth.signInAnonymously()
            Log.d("SupabaseRepository", "Anonymous sign-in successful")
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Auth failed", e)
            throw e
        }
    }

    suspend fun saveProfile(profile: WeaverProfile) {
        try {
            client.from("profiles").upsert(profile)
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Error saving profile", e)
            throw e
        }
    }

    suspend fun getProfile(uid: String): WeaverProfile? {
        return try {
            client.from("profiles")
                .select {
                    filter {
                        eq("id", uid)
                    }
                }
                .decodeSingleOrNull<WeaverProfile>()
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Error getting profile", e)
            null
        }
    }

    suspend fun uploadSaree(
        context: Context,
        title: String,
        price: String,
        origin: String,
        imageUri: Uri,
        palette: List<String>
    ) {
        val uid = getCurrentUserId() ?: throw Exception("User not logged in")
        val profile = getProfile(uid) ?: WeaverProfile(name = "Unknown")

        try {
            val fileName = "saree_${UUID.randomUUID()}.jpg"
            val bucket = client.storage.from("sarees")
            
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(imageUri)?.readBytes()
            } ?: throw Exception("Could not read image bytes")

            bucket.upload(fileName, bytes)
            
            val downloadUrl = bucket.publicUrl(fileName)

            val saree = Saree(
                id = fileName,
                title = title,
                origin = origin,
                price = price,
                imageUrl = downloadUrl,
                weaverId = uid,
                weaverName = profile.name,
                weaverWhatsapp = profile.whatsapp,
                palette = palette,
                timestamp = System.currentTimeMillis()
            )

            client.from("gallery").insert(saree)
            Log.d("SupabaseRepository", "Upload successful: $fileName")
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Upload error: ${e.message}", e)
            throw e
        }
    }

    suspend fun getSareesResult(): SareeResult {
        return try {
            val response = client.from("gallery")
                .select {
                    order("timestamp", Order.DESCENDING)
                }
            
            val rawData = response.data
            Log.d("SupabaseRepository", "RAW DATA: $rawData")
            
            if (rawData == "[]" || rawData.isEmpty()) {
                return SareeResult(emptyList(), rawData, "Server returned 0 items. Check RLS policies.")
            }

            val jsonElement = json.parseToJsonElement(rawData)
            val jsonArray = jsonElement.jsonArray
            
            val validSarees = mutableListOf<Saree>()
            var decodeErrors = ""
            jsonArray.forEach { element ->
                try {
                    val saree = json.decodeFromJsonElement<Saree>(element)
                    validSarees.add(saree)
                } catch (e: Exception) {
                    decodeErrors += "${e.message} | "
                }
            }
            
            val errorInfo = if (decodeErrors.isNotEmpty()) "Decode Errors: $decodeErrors" else null
            SareeResult(validSarees, rawData, errorInfo)
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Fetch error", e)
            SareeResult(emptyList(), "Network Error", e.message)
        }
    }

    suspend fun getSarees(): List<Saree> = getSareesResult().items

    suspend fun getTrends(): List<Trend> {
        return try {
            client.from("trends").select().decodeList<Trend>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Sign out error", e)
        }
    }
}
