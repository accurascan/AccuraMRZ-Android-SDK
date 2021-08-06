# Accura MRZ Android SDK
Accura MRZ is used for Optical character recognition.<br/>

Below steps to setup Accura MRZ SDK's to your project.

## Install SDK in to your App

#### Step 1: Add the JitPack repository to your build file:
    Add it in your root build.gradle at the end of repositories.

    allprojects {
        repositories {
            ...
            maven {
                url 'https://jitpack.io'
                credentials { username authToken }
            }
        }
    }

#### Step 2. Add the token to `gradle.properties`:

    authToken=jp_9ldoc7h8fl5gbk4rsojgdiupa9

#### Step 3: Add the dependency:
    Set Accura MRZ SDK as a dependency to our app/build.gradle file.

    android {
        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
    }
    dependencies {
        implementation 'com.github.accurascan:AccuraMRZ-AndroidSDK:2.1.0'
    }

#### Step 4: Add files to project assets folder:

* Create "assets" folder under `app/src/main` and Add `key.license` file in to assets folder.

* Generate your Accura license from https://accurascan.com/developer/dashboard

## 1. Setup Accura MRZ
* Require `key.license` to implement Accura MRZ in to your app

#### Step 1 : To initialize sdk on app start:

    RecogEngine recogEngine = new RecogEngine();
    RecogEngine.SDKModel sdkModel = recogEngine.initEngine(your activity context);

    if (sdkModel.i > 0) { // means license is valid
         if (sdkModel.isMRZEnable) // True if MRZ option is selected while creating license
    }

##### Update filters like below.</br>
  Call this function after initialize sdk if license is valid(sdkModel.i > 0)
   * Set Blur Percentage to allow blur on document

        ```
		//0 for clean document and 100 for Blurry document
		recogEngine.setBlurPercentage(Context context, int blurPercentage/*52*/);
		```
   * Set Face blur Percentage to allow blur on detected Face

        ```
		// 0 for clean face and 100 for Blurry face
		recogEngine.setFaceBlurPercentage(Context context, int faceBlurPercentage/*70*/);
        ```
   * Set Glare Percentage to detect Glare on document

        ```
		// Set min and max percentage for glare
		recogEngine.setGlarePercentage(Context context, int /*minPercentage*/6, int /*maxPercentage*/98);
		```
   * Set Photo Copy to allow photocopy document or not

        ```
		// Set min and max percentage for glare
		recogEngine.isCheckPhotoCopy(Context context, boolean /*isCheckPhotoCopy*/false);
		```
   * Set Hologram detection to verify the hologram on the face

        ```
		// true to check hologram on face
		recogEngine.SetHologramDetection(Context context, boolean /*isDetectHologram*/true);
		```
   * Set light tolerance to detect light on document

        ```
        // 0 for full dark document and 100 for full bright document
        recogEngine.setLowLightTolerance(Context context, int /*tolerance*/39);
        ```
   * Set motion threshold to detect motion on camera document
		```
        // 1 - allows 1% motion on document and
        // 100 - it can not detect motion and allow document to scan.
        recogEngine.setMotionThreshold(Context context, int /*motionThreshold*/18);
        ```

