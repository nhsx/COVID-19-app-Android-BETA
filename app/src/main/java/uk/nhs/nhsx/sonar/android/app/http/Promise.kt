/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import uk.nhs.nhsx.sonar.android.app.http.Promise.State.Explanation
import uk.nhs.nhsx.sonar.android.app.http.Promise.State.Failed
import uk.nhs.nhsx.sonar.android.app.http.Promise.State.InProgress
import uk.nhs.nhsx.sonar.android.app.http.Promise.State.Succeeded
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.suspendCoroutine

class Promise<T : Any?> private constructor() {

    sealed class State<T> {
        class InProgress<T> : State<T>()
        data class Succeeded<T>(val value: T) : State<T>()
        data class Failed<T>(val reason: Explanation) : State<T>()
        data class Explanation(val message: String, val exception: Exception? = null, val code: Int? = null) {
            constructor(exception: Exception) : this(exception.message ?: "Exception", exception, null)
        }
    }

    private var state: State<T> = InProgress()

    val isInProgress get() = state is InProgress
    val isSuccess get() = state is Succeeded
    val isFailed get() = state is Failed

    val value
        get() =
            when (val s = state) {
                is Succeeded -> s.value
                else -> null
            }

    val error
        get() =
            when (val s = state) {
                is Failed -> s.reason
                else -> null
            }

    private class Callback<U : Any?>(val f: (U) -> Unit) {
        private val triggered = AtomicBoolean(false)

        fun trigger(value: U) {
            if (triggered.compareAndSet(false, true)) {
                f(value)
            }
        }
    }

    private val successCallbacks = mutableListOf<Callback<T>>()
    private val errorCallbacks = mutableListOf<Callback<Explanation>>()

    fun onSuccess(function: (T) -> Unit): Promise<T> {
        successCallbacks.add(Callback(function))
        trigger()
        return this
    }

    fun onError(function: (Explanation) -> Unit): Promise<T> {
        errorCallbacks.add(Callback(function))
        trigger()
        return this
    }

    fun <U : Any?> map(mapping: (T) -> U): Promise<U> {
        val deferred = Deferred<U>()
        onSuccess { deferred.resolve(mapping(it)) }
        onError { deferred.fail(it) }
        return deferred.promise
    }

    fun mapToUnit(): Promise<Unit> =
        map {}

    suspend fun toCoroutine(): T =
        suspendCoroutine { continuation ->
            this
                .onSuccess { continuation.resumeWith(Result.success(it)) }
                .onError {
                    val exception = it.exception ?: IllegalStateException(it.message)
                    continuation.resumeWith(Result.failure(exception))
                }
        }

    private fun trigger() {
        when (val s = state) {
            is Succeeded -> {
                successCallbacks.forEach {
                    it.trigger(s.value)
                }
            }
            is Failed -> {
                errorCallbacks.forEach {
                    it.trigger(s.reason)
                }
            }
        }
    }

    class Deferred<T : Any?> {

        val promise = Promise<T>()

        fun resolve(value: T) {
            promise.state = Succeeded(value)
            promise.trigger()
        }

        fun fail(reason: Explanation) {
            promise.state = Failed(reason)
            promise.trigger()
        }

        fun fail(exception: Exception) =
            fail(Explanation(exception))

        fun fail(message: String) =
            fail(Explanation(message))
    }
}
