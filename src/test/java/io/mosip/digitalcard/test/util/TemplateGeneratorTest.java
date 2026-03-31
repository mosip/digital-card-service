package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.util.TemplateGenerator;
import io.mosip.kernel.core.templatemanager.exception.TemplateParsingException;
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
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
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
    public void testGetTemplateSuccess() throws Exception {
        String cardTemplate= "templateCard";
        TemplateGenerator spyGenerator = spy(templateGenerator);
        Map<String, Object> attributes = new HashMap<>();
        String encodedTemplate = Base64.getEncoder().encodeToString("template-content".getBytes());
        InputStream mergedStream = new ByteArrayInputStream("merged-content".getBytes());
        when(environment.getProperty(cardTemplate)).thenReturn(encodedTemplate);
        doReturn(templateManager).when(spyGenerator).getTemplateManager();
        when(templateManager.merge(any(InputStream.class), any(Map.class))).thenReturn(mergedStream);

        InputStream actualStream = spyGenerator.getTemplate(cardTemplate, attributes, "eng");

        assertNotNull(actualStream);
    }

    @Test
    public void testGetTemplateTemplateResourceNotFoundException() throws Exception {
        String langCode = "eng";
        Map<String, Object> attributes = new HashMap<>();
        when(environment.getProperty(CARD_TEMPLATE)).thenReturn(null);
        lenient().doThrow(new TemplateResourceNotFoundException("Template not found", "ERR_TEMPLATE_NOT_FOUND")).when(templateManager).merge(any(InputStream.class), any(Map.class));

        assertThrows(NullPointerException.class, () -> templateGenerator.getTemplate(CARD_TEMPLATE, attributes, langCode));
    }

    @Test
    public void testGetTemplateShouldWrapTemplateManagerExceptions() throws Exception {
        TemplateGenerator spyGenerator = spy(templateGenerator);
        Map<String, Object> attributes = new HashMap<>();
        String encodedTemplate = Base64.getEncoder().encodeToString("template-content".getBytes());

        when(environment.getProperty(CARD_TEMPLATE)).thenReturn(encodedTemplate);
        doReturn(templateManager).when(spyGenerator).getTemplateManager();
        when(templateManager.merge(any(InputStream.class), any(Map.class)))
                .thenThrow(new TemplateResourceNotFoundException("ERR_TEMPLATE_NOT_FOUND", "Template not found"));

        TemplateParsingException exception = assertThrows(TemplateParsingException.class,
                () -> spyGenerator.getTemplate(CARD_TEMPLATE, attributes, "eng"));

        assertEquals("ERR_TEMPLATE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    public void testGetTemplateManagerSuccess() {
        TemplateManager templateManager = templateGenerator.getTemplateManager();

        assertNotNull(templateManager);
        assertTrue(templateManager instanceof TemplateManagerImpl);
    }

}
