package com.supernova.wheresmydevice.utils

/**
 * Helper class to mark code that should not be obfuscated by ProGuard
 * 
 * Note: This is only for code documentation. The actual ProGuard rules are defined in proguard-rules.pro
 */
object ProguardKeep {
    
    /**
     * Annotation to mark classes and methods that should not be obfuscated
     * This is a documentation annotation only - the actual rules are in proguard-rules.pro
     */
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.SOURCE)
    annotation class DoNotObfuscate
    
    /**
     * Annotation to mark classes that are accessed via reflection
     * This is a documentation annotation only - the actual rules are in proguard-rules.pro
     */
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.SOURCE)
    annotation class KeepForReflection
    
    /**
     * Logs a message to help developers understand what's safe to obfuscate
     */
    fun logProguardInfo() {
        // This method will be removed in release builds due to the ProGuard rule for Log
        android.util.Log.d("ProGuardKeep", "Critical Android components are kept from obfuscation")
        android.util.Log.d("ProGuardKeep", "BroadcastReceivers, Services, and Activities are preserved")
    }
} 