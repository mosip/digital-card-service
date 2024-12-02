package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.digitalcard.util.TemplateGenerator;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.templatemanager.spi.TemplateManager;
import io.mosip.kernel.templatemanager.velocity.impl.TemplateManagerImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TemplateGeneratorTest {

    @InjectMocks
    TemplateGenerator templateGenerator;

    @Mock
    Environment environment;

    private static Logger printLogger = DigitalCardRepoLogger.getLogger(TemplateGenerator.class);
    private String resourceLoader = "classpath";
    private String templatePath = ".";
    private boolean cache = Boolean.TRUE;
    private String defaultEncoding = StandardCharsets.UTF_8.name();

    @Test
    public void testGetTemplate_Success2() throws Exception {
        String cardTemplate= "templateCard";
        Map<String, Object> attributes = new HashMap<>();

        String encodedTemplate = Base64.getEncoder().encodeToString("template-content".getBytes());
        InputStream expectedStream = new ByteArrayInputStream("merged-content".getBytes());

        when(environment.getProperty(cardTemplate)).thenReturn(encodedTemplate);

        InputStream actualStream = templateGenerator.getTemplate(cardTemplate, attributes, "eng");
    }

    @Test
    public void testGetTemplateManager_Success() {
        TemplateManager templateManager = templateGenerator.getTemplateManager();

        assertNotNull(templateManager);
        assertTrue(templateManager instanceof TemplateManagerImpl);

    }

}
