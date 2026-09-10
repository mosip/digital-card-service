package io.mosip.digitalcard.test.exception;

import io.mosip.digitalcard.constant.AuthAdapterErrorCode;
import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.exception.ApiExceptionHandler;
import io.mosip.digitalcard.exception.DataNotFoundException;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.kernel.core.authmanager.exception.AuthNException;
import io.mosip.kernel.core.authmanager.exception.AuthZException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;

import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ApiExceptionHandlerTest {

    @InjectMocks
    private ApiExceptionHandler apiExceptionHandler;

    @Test
    public void controlDataNotFoundExceptionTest()throws IOException {
        ResponseEntity<ResponseWrapper<ServiceError>> actualControlDataServiceExceptionResult = apiExceptionHandler
                .controlDataNotFoundException(null, new DataNotFoundException("DCS-408","PDF not found"));
        assertTrue(actualControlDataServiceExceptionResult.hasBody());
        assertTrue(actualControlDataServiceExceptionResult.getHeaders().isEmpty());
        assertEquals(HttpStatus.OK, actualControlDataServiceExceptionResult.getStatusCode());
        List<ServiceError> errors = actualControlDataServiceExceptionResult.getBody().getErrors();
        assertEquals(1, errors.size());
        ServiceError getResult = errors.get(0);
        assertEquals("DCS-408", getResult.getErrorCode());
        assertEquals("PDF not found", getResult.getMessage());
    }

    @Test
    public void controlDataNotFoundExceptionTest1()throws IOException {
        ResponseEntity<ResponseWrapper<ServiceError>> actualControlDataServiceExceptionResult = apiExceptionHandler
                .controlDataNotFoundException(null, new DigitalCardServiceException("DCS-408","PDF not found"));
        assertTrue(actualControlDataServiceExceptionResult.hasBody());
        assertTrue(actualControlDataServiceExceptionResult.getHeaders().isEmpty());
        assertEquals(HttpStatus.OK, actualControlDataServiceExceptionResult.getStatusCode());
        List<ServiceError> errors = actualControlDataServiceExceptionResult.getBody().getErrors();
        assertEquals(1, errors.size());
        ServiceError getResult = errors.get(0);
        assertEquals("DCS-011", getResult.getErrorCode());
        assertEquals("PDF not found", getResult.getMessage());
    }

    @Test
    public void testOnAccessDeniedException() throws IOException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied");
        ResponseEntity<ResponseWrapper<ServiceError>> responseEntity = apiExceptionHandler.onAccessDeniedException(httpServletRequest, accessDeniedException);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());

        ResponseWrapper<ServiceError> responseBody = responseEntity.getBody();
        assertNotNull(responseBody);
        assertNotNull(responseBody.getErrors());
        assertEquals(1, responseBody.getErrors().size());

        ServiceError serviceError = responseBody.getErrors().get(0);
        assertEquals(AuthAdapterErrorCode.FORBIDDEN.getErrorCode(), serviceError.getErrorCode());
    }

    @Test
    public void testOnAuthZException() throws IOException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

        List<ServiceError> serviceErrors = new ArrayList<>();
        serviceErrors.add(new ServiceError(AuthAdapterErrorCode.FORBIDDEN.getErrorCode(),
                AuthAdapterErrorCode.FORBIDDEN.getErrorMessage()));

        AuthZException authZException = new AuthZException(serviceErrors);

        ResponseEntity<ResponseWrapper<ServiceError>> responseEntity = apiExceptionHandler.onAuthZException(httpServletRequest, authZException);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());

        ResponseWrapper<ServiceError> responseBody = responseEntity.getBody();
        assertNotNull(responseBody);
        assertNotNull(responseBody.getErrors());
        assertEquals(1, responseBody.getErrors().size());

        ServiceError serviceError = responseBody.getErrors().get(0);
        assertEquals(AuthAdapterErrorCode.FORBIDDEN.getErrorCode(), serviceError.getErrorCode());
    }

    @Test
    public void testOnAuthNException() throws IOException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        List<ServiceError> serviceErrors = new ArrayList<>();
        serviceErrors.add(new ServiceError(AuthAdapterErrorCode.UNAUTHORIZED.getErrorCode(),
                AuthAdapterErrorCode.UNAUTHORIZED.getErrorMessage()));

        AuthNException authNException = new AuthNException(serviceErrors);

        ResponseEntity<ResponseWrapper<ServiceError>> responseEntity = apiExceptionHandler.onAuthNException(httpServletRequest, authNException);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());

        ResponseWrapper<ServiceError> responseBody = responseEntity.getBody();
        assertNotNull(responseBody);
        assertNotNull(responseBody.getErrors());
        assertEquals(1, responseBody.getErrors().size());

        ServiceError serviceError = responseBody.getErrors().get(0);
        assertEquals(AuthAdapterErrorCode.UNAUTHORIZED.getErrorCode(), serviceError.getErrorCode());
    }

    @Test
    public void testDefaultErrorHandler() throws IOException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        Exception exception = new Exception("An unexpected error occurred");
        ResponseEntity<ResponseWrapper<ServiceError>> responseEntity = apiExceptionHandler.defaultErrorHandler(httpServletRequest, exception);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        ResponseWrapper<ServiceError> responseBody = responseEntity.getBody();
        assertNotNull(responseBody);
        assertNotNull(responseBody.getErrors());
        assertEquals(1, responseBody.getErrors().size());

        ServiceError serviceError = responseBody.getErrors().get(0);
        assertEquals(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorCode(), serviceError.getErrorCode());
    }

}
