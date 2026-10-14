/*
 * Copyright (c) 2022-2026 NOISIF. All Rights Reserved.
 *
 * NOTICE: This source code is publicly available for reference
 * and educational purposes only. It is NOT open-source software.
 *
 * You are granted permission to view this code. However, you are strictly
 * PROHIBITED from copying, modifying, or merging this code into other software,
 * distributing, publishing, or sublicensing this code, using this code for
 * commercial purposes or in production environments.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * Please refer to the LICENSE file in the root directory for full restrictions.
 */
package xyz.noisif.buildconfig.spa.teavm

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

internal abstract class ProcessFrontendTask : DefaultTask() {
  @get:InputFile
  abstract val inputJs: RegularFileProperty

  @get:InputFile
  @get:Optional
  abstract val htmlTemplate: RegularFileProperty

  @get:Input
  abstract val pageTitle: Property<String>

  @get:Input
  abstract val minify: Property<Boolean>

  @get:Input
  abstract val generateHash: Property<Boolean>

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val spaResourceDirs: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  private companion object {
    val CSS_ORDER_REGEX = Regex("^(\\d+)\\..+\\.css$", RegexOption.IGNORE_CASE)
  }

  @TaskAction
  fun process() {
    val outDir = prepareOutputDir()
    val useHash = generateHash.get()
    val shouldMinify = minify.get()
    val timestamp = System.currentTimeMillis()

    val scriptSrc = processJavaScript(outDir, useHash, timestamp)
    val styleHref = processCssBundle(outDir, useHash, shouldMinify, timestamp)

    buildAndWriteHtml(outDir, scriptSrc, styleHref, shouldMinify)
  }

  private fun prepareOutputDir(): File {
    val outDir = outputDir.get().asFile
    if (outDir.exists()) {
      outDir.deleteRecursively()
    }
    outDir.mkdirs()
    return outDir
  }

  private fun processJavaScript(outDir: File, useHash: Boolean, timestamp: Long): String {
    val jsFile = inputJs.get().asFile
    if (!jsFile.exists()) {
      throw IllegalStateException("File $TARGET_FILE_NAME.js not found at: ${jsFile.absolutePath}")
    }
    val scriptSrc = resolveAssetReference(
      extension = "js",
      bytes = jsFile.readBytes(),
      useHash = useHash,
      timestamp = timestamp,
    )
    val finalJsName = scriptSrc.substringBefore("?")
    jsFile.copyTo(File(outDir, finalJsName), overwrite = true)
    return scriptSrc
  }

  private fun processCssBundle(
    outDir: File,
    useHash: Boolean,
    shouldMinify: Boolean,
    timestamp: Long,
  ): String? {
    val cssEntries = collectCssEntries()
    if (cssEntries.isEmpty()) {
      return null
    }
    val sortedEntries = cssEntries.sortedWith(
      compareBy<CssEntry> { it.order }
        .thenBy { it.sourcePriority }
        .thenBy { it.relativePath },
    )
    var combinedCss = sortedEntries.joinToString("\n") { it.content }
    if (shouldMinify) {
      combinedCss = minifyCss(combinedCss)
    }
    val cssBytes = combinedCss.toByteArray(Charsets.UTF_8)
    val styleHref = resolveAssetReference(
      extension = "css",
      bytes = cssBytes,
      useHash = useHash,
      timestamp = timestamp,
    )
    val finalCssName = styleHref.substringBefore("?")
    File(outDir, finalCssName).writeBytes(cssBytes)
    return styleHref
  }

  private fun collectCssEntries(): List<CssEntry> = spaResourceDirs.files
    .filter { it.exists() && it.isDirectory }
    .flatMapIndexed { index, spaDir ->
      spaDir.walkTopDown()
        .filter { it.isFile && it.extension.equals("css", ignoreCase = true) }
        .map { file ->
          CssEntry(
            relativePath = file.relativeTo(spaDir).invariantSeparatorsPath,
            order = extractOrderFromFileName(file.name),
            sourcePriority = index,
            content = file.readText(Charsets.UTF_8),
          )
        }
    }

  private fun extractOrderFromFileName(fileName: String): Int {
    val match = CSS_ORDER_REGEX.matchEntire(fileName)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
  }

  private fun buildAndWriteHtml(
    outDir: File,
    scriptSrc: String,
    styleHref: String?,
    shouldMinify: Boolean,
  ) {
    var html = loadHtmlTemplate().replace("%TITLE%", pageTitle.get())
    if (styleHref != null) {
      html = injectStylesheet(html, styleHref)
    }
    html = injectScripts(html, scriptSrc)
    if (shouldMinify) {
      html = minifyHtml(html)
    }
    File(outDir, INDEX_HTML).writeText(html)
  }

  private fun loadHtmlTemplate(): String {
    val templateFile = htmlTemplate.orNull?.asFile
    if (templateFile != null && templateFile.exists()) {
      return templateFile.readText()
    }
    val defaultTemplateFile = spaResourceDirs.files
      .map { dir -> if (dir.name == SPA_DIR) dir else File(dir, SPA_DIR) }
      .map { File(it, INDEX_HTML) }
      .firstOrNull { it.exists() && it.isFile }

    if (defaultTemplateFile != null) {
      return defaultTemplateFile.readText(Charsets.UTF_8)
    }
    throw IllegalStateException("Not found file $INDEX_HTML file in classpath resources!")
  }

  private fun injectStylesheet(html: String, styleHref: String): String {
    val styleSnippet = """<link rel="stylesheet" href="/$styleHref" nonce="%NONCE%">"""
    return if (html.contains("</head>", ignoreCase = true)) {
      html.replaceFirst(
        Regex("</head>", RegexOption.IGNORE_CASE),
        "  $styleSnippet\n</head>",
      )
    } else {
      "$styleSnippet\n$html"
    }
  }

  private fun injectScripts(html: String, scriptSrc: String): String {
    val scriptsSnippet = """
      <script src="/$scriptSrc" nonce="%NONCE%"></script>
      <script nonce="%NONCE%">
        window.onload = function () {
          main();
        }
      </script>
    """.trimIndent()
    return if (html.contains("</body>", ignoreCase = true)) {
      html.replaceFirst(
        Regex("</body>", RegexOption.IGNORE_CASE),
        "$scriptsSnippet\n</body>",
      )
    } else {
      "$html\n$scriptsSnippet"
    }
  }

  private fun resolveAssetReference(
    extension: String,
    bytes: ByteArray,
    useHash: Boolean,
    timestamp: Long,
  ): String = if (useHash) {
    val hash = md5(bytes).take(8)
    "$TARGET_FILE_NAME-$hash.$extension"
  } else {
    "$TARGET_FILE_NAME.$extension?t=$timestamp"
  }

  private fun md5(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("MD5")
    return md.digest(bytes).joinToString("") { "%02x".format(it) }
  }

  private fun minifyHtml(htmlContent: String): String = htmlContent
    .replace(Regex("<!--[\\s\\S]*?copyright[\\s\\S]*?-->", RegexOption.IGNORE_CASE), "")
    .replace(Regex(">\\s+<"), "><")
    .replace(Regex("[\\r\\n]+"), " ")
    .replace(Regex("\\s{2,}"), " ")
    .trim()

  private fun minifyCss(cssContent: String): String = cssContent
    .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
    .replace(Regex("\\s*([{}:;,>~+])\\s*"), "$1")
    .replace(";}", "}")
    .replace(Regex("[\\r\\n]+"), "")
    .replace(Regex("\\s{2,}"), " ")
    .trim()
}
