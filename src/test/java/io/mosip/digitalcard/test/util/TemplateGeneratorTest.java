package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.util.TemplateGenerator;
import io.mosip.kernel.core.templatemanager.exception.TemplateResourceNotFoundException;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TemplateGeneratorTest {

    @InjectMocks
    TemplateGenerator templateGenerator;

    @Mock
    Environment environment;

    @Mock
    private TemplateManager templateManager;

    private static final String CARD_TEMPLATE = "cardTemplate";

    @Test
    public void testGetTemplate_Success() throws Exception {
        String cardTemplate= "templateCard";
        Map<String, Object> attributes = new HashMap<>();

        String encodedTemplate = Base64.getEncoder().encodeToString("template-content".getBytes());
        InputStream expectedStream = new ByteArrayInputStream("merged-content".getBytes());

        when(environment.getProperty(cardTemplate)).thenReturn(encodedTemplate);

        InputStream actualStream = templateGenerator.getTemplate(cardTemplate, attributes, "eng");
    }

    @Test
    public void testGetTemplate_TemplateResourceNotFoundException() throws Exception {
        String langCode = "eng";
        Map<String, Object> attributes = new HashMap<>();
        when(environment.getProperty(CARD_TEMPLATE)).thenReturn(null);
        lenient().doThrow(new TemplateResourceNotFoundException("Template not found", "ERR_TEMPLATE_NOT_FOUND")).when(templateManager).merge(any(InputStream.class), any(Map.class));

        assertThrows(NullPointerException.class, () -> templateGenerator.getTemplate(CARD_TEMPLATE, attributes, langCode));
    }

    @Test
    public void testGetTemplateManager_Success() {
        TemplateManager templateManager = templateGenerator.getTemplateManager();

        assertNotNull(templateManager);
        assertTrue(templateManager instanceof TemplateManagerImpl);
    }

}
