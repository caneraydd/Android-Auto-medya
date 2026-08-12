# Implementation Plan - Fix Duplicate Resources

The project is failing to build because of duplicate resource definitions for `ic_launcher`. Specifically, `ic_launcher` exists as both a `.png` and a `.webp` file in multiple density-specific mipmap folders. This conflict prevents the `mergeDebugResources` task from completing.

## User Review Required

> [!IMPORTANT]
> I will be removing the `.png` versions of the `ic_launcher` icon and keeping the `.webp` versions. WebP is a more modern, compressed format and appears to be what was intended for the project (as foreground and round versions are already in WebP).

## Proposed Changes

### Resource Cleanup

I will remove the following duplicate PNG files:

#### [DELETE] [ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-hdpi/ic_launcher.png)
#### [DELETE] [ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-mdpi/ic_launcher.png)
#### [DELETE] [ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-xhdpi/ic_launcher.png)
#### [DELETE] [ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-xxhdpi/ic_launcher.png)
#### [DELETE] [ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

## Verification Plan

### Automated Tests
- I will run `./gradlew :app:mergeDebugResources` (via `gradle_build`) to ensure the resource merging process now completes without errors.
- I will run a full build `:app:assembleDebug` to verify the final APK can be generated.

### Manual Verification
- None required as this is a build-time fix.
