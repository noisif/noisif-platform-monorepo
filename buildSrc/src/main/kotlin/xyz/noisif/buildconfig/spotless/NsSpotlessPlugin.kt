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
package xyz.noisif.buildconfig.spotless

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import xyz.noisif.buildconfig.spotless.spec.JavaFormatSpec
import xyz.noisif.buildconfig.spotless.spec.KotlinFormatSpec
import xyz.noisif.buildconfig.spotless.spec.KotlinGradleFormatSpec
import xyz.noisif.buildconfig.spotless.spec.PropertiesFormatSpec
import xyz.noisif.buildconfig.spotless.spec.ScalaFormatSpec
import xyz.noisif.buildconfig.spotless.spec.XmlFormatSpec
import xyz.noisif.buildconfig.spotless.spec.ui.CssFormatSpec
import xyz.noisif.buildconfig.spotless.spec.ui.HtmlFormatSpec
import xyz.noisif.buildconfig.spotless.spec.ui.SvelteFormatSpec
import xyz.noisif.buildconfig.spotless.spec.ui.TypescriptFormatSpec
import java.io.File

class NsSpotlessPlugin : NsSpotlessBasePlugin() {
  override fun SpotlessExtension.configureSpotless(
    root: Project,
    target: Project,
    licenseFile: File,
  ) {
    val formatSpecs = listOf(
      JavaFormatSpec(root, licenseFile),
      ScalaFormatSpec(root, licenseFile),
      KotlinFormatSpec(root, licenseFile),
      KotlinGradleFormatSpec(root, licenseFile),
      XmlFormatSpec(root, licenseFile),
      PropertiesFormatSpec(root, licenseFile),
      CssFormatSpec(root, licenseFile),
      HtmlFormatSpec(root, licenseFile),
      SvelteFormatSpec(root, licenseFile),
      TypescriptFormatSpec(root, licenseFile),
    )
    for (spec in formatSpecs) {
      if (spec.isApplicable(target)) {
        spec.applyFormat(this)
      }
    }
  }
}
