/*
 * B-Chess · © Elal Chico
 * Señalización WebRTC vía Firebase Realtime Database.
 * Solo intercambia metadatos (SDP/ICE) para el "apretón de manos";
 * una vez establecida la conexión P2P, Firebase queda fuera de la partida.
 *
 * Si Firebase no está configurado (falta google-services.json en la app),
 * todos los métodos reportan un error legible en vez de romper el arranque.
 */
package com.bchess.android.network

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ChildEventListener

private const val TAG = "Signaling"

/** Resultado de un intento de inicializar Firebase. */
sealed interface FirebaseInit {
    data class Ok(val database: FirebaseDatabase) : FirebaseInit
    data class Missing(val error: String) : FirebaseInit
}

/** Crea FirebaseDatabase de forma segura: nunca lanza, devuelve Missing con motivo. */
object FirebaseBootstrap {
    fun get(): FirebaseInit = try {
        FirebaseInit.Ok(FirebaseDatabase.getInstance())
    } catch (e: Throwable) {
        FirebaseInit.Missing(e.message ?: e.javaClass.simpleName)
    }
}

class SignalingManager(private val database: FirebaseDatabase?) {

    private val matchesRef = database?.getReference("matches")

    fun createMatchId(): String {
        val key = matchesRef?.push()?.key
        return key ?: throw IllegalStateException(MISSING_ERROR)
    }

