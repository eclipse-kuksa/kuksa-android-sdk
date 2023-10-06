-keep class org.eclipse.kuksa.proto.v1.** { *; }
-keep class com.google.protobuf.** { *; }

# Generated classes
-keep class ** implements org.eclipse.kuksa.vsscore.model.VssProperty { *; }
-keep class ** implements org.eclipse.kuksa.vsscore.model.VssSpecification { *; }
