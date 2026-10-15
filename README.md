# Android File Search Engine (Java + Threading)

This is an Android version of the File Search Engine mini project. It is designed for older Android phones including devices that support Android 6.0 (API 23) or newer.

**You do not need Android Studio to build the APK.** GitHub Actions can compile the project in the cloud after you upload these files to a GitHub repository.

## Features

- Java Android application
- Choose a folder using Android's system folder picker
- Search for a full or partial file name, case-insensitively
- Traverse nested folders
- Search work runs on a background executor thread
- Stop a running search
- No database and no app account required

## Project structure

```text
AndroidFileSearchEngine/
├── .github/
│   └── workflows/
│       └── build-apk.yml
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/example/filesearchengine/MainActivity.java
├── build.gradle
├── gradle.properties
├── settings.gradle
└── README.md
```

## Build the APK using GitHub (no Android Studio)

1. Download and extract this ZIP on your computer.
2. Sign in to https://github.com/.
3. Create a repository named `AndroidFileSearchEngine`.
4. In the empty repository, choose **Add file → Upload files**.
5. Upload the *contents* of this extracted folder, including the hidden `.github` folder. The `.github/workflows/build-apk.yml` file is essential.
   - If your browser uploader does not show hidden files/folders, use GitHub's web editor to create `.github/workflows/build-apk.yml` with the exact contents from this ZIP, or upload with Git.
6. Commit the files to the `main` branch.
7. Open the repository's **Actions** tab.
8. Select **Build Android APK** in the workflow list.
9. If it has not started automatically, click **Run workflow**, choose `main`, and confirm.
10. Wait for the workflow to finish with a green check.
11. Open the successful workflow run and scroll to **Artifacts**.
12. Download `FileSearchEngine-debug-apk`. Extract that downloaded artifact ZIP; inside is `app-debug.apk`.

GitHub Actions availability depends on your account's included minutes and repository settings. Public repositories can generally use standard GitHub-hosted runners subject to GitHub's current usage policies and limits.

## Install on Redmi 8A Dual

1. Transfer `app-debug.apk` to your phone, or open the repository's Actions run in your phone browser and download the artifact.
2. If you downloaded the artifact ZIP, extract it first using the phone's file manager.
3. Tap `app-debug.apk`.
4. If Android blocks it, open the prompt's Settings option and allow installation from that source (for example, Chrome or Files). Return and tap Install.
5. After installation, open **File Search Engine**.
6. Tap **Choose folder**, select the folder you want to search, and grant access.
7. Enter a file name or part of a name and tap **Search**.

Only install APKs you built yourself or trust. This project generates a debug APK for learning/testing, not a Play Store release.

## Troubleshooting

- **Actions failed:** Open the failed run and read the first red error. Confirm that the workflow file is at `.github/workflows/build-apk.yml` and that the project files are at the repository root, not nested in another project folder.
- **No Actions workflow appears:** Ensure the workflow file was uploaded and committed under the exact `.github/workflows/` path.
- **Install blocked:** Check Android's “Install unknown apps” setting for the app used to open the APK. Only enable it for a source you trust.
- **Folder selection/access:** Select a folder through the app's own Choose folder button. Android does not allow apps to silently scan every folder.
- **No matches:** Try a short part of a filename, and choose a folder that actually contains files.

## Mini-project write-up

**Aim:** To create an Android file search engine using Java multithreading.

**Algorithm:**
1. Launch the app.
2. Ask the user to choose a folder and enter a search keyword.
3. Run recursive folder traversal on a background thread.
4. Compare each file name with the keyword without case sensitivity.
5. Display matching file names and URIs.
6. Stop traversal if the user presses Stop or finish when all accessible subfolders are scanned.

**Concepts:** Java, Android Activity, event listeners, Storage Access Framework, recursive traversal, ExecutorService background threading, Handler-based UI updates, and exception handling.
