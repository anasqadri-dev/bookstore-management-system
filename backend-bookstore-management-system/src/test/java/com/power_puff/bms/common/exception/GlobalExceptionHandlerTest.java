package com.power_puff.bms.common.exception;

import com.power_puff.bms.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsMissingResourceToNotFoundWithStableErrorEnvelope() {
        var response = handler.handleNotFound(
                new ResourceNotFoundException("Product was not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertErrorEnvelope(response.getBody(), "Product was not found");
    }

    @Test
    void mapsDuplicateResourceToConflictWithStableErrorEnvelope() {
        var response = handler.handleDuplicate(
                new DuplicateResourceException("Duplicate SKU"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertErrorEnvelope(response.getBody(), "Duplicate SKU");
    }

    @Test
    void mapsBeanValidationErrorsToFieldMessageMap() throws Exception {
        Method method = getClass().getDeclaredMethod("validationTarget", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError(
                "request", "name", null, false, null, null, "Name is required"));

        var response = handler.handleValidation(
                new MethodArgumentNotValidException(parameter, bindingResult));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Request validation failed", response.getBody().message());
        assertEquals("Name is required",
                ((Map<?, ?>) response.getBody().data()).get("name"));
    }

    @Test
    void mapsMissingQueryParameterToActionableBadRequestMessage() {
        var response = handler.handleInvalidRequestParameter(
                new MissingServletRequestParameterException("startDate", "LocalDate"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertErrorEnvelope(response.getBody(),
                "A required request parameter is missing or invalid.");
    }

    @Test
    void mapsUnreadableRequestBodyWithoutExposingParserDetails() {
        var inputMessage = new MockHttpInputMessage(new byte[0]);
        var exception = new HttpMessageNotReadableException(
                "Malformed request body",
                new IllegalArgumentException("private parser detail"),
                inputMessage);

        var response = handler.handleUnreadableRequest(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertErrorEnvelope(response.getBody(),
                "Request body is missing or contains an invalid value.");
        assertFalse(response.getBody().message().contains("private"));
    }

    @Test
    void mapsConstraintViolationAndIllegalArgumentToBadRequest() {
        var constraintResponse = handler.handleConstraintViolation(
                new ConstraintViolationException("Quantity must be positive", Set.of()));
        var argumentResponse = handler.handleIllegalArgument(
                new IllegalArgumentException("Start date cannot be after end date"));

        assertEquals(HttpStatus.BAD_REQUEST, constraintResponse.getStatusCode());
        assertErrorEnvelope(constraintResponse.getBody(), "Quantity must be positive");

        assertEquals(HttpStatus.BAD_REQUEST, argumentResponse.getStatusCode());
        assertErrorEnvelope(argumentResponse.getBody(),
                "Start date cannot be after end date");
    }

    @Test
    void mapsDatabaseIntegrityConflictToGenericSafeMessage() {
        var response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("private SQL and constraint details"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertErrorEnvelope(response.getBody(),
                "The operation conflicts with existing data.");
        assertFalse(response.getBody().message().contains("private"));
    }

    @Test
    void mapsAuthenticationAndAuthorizationFailuresToCorrectStatuses() {
        var authenticationResponse = handler.handleAuthentication(
                new BadCredentialsException("sensitive authentication detail"));
        var authorizationResponse = handler.handleAccessDenied(
                new AccessDeniedException("internal authorization detail"));

        assertEquals(HttpStatus.UNAUTHORIZED, authenticationResponse.getStatusCode());
        assertErrorEnvelope(authenticationResponse.getBody(),
                "Username or password is incorrect.");

        assertEquals(HttpStatus.FORBIDDEN, authorizationResponse.getStatusCode());
        assertErrorEnvelope(authorizationResponse.getBody(),
                "You are not allowed to perform this operation.");
    }

    @Test
    void mapsUnexpectedFailuresToGenericMessageWithoutLeakingExceptionDetails() {
        var response = handler.handleUnexpected(
                new IllegalStateException("private file path and SQL details"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertErrorEnvelope(response.getBody(),
                "An unexpected server error occurred.");
        assertFalse(response.getBody().message().contains("private"));
        assertFalse(response.getBody().message().contains("SQL"));
    }

    private void assertErrorEnvelope(ApiResponse<?> response, String expectedMessage) {
        assertNotNull(response);
        assertFalse(response.success());
        assertEquals(expectedMessage, response.message());
        assertNull(response.data());
    }

    @SuppressWarnings("unused")
    private void validationTarget(String name) {
        // Supplies a MethodParameter for the standalone validation-handler test.
    }
}
