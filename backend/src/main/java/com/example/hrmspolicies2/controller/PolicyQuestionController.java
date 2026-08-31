package com.example.hrmspolicies2.controller;

import com.example.hrmspolicies2.dto.AskPolicyRequest;
import com.example.hrmspolicies2.dto.response.ApiResponse;
import com.example.hrmspolicies2.dto.response.PolicyAnswerResponse;
import com.example.hrmspolicies2.service.PolicyQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policies")
public class PolicyQuestionController {

    private final PolicyQuestionService questionService;

    public PolicyQuestionController(
            PolicyQuestionService questionService
    ) {
        this.questionService =
                questionService;
    }

    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<
            ApiResponse<PolicyAnswerResponse>
            > ask(
            @Valid
            @RequestBody
            AskPolicyRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Policy question processed successfully",
                        questionService.ask(
                                request
                        )
                )
        );
    }
}