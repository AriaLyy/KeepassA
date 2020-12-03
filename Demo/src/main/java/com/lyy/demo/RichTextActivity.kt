/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.demo

import android.os.Bundle
import android.util.Log
import com.arialyy.frame.core.AbsActivity
import com.lyy.demo.databinding.ActivityRichtextBinding
import com.zzhoujay.richtext.RichText

class RichTextActivity : AbsActivity<ActivityRichtextBinding>() {

  val str1 = "# Looking for new maintainer!\n" +
      "\n" +
      "\n" +
      "# Android PdfViewer\n" +
      "\n" +
      "__AndroidPdfViewer 1.x is available on [AndroidPdfViewerV1](https://github.com/barteksc/AndroidPdfViewerV1)\n" +
      "repo, where can be developed independently. Version 1.x uses different engine for drawing document on canvas,\n" +
      "so if you don't like 2.x version, try 1.x.__\n" +
      "\n" +
      "Library for displaying PDF documents on Android, with `animations`, `gestures`, `zoom` and `double tap` support.\n" +
      "It is based on [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid) for decoding PDF files. Works on API 11 (Android 3.0) and higher.\n" +
      "Licensed under Apache License 2.0.\n" +
      "\n" +
      "## What's new in 3.2.0-beta.1?\n" +
      "* Merge PR #714 with optimized page load\n" +
      "* Merge PR #776 with fix for max & min zoom level\n" +
      "* Merge PR #722 with fix for showing right position when view size changed\n" +
      "\n" +
      "## Changes in 3.0 API\n" +
      "* Replaced `Contants.PRELOAD_COUNT` with `PRELOAD_OFFSET`\n" +
      "```\n" +
      "Copyright 2017 Bartosz Schiller\n" +
      "\n" +
      "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
      "you may not use this file except in compliance with the License.\n" +
      "You may obtain a copy of the License at\n" +
      "\n" +
      "    http://www.apache.org/licenses/LICENSE-2.0\n" +
      "\n" +
      "Unless required by applicable law or agreed to in writing, software\n" +
      "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
      "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
      "See the License for the specific language governing permissions and\n" +
      "limitations under the License.\n" +
      "```"

