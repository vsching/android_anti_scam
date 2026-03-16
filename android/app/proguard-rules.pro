# Safe Anot? ProGuard Rules

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Room entities
-keep class com.safeanot.app.data.local.entity.** { *; }

# Keep domain models (used by Room type converters)
-keep class com.safeanot.app.domain.model.** { *; }
