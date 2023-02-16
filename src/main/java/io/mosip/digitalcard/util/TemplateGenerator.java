package io.mosip.digitalcard.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.dto.TemplateResponseDto;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.templatemanager.exception.TemplateMethodInvocationException;
import io.mosip.kernel.core.templatemanager.exception.TemplateParsingException;
import io.mosip.kernel.core.templatemanager.exception.TemplateResourceNotFoundException;
import io.mosip.kernel.core.templatemanager.spi.TemplateManager;
import io.mosip.kernel.templatemanager.velocity.impl.TemplateManagerImpl;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
/**
 * The Class TemplateGenerator.
 * 
 * @author M1048358 Alok
 */
@Component
public class TemplateGenerator {

	/** The reg proc logger. */
	private static Logger printLogger = DigitalCardRepoLogger.getLogger(TemplateGenerator.class);

	/** The resource loader. */
	private String resourceLoader = "classpath";

	/** The template path. */
	private String templatePath = ".";

	/** The cache. */
	private boolean cache = Boolean.TRUE;

	/** The default encoding. */
	private String defaultEncoding = StandardCharsets.UTF_8.name();

	/** The rest client service. */
	@Autowired
	private RestClient restClient;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private Utility utility;

	@Value("${mosip.digitalcard.service.template}")
	private String templateFile;

	private static String SEMICOLON = ";";
	private static String COLON = ":";

	@Autowired
	private Environment environment;


	/**
	 * Gets the template.
	 *
	 *   the template type code
	 * @param attributes
	 *            the attributes
	 * @param langCode
	 *            the lang code
	 * @return the template
	 * @throws Exception
	 *             Signals that an I/O exception has occurred.
	 *             the apis resource access exception
	 */
	public InputStream getTemplate(String cardTemplate, Map<String, Object> attributes, String langCode)
			throws Exception {

		//ResponseWrapper<?> responseWrapper;
		//TemplateResponseDto template;
		printLogger.debug("TemplateGenerator::getTemplate()::entry");
		try {
			InputStream fileTextStream = null;
			InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(environment.getProperty(cardTemplate)));
			fileTextStream = getTemplateManager().merge(stream, attributes);
			printLogger.debug("TemplateGenerator::getTemplate()::exit");
			return fileTextStream;

		} catch (TemplateResourceNotFoundException | TemplateParsingException | TemplateMethodInvocationException e) {
			printLogger.error(e.getMessage()
							+ ExceptionUtils.getStackTrace(e));

			throw new TemplateParsingException(e.getErrorCode()
					,e.getErrorText());
		}
	}

	/**
	 * Gets the template manager.
	 *
	 * @return the template manager
	 */
	public TemplateManager getTemplateManager() {
		final Properties properties = new Properties();
		properties.put(RuntimeConstants.INPUT_ENCODING, defaultEncoding);
		properties.put(RuntimeConstants.OUTPUT_ENCODING, defaultEncoding);
		properties.put(RuntimeConstants.ENCODING_DEFAULT, defaultEncoding);
		properties.put(RuntimeConstants.RESOURCE_LOADER, resourceLoader);
		properties.put(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templatePath);
		properties.put(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, cache);
		properties.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute.class.getName());
		properties.put("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		properties.put("file.resource.loader.class", FileResourceLoader.class.getName());
		VelocityEngine engine = new VelocityEngine(properties);
		engine.init();
		return new TemplateManagerImpl(engine);
	}
}
