-keep class org.eclipse.kuksa.proto.v1.** { *; }
-keep class com.google.protobuf.** { *; }

# Generated data classes have to keep their copy method for reflection methods
-keepclassmembers class ** implements org.eclipse.kuksa.vsscore.model.VssNode { *; }
-keepnames class ** implements org.eclipse.kuksa.vsscore.model.VssNode { *; }
