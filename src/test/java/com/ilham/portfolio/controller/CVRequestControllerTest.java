
package com.ilham.portfolio.controller;

import com.ilham.portfolio.dto.CVRequest;
import com.ilham.portfolio.exception.GlobalExceptionHandler;
import com.ilham.portfolio.ratelimit.CVRateLimiter;
import com.ilham.portfolio.service.CVDownloadTokenService;
import com.ilham.portfolio.service.CVEmailException;
import com.ilham.portfolio.service.CVRequestService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CVRequestControllerTest {

    private MockMvc mockMvc;
    private CVRequestService cvRequestService;
    private CVDownloadTokenService tokenService;

    private static final String VISITOR_IP = "192.168.1.10";

    private static final String VALID_REQUEST = """
            {
                "fullName": "Jane Smith",
                "email": "jane@example.com",
                "company": "Example Company",
                "message": "Interested in your portfolio."
            }
            """;

    @BeforeEach
    void setUp() {

        cvRequestService = mock(CVRequestService.class);
        tokenService = mock(CVDownloadTokenService.class);

        CVRateLimiter rateLimiter = new CVRateLimiter();

        CVRequestController controller =
                new CVRequestController(
                        cvRequestService,
                        tokenService,
                        rateLimiter
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(tokenService.generateToken())
                .thenReturn("test-download-token");
    }

    @Test
    void shouldAcceptValidCVRequest() throws Exception {

        mockMvc.perform(post("/api/cv/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Thanks for your interest in my work!"))
                .andExpect(jsonPath("$.downloadToken")
                        .value("test-download-token"));

        verify(cvRequestService, times(1))
                .processRequest(any(CVRequest.class));

        verify(tokenService, times(1))
                .generateToken();
    }

    @Test
    void shouldRejectMissingFullName() throws Exception {

        String invalidRequest = """
                {
                    "email": "jane@example.com",
                    "company": "Example Company",
                    "message": ""
                }
                """;

        mockMvc.perform(post("/api/cv/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cvRequestService);

        verify(tokenService, never()).generateToken();
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {

        String invalidRequest = """
                {
                    "fullName": "Jane Smith",
                    "email": "invalid-email",
                    "company": "",
                    "message": ""
                }
                """;

        mockMvc.perform(post("/api/cv/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cvRequestService);

        verify(tokenService, never()).generateToken();
    }

    @Test
    void shouldRejectFourthRequestFromSameVisitor()
            throws Exception {

        for (int i = 0; i < 3; i++) {

            mockMvc.perform(post("/api/cv/request")
                            .with(request -> {
                                request.setRemoteAddr(VISITOR_IP);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/cv/request")
                        .with(request -> {
                            request.setRemoteAddr(VISITOR_IP);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error")
                        .value("Too many requests. Please try again later."));

        verify(cvRequestService, times(3))
                .processRequest(any(CVRequest.class));

        verify(tokenService, times(3))
                .generateToken();
    }

    @Test
    void shouldAllowRequestFromDifferentVisitor()
            throws Exception {

        for (int i = 0; i < 3; i++) {

            mockMvc.perform(post("/api/cv/request")
                            .with(request -> {
                                request.setRemoteAddr(VISITOR_IP);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/cv/request")
                        .with(request -> {
                            request.setRemoteAddr("192.168.1.20");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk());

        verify(cvRequestService, times(4))
                .processRequest(any(CVRequest.class));

        verify(tokenService, times(4))
                .generateToken();
    }

    @Test
    void shouldReturnServiceUnavailableWhenEmailFails()
            throws Exception {

        doThrow(CVEmailException.class)
                .when(cvRequestService)
                .processRequest(any(CVRequest.class));

        mockMvc.perform(post("/api/cv/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error")
                        .value("CV requests are temporarily unavailable. Please try again later."));

        verify(tokenService, never()).generateToken();
    }
}

