/**
 * Sealed class representing the result of a database sync operation.
 */
package com.safeanot.app.domain.model

sealed class SyncOutcome {
    object Success : SyncOutcome()
    object Retry : SyncOutcome()
    data class PermanentFailure(val error: String) : SyncOutcome()
}
