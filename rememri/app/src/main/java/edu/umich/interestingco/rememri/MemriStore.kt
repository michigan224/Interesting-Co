package edu.umich.interestingco.rememri

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley.newRequestQueue
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.reflect.full.declaredMemberProperties

object MemriStore {
    val memris = arrayListOf<Memri?>()
    private val nFields = Memri::class.declaredMemberProperties.size

    private lateinit var queue: RequestQueue
    private const val serverUrl = "https://temp/"
    fun postMemri(context: Context, memri: Memri) {
        val jsonObj = mapOf(
            "username" to memri.owner_id,
            "is_public" to memri.is_public,
            "pin_location" to memri.location,
            "post_id" to memri.pin_id
        )
        val postRequest = JsonObjectRequest(
            Request.Method.POST,
            serverUrl+"pin/", JSONObject(jsonObj),
            { Log.d("pin", "memri posted!") },
            { error -> Log.e("pin", error.localizedMessage ?: "JsonObjectRequest error") }
        )

        if (!this::queue.isInitialized) {
            queue = newRequestQueue(context)
        }
        queue.add(postRequest)
    }

    fun getMemris(context: Context, completion: () -> Unit) {
        val getRequest = JsonObjectRequest(serverUrl+"nearby_pins/",
            { response ->
                memris.clear()
                val memrisReceived = try { response.getJSONArray("pins") } catch (e: JSONException) { JSONArray() }
                for (i in 0 until memrisReceived.length()) {
                    val memriEntry = memrisReceived[i] as JSONArray
                    if (memriEntry.length() == nFields) {
                        memris.add(Memri(
                            owner_id = memriEntry[0].toString(),
                            media_url = memriEntry[1].toString(),
                            timestamp = memriEntry[2].toString()))
                    } else {
                        Log.e("getMemris", "Received unexpected number of fields: " + memriEntry.length().toString() + " instead of " + nFields.toString())
                    }
                }
                completion()
            }, { completion() }
        )

        if (!this::queue.isInitialized) {
            queue = newRequestQueue(context)
        }
        queue.add(getRequest)
    }
}