#### Step 2 : Set CameraView
```
Must have to extend com.accurascan.ocr.mrz.motiondetection.SensorsActivity to your activity.
- Make sure your activity orientation locked from Manifest. Because auto rotate not support.

private CameraView cameraView;

@Override
public void onCreate(Bundle savedInstanceState) {
    if (isPortrait) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // to set portarait mode
    } else {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // to set landscape mode
    }
    super.onCreate(savedInstanceState);
    setContentView(R.layout.your_layout);

    // initialized camera
    initCamera();
}

private void initCamera() {
    //<editor-fold desc="To get status bar height">
    Rect rectangle = new Rect();
    Window window = getWindow();
    window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
    int statusBarTop = rectangle.top;
    int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
    int statusBarHeight = contentViewTop - statusBarTop;
    //</editor-fold>

    RelativeLayout linearLayout = findViewById(R.id.ocr_root); // layout width and height is match_parent

    cameraView = new CameraView(this);
   
    // Also set MRZ document type to scan specific MRZ document
    // 1. ALL MRZ document       - MRZDocumentType.NONE        
    // 2. Passport MRZ document  - MRZDocumentType.PASSPORT_MRZ
    // 3. ID card MRZ document   - MRZDocumentType.ID_CARD_MRZ 
    // 4. Visa MRZ document      - MRZDocumentType.VISA_MRZ    
    cameraView.setMRZDocumentType(MRZDocumentType.NONE);
    
    cameraView.setView(linearLayout) // To add camera view
            .setCameraFacing(0) // // To set selfie(1) or rear(0) camera.
            .setOcrCallback(this)  // To get feedback and Success Call back
            .setStatusBarHeight(statusBarHeight)  // To remove Height from Camera View if status bar visible
//                Option setup
//                .setEnableMediaPlayer(false) // false to disable default sound and true to enable sound and default it is true
//                .setCustomMediaPlayer(MediaPlayer.create(this, /*custom sound file*/)) // To add your custom sound and Must have to enable media player
            .init();  // initialized camera
}

/**
 * To handle camera on window focus update
 * @param hasFocus
 */
@Override
public void onWindowFocusChanged(boolean hasFocus) {
    if (cameraView != null) {
        cameraView.onWindowFocusUpdate(hasFocus);
    }
}

@Override
protected void onResume() {
    super.onResume();
    cameraView.onResume();
}

@Override
protected void onPause() {
    cameraView.onPause();
    super.onPause();
}

@Override
protected void onDestroy() {
    cameraView.onDestroy();
    super.onDestroy();
}

/**
 * To update your border frame according to width and height
 * it's different for different card
 * Call {@link CameraView#startOcrScan(boolean isReset)} To start Camera Preview
 * @param width    border layout width
 * @param height   border layout height
 */
@Override
public void onUpdateLayout(int width, int height) {
    if (cameraView != null) cameraView.startOcrScan(false);

    //<editor-fold desc="To set camera overlay Frame">
    ViewGroup.LayoutParams layoutParams = borderFrame.getLayoutParams();
    layoutParams.width = width;
    layoutParams.height = height;
    borderFrame.setLayoutParams(layoutParams);

    ViewGroup.LayoutParams lpRight = viewRight.getLayoutParams();
    lpRight.height = height;
    viewRight.setLayoutParams(lpRight);

    ViewGroup.LayoutParams lpLeft = viewLeft.getLayoutParams();
    lpLeft.height = height;
    viewLeft.setLayoutParams(lpLeft);
    //</editor-fold>
}

/**
 * Override this method after scan complete to get data from document
 *
 * @param result is scanned card data
 *
 */
@Override
public void onScannedComplete(RecogResult result) {
    // display data on ui thread
    Log.e("TAG", "onScannedComplete: ");
    if (result != null) {
        RecogResult.setRecogResult((RecogResult) result);
    } else Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
}

/**
 * @param titleCode to display scan card message on top of border Frame
 *
 * @param errorMessage To display process message.
 *                null if message is not available
 * @param isFlip  true to set your customize animation for scan back card alert after complete front scan
 *                and also used cameraView.flipImage(ImageView) for default animation
 */
@Override
public void onProcessUpdate(int titleCodetitleCode, String errorMessage, boolean isFlip) {
	// Put UI thread to update UI elements
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
          if (getTitleMessage(titleCode) != null) { // check
              Toast.makeText(this, getTitleMessage(titleCode), Toast.LENGTH_SHORT).show(); // display title
          }
          if (message != null) {
              Toast.makeText(this, getErrorMessage(message), Toast.LENGTH_SHORT).show(); // display message
          }
          if (isFlip) {
          // To set default animation or remove this line to set your custom animation after successfully scan front side.
              CameraView.flipImage(imageView);
          }
      }
	});
}

@Override
public void onError(String errorMessage) {
    // display data on ui thread
    // stop ocr if failed
    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
}

private String getTitleMessage(int titleCode) {
    if (titleCode < 0) return null;
    switch (titleCode){
        case RecogEngine.SCAN_TITLE_MRZ_FRONT:
            return "Scan Front Side of Document";
        case RecogEngine.SCAN_TITLE_MRZ_BACK:
            return "Now Scan Back Side of Document";
        default:return "";
    }
}

private String getErrorMessage(String s) {
    switch (s) {
        case RecogEngine.ACCURA_ERROR_CODE_MOTION:
            return "Keep Document Steady";
        case RecogEngine.ACCURA_ERROR_CODE_PROCESSING:
            return "Processing...";
        case RecogEngine.ACCURA_ERROR_CODE_BLUR_DOCUMENT:
            return "Blur detect in document";
        case RecogEngine.ACCURA_ERROR_CODE_FACE_BLUR:
            return "Blur detected over face";
        case RecogEngine.ACCURA_ERROR_CODE_GLARE_DOCUMENT:
            return "Glare detect in document";
        case RecogEngine.ACCURA_ERROR_CODE_HOLOGRAM:
            return "Hologram Detected";
        case RecogEngine.ACCURA_ERROR_CODE_DARK_DOCUMENT:
            return "Low lighting detected";
        case RecogEngine.ACCURA_ERROR_CODE_PHOTO_COPY_DOCUMENT:
            return "Can not accept Photo Copy Document";
        case RecogEngine.ACCURA_ERROR_CODE_FACE:
            return "Face not detected";
        case RecogEngine.ACCURA_ERROR_CODE_MRZ:
            return "MRZ not detected";
        case RecogEngine.ACCURA_ERROR_CODE_PASSPORT_MRZ:
            return "Passport MRZ not detected";
        case RecogEngine.ACCURA_ERROR_CODE_ID_MRZ:
            return "ID card MRZ not detected";
        case RecogEngine.ACCURA_ERROR_CODE_VISA_MRZ:
            return "Visa MRZ not detected";
        default:
            return s;
    }
}

// After getting result to restart scanning you have to set below code onActivityResult
// when you use startActivityForResult(Intent, RESULT_ACTIVITY_CODE) to open result activity.
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    ...
    if (resultCode == RESULT_OK) {
        if (requestCode == RESULT_ACTIVITY_CODE) {
            //<editor-fold desc="Call CameraView#startOcrScan(true) to scan document again">

            if (cameraView != null) cameraView.startOcrScan(true);

            //</editor-fold>
        }
    }
}
```

## ProGuard

Depending on your ProGuard (DexGuard) config and usage, you may need to include the following lines in your proguards.

```
-keep class com.accurascan.ocr.mrz.model.* {;}
-keep class com.accurascan.ocr.mrz.interfaces.* {;}
```
