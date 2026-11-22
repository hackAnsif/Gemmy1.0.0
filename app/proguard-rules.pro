# Add project specific ProGuard rules here.
# Keep Room database, entities, and broadcast receivers intact during R8 minification.

-keep class com.example.data.model.** { *; }
-keep class com.example.data.db.** { *; }
-keep class com.example.receiver.** { *; }
-keep class com.example.widget.** { *; }