  val str = "\n" +
      "# Looking for new maintainer!\n" +
      "\n" +
      "\n" +
      "# Android PdfViewer\n" +
      "\n" +
      "__AndroidPdfViewer 1.x is available on [AndroidPdfViewerV1](https://github.com/barteksc/AndroidPdfViewerV1)\n" +
      "repo, where can be developed independently. Version 1.x uses different engine for drawing document on canvas,\n" +
      "so if you don't like 2.x version, try 1.x.__\n" +
      "\n" +
      "Library for displaying PDF documents on Android, with `animations`, `gestures`, `zoom` and `double tap` support.\n" +
      "It is based on [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid) for decoding PDF files. Works on API 11 (Android 3.0) and higher.\n" +
      "Licensed under Apache License 2.0.\n" +
      "\n" +
      "## What's new in 3.2.0-beta.1?\n" +
      "* Merge PR #714 with optimized page load\n" +
      "* Merge PR #776 with fix for max & min zoom level\n" +
      "* Merge PR #722 with fix for showing right position when view size changed\n" +
      "* Merge PR #703 with fix for too many threads\n" +
      "* Merge PR #702 with fix for memory leak\n" +
      "* Merge PR #689 with possibility to disable long click\n" +
      "* Merge PR #628 with fix for hiding scroll handle\n" +
      "* Merge PR #627 with `fitEachPage` option\n" +
      "* Merge PR #638 and #406 with fixed NPE\n" +
      "* Merge PR #780 with README fix\n" +
      "* Update compile SDK and support library to 28\n" +
      "* Update Gradle and Gradle Plugin\n" +
      "\n" +
      "## Changes in 3.0 API\n" +
      "* Replaced `Contants.PRELOAD_COUNT` with `PRELOAD_OFFSET`\n" +
      "* Removed `PDFView#fitToWidth()` (variant without arguments)\n" +
      "* Removed `Configurator#invalidPageColor(int)` method as invalid pages are not rendered\n" +
      "* Removed page size parameters from `OnRenderListener#onInitiallyRendered(int)` method, as document may have different page sizes\n" +
      "* Removed `PDFView#setSwipeVertical()` method\n" +
      "\n" +
      "## Installation\n" +
      "\n" +
      "Add to _build.gradle_:\n" +
      "\n" +
      "`implementation 'com.github.barteksc:android-pdf-viewer:3.2.0-beta.1'`\n" +
      "\n" +
      "or if you want to use more stable version:\n" +
      " \n" +
      "`implementation 'com.github.barteksc:android-pdf-viewer:2.8.2'`\n" +
      "\n" +
      "Library is available in jcenter repository, probably it'll be in Maven Central soon.\n" +
      "\n" +
      "## ProGuard\n" +
      "If you are using ProGuard, add following rule to proguard config file:\n" +
      "\n" +
      "```proguard\n" +
      "-keep class com.shockwave.**\n" +
      "```\n" +
      "\n" +
      "## Include PDFView in your layout\n" +
      "\n" +
      "``` xml\n" +
      "<com.github.barteksc.pdfviewer.PDFView\n" +
      "        android:id=\"@+id/pdfView\"\n" +
      "        android:layout_width=\"match_parent\"\n" +
      "        android:layout_height=\"match_parent\"/>\n" +
      "```\n" +
      "\n" +
      "## Load a PDF file\n" +
      "\n" +
      "All available options with default values:\n" +
      "``` java\n" +
      "pdfView.fromUri(Uri)\n" +
      "or\n" +
      "pdfView.fromFile(File)\n" +
      "or\n" +
      "pdfView.fromBytes(byte[])\n" +
      "or\n" +
      "pdfView.fromStream(InputStream) // stream is written to bytearray - native code cannot use Java Streams\n" +
      "or\n" +
      "pdfView.fromSource(DocumentSource)\n" +
      "or\n" +
      "pdfView.fromAsset(String)\n" +
      "    .pages(0, 2, 1, 3, 3, 3) // all pages are displayed by default\n" +
      "    .enableSwipe(true) // allows to block changing pages using swipe\n" +
      "    .swipeHorizontal(false)\n" +
      "    .enableDoubletap(true)\n" +
      "    .defaultPage(0)\n" +
      "    // allows to draw something on the current page, usually visible in the middle of the screen\n" +
      "    .onDraw(onDrawListener)\n" +
      "    // allows to draw something on all pages, separately for every page. Called only for visible pages\n" +
      "    .onDrawAll(onDrawListener)\n" +
      "    .onLoad(onLoadCompleteListener) // called after document is loaded and starts to be rendered\n" +
      "    .onPageChange(onPageChangeListener)\n" +
      "    .onPageScroll(onPageScrollListener)\n" +
      "    .onError(onErrorListener)\n" +
      "    .onPageError(onPageErrorListener)\n" +
      "    .onRender(onRenderListener) // called after document is rendered for the first time\n" +
      "    // called on single tap, return true if handled, false to toggle scroll handle visibility\n" +
      "    .onTap(onTapListener)\n" +
      "    .onLongPress(onLongPressListener)\n" +
      "    .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)\n" +
      "    .password(null)\n" +
      "    .scrollHandle(null)\n" +
      "    .enableAntialiasing(true) // improve rendering a little bit on low-res screens\n" +
      "    // spacing between pages in dp. To define spacing color, set view background\n" +
      "    .spacing(0)\n" +
      "    .autoSpacing(false) // add dynamic spacing to fit each page on its own on the screen\n" +
      "    .linkHandler(DefaultLinkHandler)\n" +
      "    .pageFitPolicy(FitPolicy.WIDTH) // mode to fit pages in the view\n" +
      "    .fitEachPage(false) // fit each page to the view, else smaller pages are scaled relative to largest page.\n" +
      "    .pageSnap(false) // snap pages to screen boundaries\n" +
      "    .pageFling(false) // make a fling change only a single page like ViewPager\n" +
      "    .nightMode(false) // toggle night mode\n" +
      "    .load();\n" +
      "```\n" +
      "\n" +
      "* `pages` is optional, it allows you to filter and order the pages of the PDF as you need\n" +
      "\n" +
      "## Scroll handle\n" +
      "\n" +
      "Scroll handle is replacement for **ScrollBar** from 1.x branch.\n" +
      "\n" +
      "From version 2.1.0 putting **PDFView** in **RelativeLayout** to use **ScrollHandle** is not required, you can use any layout.\n" +
      "\n" +
      "To use scroll handle just register it using method `Configurator#scrollHandle()`.\n" +
      "This method accepts implementations of **ScrollHandle** interface.\n" +
      "\n" +
      "There is default implementation shipped with AndroidPdfViewer, and you can use it with\n" +
      "`.scrollHandle(new DefaultScrollHandle(this))`.\n" +
      "**DefaultScrollHandle** is placed on the right (when scrolling vertically) or on the bottom (when scrolling horizontally).\n" +
      "By using constructor with second argument (`new DefaultScrollHandle(this, true)`), handle can be placed left or top.\n" +
      "\n" +
      "You can also create custom scroll handles, just implement **ScrollHandle** interface.\n" +
      "All methods are documented as Javadoc comments on interface [source](https://github.com/barteksc/AndroidPdfViewer/tree/master/android-pdf-viewer/src/main/java/com/github/barteksc/pdfviewer/scroll/ScrollHandle.java).\n" +
      "\n" +
      "## Document sources\n" +
      "Version 2.3.0 introduced _document sources_, which are just providers for PDF documents.\n" +
      "Every provider implements **DocumentSource** interface.\n" +
      "Predefined providers are available in **com.github.barteksc.pdfviewer.source** package and can be used as\n" +
      "samples for creating custom ones.\n" +
      "\n" +
      "Predefined providers can be used with shorthand methods:\n" +
      "```\n" +
      "pdfView.fromUri(Uri)\n" +
      "pdfView.fromFile(File)\n" +
      "pdfView.fromBytes(byte[])\n" +
      "pdfView.fromStream(InputStream)\n" +
      "pdfView.fromAsset(String)\n" +
      "```\n" +
      "Custom providers may be used with `pdfView.fromSource(DocumentSource)` method.\n" +
      "\n" +
      "## Links\n" +
      "Version 3.0.0 introduced support for links in PDF documents. By default, **DefaultLinkHandler**\n" +
      "is used and clicking on link that references page in same document causes jump to destination page\n" +
      "and clicking on link that targets some URI causes opening it in default application.\n" +
      "\n" +
      "You can also create custom link handlers, just implement **LinkHandler** interface and set it using\n" +
      "`Configurator#linkHandler(LinkHandler)` method. Take a look at [DefaultLinkHandler](https://github.com/barteksc/AndroidPdfViewer/tree/master/android-pdf-viewer/src/main/java/com/github/barteksc/pdfviewer/link/DefaultLinkHandler.java)\n" +
      "source to implement custom behavior.\n" +
      "\n" +
      "## Pages fit policy\n" +
      "Since version 3.0.0, library supports fitting pages into the screen in 3 modes:\n" +
      "* WIDTH - width of widest page is equal to screen width\n" +
      "* HEIGHT - height of highest page is equal to screen height\n" +
      "* BOTH - based on widest and highest pages, every page is scaled to be fully visible on screen\n" +
      "\n" +
      "Apart from selected policy, every page is scaled to have size relative to other pages.\n" +
      "\n" +
      "Fit policy can be set using `Configurator#pageFitPolicy(FitPolicy)`. Default policy is **WIDTH**.\n" +
      "\n" +
      "## Additional options\n" +
      "\n" +
      "### Bitmap quality\n" +
      "By default, generated bitmaps are _compressed_ with `RGB_565` format to reduce memory consumption.\n" +
      "Rendering with `ARGB_8888` can be forced by using `pdfView.useBestQuality(true)` method.\n" +
      "\n" +
      "### Double tap zooming\n" +
      "There are three zoom levels: min (default 1), mid (default 1.75) and max (default 3). On first double tap,\n" +
      "view is zoomed to mid level, on second to max level, and on third returns to min level.\n" +
      "If you are between mid and max levels, double tapping causes zooming to max and so on.\n" +
      "\n" +
      "Zoom levels can be changed using following methods:\n" +
      "\n" +
      "``` java\n" +
      "void setMinZoom(float zoom);\n" +
      "void setMidZoom(float zoom);\n" +
      "void setMaxZoom(float zoom);\n" +
      "```\n" +
      "\n" +
      "## Possible questions\n" +
      "### Why resulting apk is so big?\n" +
      "Android PdfViewer depends on PdfiumAndroid, which is set of native libraries (almost 16 MB) for many architectures.\n" +
      "Apk must contain all this libraries to run on every device available on market.\n" +
      "Fortunately, Google Play allows us to upload multiple apks, e.g. one per every architecture.\n" +
      "There is good article on automatically splitting your application into multiple apks,\n" +
      "available [here](http://ph0b.com/android-studio-gradle-and-ndk-integration/).\n" +
      "Most important section is _Improving multiple APKs creation and versionCode handling with APK Splits_, but whole article is worth reading.\n" +
      "You only need to do this in your application, no need for forking PdfiumAndroid or so.\n" +
      "\n" +
      "### Why I cannot open PDF from URL?\n" +
      "Downloading files is long running process which must be aware of Activity lifecycle, must support some configuration, \n" +
      "data cleanup and caching, so creating such module will probably end up as new library.\n" +
      "\n" +
      "### How can I show last opened page after configuration change?\n" +
      "You have to store current page number and then set it with `pdfView.defaultPage(page)`, refer to sample app\n" +
      "\n" +
      "### How can I fit document to screen width (eg. on orientation change)?\n" +
      "Use `FitPolicy.WIDTH` policy or add following snippet when you want to fit desired page in document with different page sizes:\n" +
      "``` java\n" +
      "Configurator.onRender(new OnRenderListener() {\n" +
      "    @Override\n" +
      "    public void onInitiallyRendered(int pages, float pageWidth, float pageHeight) {\n" +
      "        pdfView.fitToWidth(pageIndex);\n" +
      "    }\n" +
      "});\n" +
      "```\n" +
      "\n" +
      "### How can I scroll through single pages like a ViewPager?\n" +
      "You can use a combination of the following settings to get scroll and fling behaviour similar to a ViewPager:\n" +
      "``` java\n" +
      "    .swipeHorizontal(true)\n" +
      "    .pageSnap(true)\n" +
      "    .autoSpacing(true)\n" +
      "    .pageFling(true)\n" +
      "```\n" +
      "\n" +
      "## One more thing\n" +
      "If you have any suggestions on making this lib better, write me, create issue or write some code and send pull request.\n" +
      "\n" +
      "## License\n" +
      "\n" +
      "Created with the help of android-pdfview by [Joan Zapata](http://joanzapata.com/)\n" +
      "```\n" +
      "Copyright 2017 Bartosz Schiller\n" +
      "\n" +
      "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
      "you may not use this file except in compliance with the License.\n" +
      "You may obtain a copy of the License at\n" +
      "\n" +
      "    http://www.apache.org/licenses/LICENSE-2.0\n" +
      "\n" +
      "Unless required by applicable law or agreed to in writing, software\n" +
      "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
      "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
      "See the License for the specific language governing permissions and\n" +
      "limitations under the License.\n" +
      "```"

  override fun setLayoutId(): Int {
    return R.layout.activity_richtext
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
//    RichText.initCacheDir(this)
    binding.tvContent.originalText = str1
//    RichText.fromMarkdown(str)
//        .urlClick { url ->
//          Log.d(TAG, "url: $url")
//          true
//        }
//        .into(binding.tvContent)
  }
}