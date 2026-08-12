# Fix Duplicate Resources Error

The build is failing with "Duplicate resources" error because both `.png` and `.webp` versions of `ic_launcher` exist in the `mipmap` resource folders. Since other launcher resources (`ic_launcher_round`, `ic_launcher_foreground`) are already using the `.webp` format, I will remove the duplicate `.png` versions of `ic_launcher` to resolve the conflict and maintain consistency.

## Proposed Changes

### app module resources

#### [DELETE] `ic_launcher.png` in mipmap folders
I will delete the following files:
- [mipmap-hdpi/ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-hdpi/ic_launcher.png)
- [mipmap-mdpi/ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-mdpi/ic_launcher.png)
- [mipmap-xhdpi/ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-xhdpi/ic_launcher.png)
- [mipmap-xxhdpi/ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-xxhdpi/ic_launcher.png)
- [mipmap-xxxhdpi/ic_launcher.png](file:///C:/Users/Caner/Desktop/AutoMedya/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

## Verification Plan

### Automated Tests
- Run `./gradlew :app:packageDebugResources` to verify that the duplicate resource error is resolved and the build task succeeds.
- Run a full build `./gradlew assembleDebug` to ensure no other resource issues exist.
