File Structure of this file  


AllReader/
│
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   │
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │
│   │   │   ├── java/
│   │   │   │   └── com/allreader/
│   │   │   │       ├── MainApplication.java
│   │   │   │       │
│   │   │   │       ├── ui/
│   │   │   │       │   ├── activities/
│   │   │   │       │   │   ├── MainActivity.java
│   │   │   │       │   │   ├── PdfReaderActivity.java
│   │   │   │       │   │   ├── EpubReaderActivity.java
│   │   │   │       │   │   ├── ExcelReaderActivity.java
│   │   │   │       │   │   ├── ImageViewerActivity.java
│   │   │   │       │   │
│   │   │   │       │   ├── fragments/
│   │   │   │       │   │   ├── HomeFragment.java
│   │   │   │       │   │   ├── FilePickerFragment.java
│   │   │   │       │   │   ├── SettingsFragment.java
│   │   │   │       │   │
│   │   │   │       │   └── adapters/
│   │   │   │       │       ├── FileListAdapter.java
│   │   │   │       │       ├── RecentFilesAdapter.java
│   │   │   │
│   │   │   │       ├── data/
│   │   │   │       │   ├── model/
│   │   │   │       │   │   ├── FileItem.java
│   │   │   │       │   │   ├── RecentFile.java
│   │   │   │       │   │
│   │   │   │       │   ├── repository/
│   │   │   │       │   │   ├── FileRepository.java
│   │   │   │       │   │
│   │   │   │       │   └── local/
│   │   │   │       │       ├── AppDatabase.java
│   │   │   │       │       ├── dao/
│   │   │   │       │           ├── FileDao.java
│   │   │   │
│   │   │   │       ├── utils/
│   │   │   │       │   ├── FileUtils.java
│   │   │   │       │   ├── PermissionUtils.java
│   │   │   │       │   ├── Constants.java
│   │   │   │
│   │   │   │       ├── viewmodel/
│   │   │   │       │   ├── FileViewModel.java
│   │   │   │       │   ├── ReaderViewModel.java
│   │   │   │
│   │   │   │       └── readers/
│   │   │   │           ├── PdfReader.java
│   │   │   │           ├── EpubReader.java
│   │   │   │           ├── ExcelReader.java
│   │   │   │           ├── ImageReader.java
│   │   │
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_pdf_reader.xml
│   │   │   │   │   ├── activity_epub_reader.xml
│   │   │   │   │   ├── activity_excel_reader.xml
│   │   │   │   │   ├── activity_image_viewer.xml
│   │   │   │   │   ├── fragment_home.xml
│   │   │   │   │   ├── fragment_file_picker.xml
│   │   │   │   │   ├── fragment_settings.xml
│   │   │   │   │   ├── item_file.xml
│   │   │   │
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── app_icon.xml
│   │   │   │   │   ├── background.xml
│   │   │   │
│   │   │   │   ├── mipmap/
│   │   │   │   │   ├── ic_launcher.png
│   │   │   │
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── styles.xml
│   │   │   │
│   │   │   └── assets/
│   │   │       ├── sample.pdf
│   │   │       ├── sample.epub
│   │   │       ├── fonts/
│   │   │           ├── Roboto-Regular.ttf
│   │   │
│   │   ├── androidTest/
│   │   └── test/
│   │
│   └── build.gradle
│
├── gradle/
│   ├── wrapper/
│   │   └── gradle-wrapper.properties
│
├── build.gradle
└── settings.gradle
