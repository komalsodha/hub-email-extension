package com.blackducksoftware.integration.email.notifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.email.ExtensionLogger;
import com.blackducksoftware.integration.email.extension.config.ExtensionConfigManager;
import com.blackducksoftware.integration.email.extension.config.ExtensionInfo;
import com.blackducksoftware.integration.email.extension.server.RestletApplication;
import com.blackducksoftware.integration.email.extension.server.oauth.AccessType;
import com.blackducksoftware.integration.email.extension.server.oauth.OAuthEndpoint;
import com.blackducksoftware.integration.email.extension.server.oauth.OAuthRestConnection;
import com.blackducksoftware.integration.email.extension.server.oauth.TokenManager;
import com.blackducksoftware.integration.email.extension.server.oauth.listeners.IAuthorizedListener;
import com.blackducksoftware.integration.email.model.ExtensionProperties;
import com.blackducksoftware.integration.email.model.HubServerBeanConfiguration;
import com.blackducksoftware.integration.email.model.JavaMailWrapper;
import com.blackducksoftware.integration.email.notifier.routers.DailyDigestRouter;
import com.blackducksoftware.integration.email.notifier.routers.MonthlyDigestRouter;
import com.blackducksoftware.integration.email.notifier.routers.RouterManager;
import com.blackducksoftware.integration.email.notifier.routers.WeeklyDigestRouter;
import com.blackducksoftware.integration.email.service.EmailMessagingService;
import com.blackducksoftware.integration.hub.dataservices.DataServicesFactory;
import com.blackducksoftware.integration.hub.dataservices.extension.ExtensionConfigDataService;
import com.blackducksoftware.integration.hub.dataservices.notification.NotificationDataService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.EncryptionException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.RestConnection;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

public class EmailEngine implements IAuthorizedListener {
	private final Logger logger = LoggerFactory.getLogger(EmailEngine.class);

	public final Configuration configuration;
	public final DateFormat notificationDateFormat;
	public final Date applicationStartDate;
	public final ExecutorService executorService;
	public final JavaMailWrapper javaMailWrapper;

	public final EmailMessagingService emailMessagingService;
	public final HubServerConfig hubServerConfig;
	public final RestConnection restConnection;
	public final Properties appProperties;
	public final ExtensionProperties customerProperties;
	public final NotificationDataService notificationDataService;
	public final RouterManager routerManager;
	public final TokenManager tokenManager;
	public final OAuthEndpoint restletComponent;
	public final ExtensionInfo extensionInfoData;
	public final ExtensionConfigManager extConfigManager;
	public final ExtensionConfigDataService extConfigDataService;
	public final DataServicesFactory dataServicesFactory;

	public EmailEngine() throws IOException, EncryptionException, URISyntaxException, BDRestException {
		appProperties = createAppProperties();
		customerProperties = createCustomerProperties();
		configuration = createFreemarkerConfig();
		hubServerConfig = createHubConfig();
		extensionInfoData = createExtensionInfoData();
		tokenManager = createTokenManager();
		restConnection = createRestConnection();
		dataServicesFactory = createDataServicesFactory();
		extConfigManager = createExtensionConfigManager();
		javaMailWrapper = createJavaMailWrapper();
		notificationDateFormat = createNotificationDateFormat();
		applicationStartDate = createApplicationStartDate();
		executorService = createExecutorService();
		emailMessagingService = createEmailMessagingService();
		notificationDataService = createNotificationDataService();
		extConfigDataService = createExtensionConfigDataService();
		routerManager = createRouterManager();
		restletComponent = createRestletComponent();
	}

	public void start() {
		try {
			restletComponent.start();
			tokenManager.refreshToken(AccessType.USER);
		} catch (final Exception e) {
			logger.error("Error Starting Email Engine", e);
		}
	}

	public void shutDown() {
		try {
			routerManager.stopRouters();
			restletComponent.stop();
		} catch (final Exception e) {
			logger.error("Error stopping Email Engine", e);
		}
	}

	public Properties createAppProperties() throws IOException {
		final Properties appProperties = new Properties();
		final String configLocation = System.getProperty("ext.config.location");
		final File customerPropertiesFile = new File(configLocation, "extension.properties");
		try (FileInputStream fileInputStream = new FileInputStream(customerPropertiesFile)) {
			appProperties.load(fileInputStream);
		}

		return appProperties;
	}

	public Configuration createFreemarkerConfig() throws IOException {
		final Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
		cfg.setDirectoryForTemplateLoading(new File(customerProperties.getEmailTemplateDirectory()));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		cfg.setLogTemplateExceptions(false);

		return cfg;
	}

