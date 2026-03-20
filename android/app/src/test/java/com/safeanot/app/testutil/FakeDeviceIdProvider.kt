package com.safeanot.app.testutil

import com.safeanot.app.util.DeviceIdProvider

/**
 * Test fake for DeviceIdProvider that avoids needing Android Context.
 * Uses Mockito to create an instance without calling the real constructor,
 * then overrides getOrCreateDeviceId() to return a fixed test device ID.
 */
class FakeDeviceIdProvider private constructor() {
    companion object {
        /**
         * Creates a DeviceIdProvider that returns "test-device-id"
         * without requiring Android Context.
         */
        fun create(): DeviceIdProvider {
            return try {
                val mockitoClass = Class.forName("org.mockito.Mockito")
                val mockMethod = mockitoClass.getMethod("mock", Class::class.java)
                val mockAnswer = Class.forName("org.mockito.stubbing.Answer")

                // Create a mock with default answer that returns "test-device-id" for getOrCreateDeviceId
                @Suppress("UNCHECKED_CAST")
                val mock = mockMethod.invoke(null, DeviceIdProvider::class.java) as DeviceIdProvider

                // Use Mockito.when().thenReturn() to stub the method
                val whenMethod = mockitoClass.getMethod("when", Any::class.java)
                val ongoingStubbing = whenMethod.invoke(null, mock.getOrCreateDeviceId())
                val thenReturnMethod = ongoingStubbing.javaClass.getMethod("thenReturn", Any::class.java)
                thenReturnMethod.invoke(ongoingStubbing, "test-device-id")

                mock
            } catch (_: Exception) {
                // Fallback: use sun.misc.Unsafe to allocate without calling constructor
                val unsafeClass = Class.forName("sun.misc.Unsafe")
                val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
                unsafeField.isAccessible = true
                val unsafe = unsafeField.get(null)
                val allocateMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
                allocateMethod.invoke(unsafe, DeviceIdProvider::class.java) as DeviceIdProvider
            }
        }
    }
}