    /**
     * Resuelve el código de sala: automático (push key) o personalizado.
     * Firebase NO guarda una lista de códigos: solo comprobamos AHORA MISMO si la
     * sala existe. Al finalizar la conexión se borra el nodo (cleanupMatch), por lo
     * que el código queda libre para usarse de nuevo más adelante.
     *
     * @param customCode código elegido por el anfitrión (vacío = automático)
     * @param onResult código definitivo a usar
     * @param onError motivo legible si el código personalizado no está disponible
     */
    fun resolveMatchId(
        customCode: String?,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val custom = customCode?.trim().orEmpty()
        if (custom.isEmpty()) {
            val key = matchesRef?.push()?.key
            if (key == null) onError(MISSING_ERROR) else onResult(key)
            return
        }
        val normalized = custom.uppercase()
        if (!CUSTOM_CODE_REGEX.matches(normalized)) {
            onError("Código no válido: usa de 4 a 12 letras o números")
            return
        }
        val ref = matchesRef
        if (ref == null) {
            onError(MISSING_ERROR)
            return
        }
        ref.child(normalized).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onResult(normalized)
                    return
                }
                val createdAt = snapshot.child("createdAt").getValue(Long::class.java)
                val stale = createdAt == null || System.currentTimeMillis() - createdAt > STALE_MS
                if (stale) {
                    // Sala huérfana (crash sin cleanup): se libera el código.
                    snapshot.ref.removeValue()
                    onResult(normalized)
                } else {
                    onError("El código $normalized ya está en uso. Elige otro.")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Sin lectura fiable: se intenta usar; el nodo se valida al escribir.
                onResult(normalized)
            }
        })
    }

    /** Marca el nacimiento de la sala (para poder detectar salas huérfanas). */
    fun touchMatch(matchId: String) {
        try {
            matchesRef?.child(matchId)?.child("createdAt")?.setValue(ServerValue.TIMESTAMP)
        } catch (e: Exception) {
            Log.w(TAG, "touchMatch failed", e)
        }
    }

    /** Establece el hostId y estado "waiting" en la sala. */
    fun setHostIdAndWaiting(matchId: String, hostId: String) {
        try {
            matchesRef?.child(matchId)?.child("hostId")?.setValue(hostId)
            matchesRef?.child(matchId)?.child("status")?.setValue("waiting")
        } catch (e: Exception) {
            Log.w(TAG, "setHostIdAndWaiting failed", e)
        }
    }

    /** Establece el guestId en la sala. */
    fun setGuestId(matchId: String, guestId: String) {
        try {
            matchesRef?.child(matchId)?.child("guestId")?.setValue(guestId)
        } catch (e: Exception) {
            Log.w(TAG, "setGuestId failed", e)
        }
    }

    /** Normaliza el código que pega el invitado (los códigos propios van en mayúsculas). */
    fun normalizeJoinCode(raw: String): String {
        val code = raw.trim()
        // Las claves push de Firebase empiezan por "-" y no se tocan.
        return if (!code.startsWith("-") && CUSTOM_CODE_REGEX.matches(code)) code.uppercase() else code
    }

    /** Borra los nodos temporales de SDP/ICE del match (higiene del nodo de Firebase). */
    fun cleanupMatch(matchId: String) {
        try {
            matchesRef?.child(matchId)?.removeValue()
        } catch (e: Exception) {
            Log.w(TAG, "cleanupMatch failed", e)
        }
    }

    /** Limpia solo los nodos de señalización (offer, answer, iceCandidates) tras conectar.
     *  Conserva createdAt, hostId y guestId para vigencia y reutilización. */
    fun cleanupSignalingOnly(matchId: String) {
        try {
            matchesRef?.child(matchId)?.child("offer")?.removeValue()
            matchesRef?.child(matchId)?.child("answer")?.removeValue()
            matchesRef?.child(matchId)?.child("iceCandidates")?.removeValue()
        } catch (e: Exception) {
            Log.w(TAG, "cleanupSignalingOnly failed", e)
        }
    }

    /** Marca la sala como cancelada por el anfitrión: pone estado "cancelled" y limpia señalización.
     *  El código queda reservado para el mismo anfitrión durante STALE_MS. */
    fun cancelMatch(matchId: String, hostId: String) {
        try {
            matchesRef?.child(matchId)?.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                    if (mutableData.value == null) return com.google.firebase.database.Transaction.success(mutableData)
                    val currentHostId = mutableData.child("hostId").value as? String
                    if (currentHostId != hostId) return com.google.firebase.database.Transaction.abort()
                    mutableData.child("status").value = "cancelled"
                    mutableData.child("offer").value = null
                    mutableData.child("answer").value = null
                    mutableData.child("iceCandidates").value = null
                    return com.google.firebase.database.Transaction.success(mutableData)
                }
                override fun onComplete(databaseError: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {}
            })
        } catch (e: Exception) {
            Log.w(TAG, "cancelMatch failed", e)
        }
    }

    /** Genera un hostId determinista a partir del nombre del jugador. */
    fun generateHostId(name: String): String {
        var hash = 0
        for (i in 0 until name.length) {
            hash = ((hash shl 5) - hash) + name[i].toInt()
        }
        return Math.abs(hash).toString(36)
    }

    /** Reserva atómica de un código personalizado usando transacción Firebase.
     *  Permite reutilizar si: no existe, está vencida, o pertenece al mismo hostId. */
    fun reserveCustomCode(
        code: String,
        hostId: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = matchesRef?.child(code)
        if (ref == null) { onError(MISSING_ERROR); return }
        ref.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                if (mutableData.value == null) {
                    // Sala libre: la reservamos
                    mutableData.child("createdAt").value = ServerValue.TIMESTAMP
                    mutableData.child("hostId").value = hostId
                    mutableData.child("status").value = "waiting"
                    return com.google.firebase.database.Transaction.success(mutableData)
                }
                val existingHostId = mutableData.child("hostId").value as? String
                val createdAt = mutableData.child("createdAt").value as? Long
                val stale = createdAt == null || System.currentTimeMillis() - createdAt > STALE_MS
                if (stale || existingHostId == hostId) {
                    // Vencida o mismo host: la reutilizamos
                    mutableData.child("createdAt").value = ServerValue.TIMESTAMP
                    mutableData.child("hostId").value = hostId
                    mutableData.child("status").value = "waiting"
                    mutableData.child("offer").value = null
                    mutableData.child("answer").value = null
                    mutableData.child("iceCandidates").value = null
                    mutableData.child("guestId").value = null
                    return com.google.firebase.database.Transaction.success(mutableData)
                }
                // Ocupada por otro host
                return com.google.firebase.database.Transaction.abort()
            }
            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (committed) onResult(code) else onError("El código $code ya está en uso por otro host. Elige otro.")
            }
        })
    }

    fun saveOffer(matchId: String, sdp: String, onComplete: () -> Unit) {
        matchesRef?.child(matchId)?.child("offer")?.setValue(sdp)
            ?.addOnCompleteListener { onComplete() }
            ?: onComplete()
    }

    fun saveAnswer(matchId: String, sdp: String, onComplete: () -> Unit) {
        matchesRef?.child(matchId)?.child("answer")?.setValue(sdp)
            ?.addOnCompleteListener { onComplete() }
            ?: onComplete()
    }

    fun saveIceCandidate(matchId: String, role: String, candidate: String) {
        matchesRef?.child(matchId)?.child("iceCandidates")?.child(role)?.push()?.setValue(candidate)
    }

    fun listenForOffer(matchId: String, onOfferReceived: (String) -> Unit) {
        matchesRef?.child(matchId)?.child("offer")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(String::class.java)?.let { onOfferReceived(it) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForAnswer(matchId: String, onAnswerReceived: (String) -> Unit) {
        matchesRef?.child(matchId)?.child("answer")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(String::class.java)?.let { onAnswerReceived(it) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForIceCandidates(matchId: String, role: String, onCandidateReceived: (String) -> Unit) {
        matchesRef?.child(matchId)?.child("iceCandidates")?.child(role)
            ?.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot.getValue(String::class.java)?.let { onCandidateReceived(it) }
                }
                override fun onCancelled(error: DatabaseError) {}
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            })
    }

    companion object {
        const val MISSING_ERROR = "Firebase no está configurado en este build (falta google-services.json). El modo Internet no está disponible; usa Bluetooh."

        /** Código personalizado: 4-12 caracteres alfanuméricos (se usa en mayúsculas). */
        val CUSTOM_CODE_REGEX = Regex("^[A-Za-z0-9]{4,12}$")

        /** Sala sin actividad durante más de esto se considera huérfana y se libera. */
        const val STALE_MS = 12 * 60 * 60 * 1000L // 12 horas
    }
}