	public DateFormat createNotificationDateFormat() {
		final DateFormat dateFormat = new SimpleDateFormat(RestConnection.JSON_DATE_FORMAT);
		dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));
		return dateFormat;
	}

	public Date createApplicationStartDate() {
		return new Date();
	}

	public ExecutorService createExecutorService() {
		final ThreadFactory threadFactory = Executors.defaultThreadFactory();
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), threadFactory);
	}

	public ExtensionProperties createCustomerProperties() {
		return new ExtensionProperties(appProperties);
	}

	public JavaMailWrapper createJavaMailWrapper() {
		return new JavaMailWrapper();
	}

	public EmailMessagingService createEmailMessagingService() {
		return new EmailMessagingService(customerProperties, configuration, javaMailWrapper);
	}

	public HubServerConfig createHubConfig() {
		final HubServerBeanConfiguration serverBeanConfig = new HubServerBeanConfiguration(customerProperties);

		return serverBeanConfig.build();
	}

	public RestConnection createRestConnection() throws EncryptionException, URISyntaxException, BDRestException {
		final RestConnection restConnection = initRestConnection();
		return restConnection;
	}

	public NotificationDataService createNotificationDataService() {
		final Logger notificationLogger = LoggerFactory.getLogger(NotificationDataService.class);
		final ExtensionLogger serviceLogger = new ExtensionLogger(notificationLogger);
		final NotificationDataService notificationDataService = dataServicesFactory
				.createNotificationDataService(serviceLogger);
		return notificationDataService;
	}

	public RestConnection initRestConnection() throws EncryptionException, URISyntaxException, BDRestException {
		final RestConnection restConnection = new OAuthRestConnection(hubServerConfig.getHubUrl().toString(),
				tokenManager);
		restConnection.setProxyProperties(hubServerConfig.getProxyInfo());
		restConnection.setTimeout(hubServerConfig.getTimeout());
		return restConnection;
	}

	public RouterManager createRouterManager() {
		final RouterManager manager = new RouterManager();

		final DailyDigestRouter dailyRouter = new DailyDigestRouter(customerProperties, notificationDataService,
				extConfigDataService, emailMessagingService);
		final WeeklyDigestRouter weeklyRouter = new WeeklyDigestRouter(customerProperties, notificationDataService,
				extConfigDataService, emailMessagingService);
		final MonthlyDigestRouter monthlyRouter = new MonthlyDigestRouter(customerProperties, notificationDataService,
				extConfigDataService, emailMessagingService);
		manager.attachRouter(dailyRouter);
		manager.attachRouter(weeklyRouter);
		manager.attachRouter(monthlyRouter);
		return manager;
	}

	public ExtensionInfo createExtensionInfoData() {
		final String id = customerProperties.getExtensionId();
		final String name = customerProperties.getExtensionName();
		final String description = customerProperties.getExtensionDescription();
		final String baseUrl = customerProperties.getExtensionBaseUrl();

		return new ExtensionInfo(id, name, description, baseUrl);
	}

	public OAuthEndpoint createRestletComponent() {
		final RestletApplication application = new RestletApplication(tokenManager, extConfigManager);
		final OAuthEndpoint endpoint = new OAuthEndpoint(application);
		try {
			final URL url = new URL(extensionInfoData.getBaseUrl());
			final int port = url.getPort();
			if (port > 0) {
				endpoint.getServers().add(Protocol.HTTP, port);
			}
		} catch (final MalformedURLException e) {
			logger.error("createRestletComponent error with base URL", e);
		}

		return endpoint;
	}

	public TokenManager createTokenManager() {
		final TokenManager tokenManager = new TokenManager(extensionInfoData);
		tokenManager.addAuthorizedListener(this);
		return tokenManager;
	}

	public ExtensionConfigManager createExtensionConfigManager() {
		final ExtensionConfigManager extConfigManager = new ExtensionConfigManager(extensionInfoData,
				dataServicesFactory.getJsonParser());
		return extConfigManager;
	}

	public ExtensionConfigDataService createExtensionConfigDataService() {
		final Logger extensionServiceLogger = LoggerFactory.getLogger(ExtensionConfigDataService.class);
		final ExtensionLogger serviceLogger = new ExtensionLogger(extensionServiceLogger);
		final ExtensionConfigDataService extConfigDataService = dataServicesFactory
				.createExtensionConfigDataService(serviceLogger);
		return extConfigDataService;
	}

	public DataServicesFactory createDataServicesFactory() {
		return new DataServicesFactory(restConnection);
	}

	@Override
	public void onAuthorized() {
		routerManager.updateHubExtensionId(tokenManager.getHubExtensionId());
		routerManager.startRouters();
	}
}
