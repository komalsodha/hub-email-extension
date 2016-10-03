package com.blackducksoftware.integration.email.model;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.builder.ValidationResultEnum;
import com.blackducksoftware.integration.hub.builder.ValidationResults;
import com.blackducksoftware.integration.hub.global.GlobalFieldKey;
import com.blackducksoftware.integration.hub.global.HubServerConfig;

public class HubServerBeanConfiguration {
	private final Logger logger = LoggerFactory.getLogger(HubServerBeanConfiguration.class);

	private final ExtensionProperties emailConfig;

	public HubServerBeanConfiguration(final ExtensionProperties emailConfig) {
		this.emailConfig = emailConfig;
	}

	public HubServerConfig build() {
		final HubServerConfigBuilder configBuilder = new HubServerConfigBuilder();
		configBuilder.setHubUrl(emailConfig.getHubServerUrl());
		configBuilder.setUsername(emailConfig.getHubServerUser());
		configBuilder.setPassword(emailConfig.getHubServerPassword());
		configBuilder.setTimeout(emailConfig.getHubServerTimeout());
		configBuilder.setProxyHost(emailConfig.getHubProxyHost());
		configBuilder.setProxyPort(emailConfig.getHubProxyPort());
		configBuilder.setIgnoredProxyHosts(emailConfig.getHubProxyNoHost());
		configBuilder.setProxyUsername(emailConfig.getHubProxyUser());
		configBuilder.setProxyPassword(emailConfig.getHubProxyPassword());

		// output the configuration details
		logger.info("Hub Server URL          = " + configBuilder.getHubUrl());
		logger.info("Hub User                = " + configBuilder.getUsername());
		logger.info("Hub Timeout             = " + configBuilder.getTimeout());
		logger.info("Hub Proxy Host          = " + configBuilder.getProxyHost());
		logger.info("Hub Proxy Port          = " + configBuilder.getProxyPort());
		logger.info("Hub Ignored Proxy Hosts = " + configBuilder.getIgnoredProxyHosts());
		logger.info("Hub Proxy User          = " + configBuilder.getProxyUsername());

		final ValidationResults<GlobalFieldKey, HubServerConfig> results = configBuilder.buildResults();

		if (results.hasErrors()) {
			logger.error("##### Properties file contains errors.####");
			final Set<GlobalFieldKey> keys = results.getResultMap().keySet();
			for (final GlobalFieldKey fieldKey : keys) {
				if (results.hasErrors(fieldKey)) {
					logger.error(results.getResultString(fieldKey, ValidationResultEnum.ERROR));
				}
				if (results.hasWarnings(fieldKey)) {
					logger.warn(results.getResultString(fieldKey, ValidationResultEnum.WARN));
				}
			}
		} else {
			if (results.hasWarnings()) {
				final Set<GlobalFieldKey> keys = results.getResultMap().keySet();
				for (final GlobalFieldKey fieldKey : keys) {
					if (results.hasWarnings(fieldKey)) {
						logger.warn(results.getResultString(fieldKey, ValidationResultEnum.WARN));
					}
				}
			}
			return results.getConstructedObject();
		}

		return null;
	}

}