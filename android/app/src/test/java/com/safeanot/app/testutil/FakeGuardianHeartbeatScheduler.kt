package com.safeanot.app.testutil

import com.safeanot.app.worker.GuardianHeartbeatScheduler

/**
 * Test fake for GuardianHeartbeatScheduler that avoids needing Android Context.
 * Uses Mockito to create instance without calling the real constructor.
 */
class FakeGuardianHeartbeatScheduler private constructor() {
    companion object {
        /**
         * Creates a no-op GuardianHeartbeatScheduler for tests using org.mockito.Mockito.
         * Falls back to reflection if Mockito is not available.
         */
        fun create(): GuardianHeartbeatScheduler {
            return try {
                val mockitoClass = Class.forName("org.mockito.Mockito")
                val mockMethod = mockitoClass.getMethod("mock", Class::class.java)
                @Suppress("UNCHECKED_CAST")
                mockMethod.invoke(null, GuardianHeartbeatScheduler::class.java) as GuardianHeartbeatScheduler
            } catch (_: Exception) {
                // Fallback: use sun.misc.Unsafe to allocate without calling constructor
                val unsafeClass = Class.forName("sun.misc.Unsafe")
                val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
                unsafeField.isAccessible = true
                val unsafe = unsafeField.get(null)
                val allocateMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
                allocateMethod.invoke(unsafe, GuardianHeartbeatScheduler::class.java) as GuardianHeartbeatScheduler
            }
        }
    }
}
