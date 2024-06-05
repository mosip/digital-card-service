package io.mosip.digitalcard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Dhanendra Sahu
 *
 */
@Configuration
@ConfigurationProperties(prefix = "openapi")
@Data
public class OpenApiProperties {
	private InfoProperty info;
	private Service service;
	private Group group;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class InfoProperty {
	private String title;
	private String description;
	private String version;
	private LicenseProperty license;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class LicenseProperty {
	private String name;
	private String url;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class Service {
	private List<Server> servers;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class Server {
	private String description;
	private String url;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class Group {
	private String name;
	private List<String> paths;
}