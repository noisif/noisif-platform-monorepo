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
package xyz.noisif.nsl.serve;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.noisif.nsl.common.bootstrap.CriticalBootstrapException;
import xyz.noisif.nsl.common.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class GenericDevServer implements Runnable {
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final CompletableFuture<Integer> serverReadyFuture = new CompletableFuture<>();
  private final String[] args;

  protected GenericDevServer(String[] args) {
    this.args = args;
  }

  public void start() {
    log.info("Starting DevServer initialization...");
    Runtime.getRuntime().addShutdownHook(new Thread(this, "devserver-shutdown-hook"));
    try {
      final String configFileLocation = getConfigFileLocation();
      final Properties props = loadConfigAsProperties(configFileLocation);

      final String serverHost = getNotNullProp(props, "server.host");
      final int serverPort = Integer.parseInt(getNotNullProp(props, "server.port"));
      final String serverDir = getNotNullProp(props, "server.dir");
      log.info(
          "Resolved basic config - host: {}, port: {}, dir: {}", serverHost, serverPort, serverDir);

      final List<ProxyDefinition> proxyDefinitions = collectProxyDefinitions(props);

      final DevServerProperties devServerProperties =
          new DevServerProperties(serverHost, serverPort, serverDir, proxyDefinitions);
      onServerInit(devServerProperties);
    } catch (Exception ex) {
      serverReadyFuture.completeExceptionally(ex);
      log.error("FATAL ERROR DURING DEV SERVER STARTUP: {}", ex.getMessage(), ex);
      System.exit(1);
    }
  }

  public CompletableFuture<Integer> getServerReadyFuture() {
    return serverReadyFuture;
  }

  protected void onServerReady(int resolvedPort) {
    serverReadyFuture.complete(resolvedPort);
  }

  protected URI createStaticDirAndGetUri(DevServerProperties devServerProperties) {
    final File staticDir = new File(devServerProperties.staticDir());
    if (!staticDir.exists() && staticDir.mkdirs()) {
      log.info("Created static directory: {}", staticDir.getAbsolutePath());
    }
    return staticDir.toURI().normalize();
  }

  @Override
  public void run() {
    log.info("Shutting down DevServer...");
    onServerStopped();
  }

  private String getConfigFileLocation() {
    final Options options = new Options();
    options.addOption(
        Option.builder().longOpt("config").hasArg().desc("config file location").get());
    final CommandLineParser parser = new DefaultParser();
    CommandLine cmd;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException ex) {
      final HelpFormatter helpFormatter = HelpFormatter.builder().get();
      throw new CriticalBootstrapException(helpFormatter.toSyntaxOptions(options), ex);
    }
    final String configFileLocation = cmd.getOptionValue("config");
    if (configFileLocation == null) {
      throw new CriticalBootstrapException("config file location is required");
    }
    log.debug("Config file location resolved to: {}", configFileLocation);
    return configFileLocation;
  }

  private Properties loadConfigAsProperties(String configFileLocation) {
    final File configFile = new File(configFileLocation);
    if (!configFile.exists()) {
      throw new CriticalBootstrapException("config file does not exist: " + configFileLocation);
    }
    final Properties properties = new Properties();
    try (final FileInputStream fileInputStream = new FileInputStream(configFile)) {
      properties.load(fileInputStream);
      log.debug("Loaded {} properties from file", properties.size());
    } catch (Exception ex) {
      throw new CriticalBootstrapException("unable to read config file: " + configFileLocation);
    }
    return properties;
  }

  private List<ProxyDefinition> collectProxyDefinitions(Properties properties) {
    final Set<String> proxyIds =
        properties.stringPropertyNames().stream()
            .filter(key -> key.startsWith("proxy."))
            .map(key -> StringUtil.splitRegex(key, "\\.").get(1))
            .collect(Collectors.toSet());
    log.debug("Found {} potential proxy IDs: {}", proxyIds.size(), proxyIds);

    final List<ProxyDefinition> proxyDefinitions = new ArrayList<>();
    for (final String id : proxyIds) {
      final String prefix = properties.getProperty("proxy." + id + ".prefix");
      final String url = properties.getProperty("proxy." + id + ".targetUrl");
      final boolean stripPrefix =
          Boolean.parseBoolean(properties.getProperty("proxy." + id + ".stripPrefix"));
      if (prefix == null || url == null) {
        log.warn("Incomplete proxy definition for ID '{}', missing 'prefix' or 'targetUrl'", id);
        continue;
      }
      log.debug(
          "Registered Proxy - ID: '{}', prefix: '{}', target: '{}', stripPrefix: '{}'",
          id,
          prefix,
          url,
          stripPrefix);
      proxyDefinitions.add(new ProxyDefinition(prefix, url, stripPrefix));
    }
    log.info("Successfully loaded {} proxy definition(s)", proxyDefinitions.size());
    return proxyDefinitions;
  }

  private String getNotNullProp(Properties props, String key) {
    final String value = props.getProperty(key);
    if (value == null) {
      log.debug("Required property '{}' is missing from properties file", key);
      throw new NullPointerException("Property '" + key + "' cannot be null");
    }
    return value;
  }

  protected abstract void onServerInit(DevServerProperties properties) throws Exception;

  protected abstract void onServerStopped();
}
