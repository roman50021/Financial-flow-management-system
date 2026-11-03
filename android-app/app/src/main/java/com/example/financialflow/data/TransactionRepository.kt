package com.example.financialflow.data

import com.example.financialflow.BuildConfig
import com.example.financialflow.model.Category
import com.example.financialflow.model.CategoryDraft
import com.example.financialflow.model.Transaction
import com.example.financialflow.model.TransactionDraft
import com.example.financialflow.model.TransactionType
import java.io.IOException
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TransactionRepository(
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) {
    suspend fun getTransactions(): List<Transaction> = withContext(Dispatchers.IO) {
        val body = request(method = "GET", path = "/transactions")
        val items = JSONArray(body)
        buildList {
            for (index in 0 until items.length()) {
                add(items.getJSONObject(index).toTransaction())
            }
        }
    }

    suspend fun createTransaction(draft: TransactionDraft): Transaction = withContext(Dispatchers.IO) {
        val payload = draft.toJson()
        JSONObject(request(method = "POST", path = "/transactions", body = payload.toString()))
            .toTransaction()
    }

    suspend fun updateTransaction(id: Long, draft: TransactionDraft): Transaction = withContext(Dispatchers.IO) {
        val payload = draft.toJson()
        JSONObject(request(method = "PUT", path = "/transactions/$id", body = payload.toString()))
            .toTransaction()
    }

    suspend fun deleteTransaction(id: Long): Unit = withContext(Dispatchers.IO) {
        request(method = "DELETE", path = "/transactions/$id")
    }

    suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        val items = JSONArray(request(method = "GET", path = "/categories"))
        buildList {
            for (index in 0 until items.length()) {
                add(items.getJSONObject(index).toCategory())
            }
        }
    }

    suspend fun createCategory(draft: CategoryDraft): Category = withContext(Dispatchers.IO) {
        val payload = draft.toJson()
        JSONObject(request(method = "POST", path = "/categories", body = payload.toString()))
            .toCategory()
    }

    suspend fun updateCategory(id: Long, draft: CategoryDraft): Category = withContext(Dispatchers.IO) {
        val payload = draft.toJson()
        JSONObject(request(method = "PUT", path = "/categories/$id", body = payload.toString()))
            .toCategory()
    }

    suspend fun deleteCategory(id: Long): Unit = withContext(Dispatchers.IO) {
        request(method = "DELETE", path = "/categories/$id")
    }

    fun observeDataChanges(): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            var connection: HttpURLConnection? = null
            try {
                val activeConnection = (URL("$baseUrl/events").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 0
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                connection = activeConnection

                val status = activeConnection.responseCode
                if (status !in 200..299) {
                    throw IOException("Live updates returned HTTP $status")
                }

                activeConnection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    var eventName: String? = null
                    while (currentCoroutineContext().isActive) {
                        val line = reader.readLine() ?: break
                        when {
                            line.startsWith("event:") -> eventName = line.substringAfter(':').trim()
                            line.isEmpty() -> {
                                if (eventName == "data-change") {
                                    emit(Unit)
                                }
                                eventName = null
                            }
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: IOException) {
                // The loop reconnects automatically after a short network interruption.
            } finally {
                connection?.disconnect()
            }

            if (currentCoroutineContext().isActive) {
                delay(1_000)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun request(method: String, path: String, body: String? = null): String {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        return try {
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }

            val status = connection.responseCode
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (status !in 200..299) {
                val message = runCatching { JSONObject(responseBody).optString("message") }
                    .getOrNull()
                    .takeUnless { it.isNullOrBlank() }
                    ?: "API повернув помилку $status"
                throw ApiException(status, message)
            }

            responseBody
        } catch (error: ApiException) {
            throw error
        } catch (error: IOException) {
            throw error
        } finally {
            connection.disconnect()
        }
    }
}

class ApiException(
    val statusCode: Int,
    override val message: String,
) : IOException(message)

private fun JSONObject.toTransaction() = Transaction(
    id = getLong("id"),
    title = nullableString("title"),
    description = nullableString("description"),
    amount = BigDecimal(get("amount").toString()),
    type = TransactionType.valueOf(getString("type")),
    transactionDate = getString("transactionDate"),
    categoryId = nullableLong("categoryId"),
    categoryName = nullableString("categoryName"),
    categoryColor = nullableString("categoryColor"),
    createdAt = nullableString("createdAt"),
)

private fun JSONObject.toCategory() = Category(
    id = getLong("id"),
    name = getString("name"),
    type = TransactionType.valueOf(getString("type")),
    color = nullableString("color"),
)

private fun CategoryDraft.toJson() = JSONObject()
    .put("name", name)
    .put("type", type.name)
    .put("userId", JSONObject.NULL)

private fun TransactionDraft.toJson() = JSONObject()
    .put("title", title?.takeIf(String::isNotBlank) ?: JSONObject.NULL)
    .put("description", description.takeIf(String::isNotBlank) ?: JSONObject.NULL)
    .put("amount", amount)
    .put("type", type.name)
    .put("transactionDate", transactionDate)
    .put("categoryId", categoryId ?: JSONObject.NULL)
    .put("userId", JSONObject.NULL)

private fun JSONObject.nullableString(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

private fun JSONObject.nullableLong(key: String): Long? =
    if (has(key) && !isNull(key)) getLong(key) else null
