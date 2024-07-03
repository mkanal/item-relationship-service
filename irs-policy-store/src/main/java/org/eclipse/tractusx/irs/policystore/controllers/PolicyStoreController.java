/********************************************************************************
 * Copyright (c) 2022,2024
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 *       2022,2024: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022,2023: BOSCH AG
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.irs.policystore.controllers;

import static org.eclipse.tractusx.irs.common.ApiConstants.FORBIDDEN_DESC;
import static org.eclipse.tractusx.irs.common.ApiConstants.UNAUTHORIZED_DESC;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.tractusx.irs.common.auth.IrsRoles;
import org.eclipse.tractusx.irs.data.JsonParseException;
import org.eclipse.tractusx.irs.dtos.ErrorResponse;
import org.eclipse.tractusx.irs.edc.client.policy.Policy;
import org.eclipse.tractusx.irs.policystore.common.CommonConstants;
import org.eclipse.tractusx.irs.policystore.common.SearchParameterParser;
import org.eclipse.tractusx.irs.policystore.models.CreatePoliciesResponse;
import org.eclipse.tractusx.irs.policystore.models.CreatePolicyRequest;
import org.eclipse.tractusx.irs.policystore.models.PolicyResponse;
import org.eclipse.tractusx.irs.policystore.models.PolicyWithBpn;
import org.eclipse.tractusx.irs.policystore.models.SearchCriteria;
import org.eclipse.tractusx.irs.policystore.models.UpdatePolicyRequest;
import org.eclipse.tractusx.irs.policystore.services.PolicyPagingService;
import org.eclipse.tractusx.irs.policystore.services.PolicyStoreService;
import org.eclipse.tractusx.irs.policystore.validators.BusinessPartnerNumberListValidator;
import org.eclipse.tractusx.irs.policystore.validators.ValidListOfBusinessPartnerNumbers;
import org.eclipse.tractusx.irs.policystore.validators.ValidPolicyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Policy Store REST controller.
 */
@Slf4j
@RestController
@RequestMapping("irs")
@RequiredArgsConstructor
@SuppressWarnings({ "PMD.AvoidDuplicateLiterals",
                    "PMD.ExcessiveImports",
                    "PMD.UseVarargs"
})
@Validated
public class PolicyStoreController {

    public static final String BPN_REGEX = BusinessPartnerNumberListValidator.BPN_REGEX;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 1000;

    private final PolicyStoreService service;

    private final PolicyPagingService policyPagingService;

    private final HttpServletRequest httpServletRequest;

    @Operation(operationId = "registerAllowedPolicy",
               summary = "Register a policy that should be accepted in EDC negotiation.",
               security = @SecurityRequirement(name = "api_key"), tags = { "Item Relationship Service" },
               description = "Register a policy that should be accepted in EDC negotiation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201"),
                            @ApiResponse(responseCode = "500",
                                         description = "Policy registration failed due to an internal error.",
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-500"))
                                         }),
                            @ApiResponse(responseCode = "400",
                                         description = "Policy registration failed due to an invalid request.",
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-400"))
                                         }),
                            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-401"))
                                         }),
                            @ApiResponse(responseCode = "403", description = FORBIDDEN_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-403"))
                                         }),
    })
    @PostMapping("/policies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + IrsRoles.ADMIN_IRS + "')")
    public CreatePoliciesResponse registerAllowedPolicy(@Valid @RequestBody final CreatePolicyRequest request) {
        final Policy registeredPolicy;

        try {
            registeredPolicy = service.registerPolicy(request);
        } catch (JsonParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

        if (registeredPolicy == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Policy was not registered");
        }

        return CreatePoliciesResponse.fromPolicy(registeredPolicy);
    }

    @Operation(operationId = "getAllowedPoliciesByBpn",
               summary = "Lists the registered policies that should be accepted in EDC negotiation.",
               security = @SecurityRequirement(name = "api_key"), tags = { "Item Relationship Service" },
               description = "Lists the registered policies that should be accepted in EDC negotiation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Returns the policies as map of BPN to list of policies.",
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              examples = @ExampleObject(
                                                                      PolicyResponse.BPN_TO_POLICY_MAP_EXAMPLE),
                                                              schema = @Schema(
                                                                      description = "Map of BPN to list of policies"))
                                         }),
                            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-401"))
                                         }),
                            @ApiResponse(responseCode = "403", description = FORBIDDEN_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-403"))
                                         }),
    })
    @GetMapping("/policies")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('" + IrsRoles.ADMIN_IRS + "')")
    public Map<String, List<PolicyResponse>> getPolicies(//
            @RequestParam(required = false) //
            @ValidListOfBusinessPartnerNumbers //
            @Parameter(description = "List of business partner numbers.") //
            final List<String> businessPartnerNumbers //
    ) {

        final Map<String, String[]> parameterMap = this.httpServletRequest.getParameterMap();
        if (CollectionUtils.containsAny(parameterMap.keySet(), List.of("bpn", "bpns", "bpnls"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please use parameter 'businessPartnerNumbers' instead");
        }

        final Map<String, List<Policy>> policies = service.getPolicies(businessPartnerNumbers);

        return policies.entrySet()
                       .stream()
                       .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(),
                               entry.getValue().stream().map(PolicyResponse::fromPolicy).toList()))
                       .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    // TODO (mfischer): #639: add documentation
    // TODO (mfischer): #639: add integration tests
    // TODO (mfischer): #639: update insomnia
    @GetMapping("/policies/paged")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('" + IrsRoles.ADMIN_IRS + "')")
    public Page<PolicyResponse> getPoliciesPaged(//
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = CommonConstants.PROPERTY_BPN,
                             direction = Sort.Direction.ASC) //
            final Pageable pageable, //
            @RequestParam(required = false) //
            @ValidListOfBusinessPartnerNumbers //
            @Parameter(description = "List of business partner numbers.") //
            final List<String> businessPartnerNumbers, //
            // There seems to be a bug concerning interpretation of delimiters in Spring.
            // If we pass only one search filter `?search=policyId,EQUALS,policy1`,
            // the parts of this search filter are split up into several search filters even if we encode the comma.
            // It only works if multiple search filters are passed `?search=policyId,EQUALS,policy1&search=...`.
            // The solution described on stackoverflow
            // (https://stackoverflow.com/questions/37058691/encoded-comma-in-url-is-read-as-list-in-spring)
            // using the annotation Delimiter did not work either.
            // Therefore, we read the search parameters from the request manually.
            final HttpServletRequest request) {

        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size too large");
        }

        // TODO (mfischer): #639: add test coverage for this block
        final List<String> searchParameters = Arrays.asList(request.getParameterMap().get("search"));
        final List<SearchCriteria<?>> searchCriteria = new SearchParameterParser(searchParameters).getSearchCriteria();
        final Map<String, List<Policy>> bpnToPoliciesMap = service.getPolicies(businessPartnerNumbers);
        final Page<PolicyWithBpn> policies = policyPagingService.getPolicies(bpnToPoliciesMap, pageable,
                searchCriteria);
        return policies.map(PolicyResponse::fromPolicyWithBpn);
    }

    @Operation(operationId = "deleteAllowedPolicy",
               summary = "Removes a policy that should no longer be accepted in EDC negotiation.",
               security = @SecurityRequirement(name = "api_key"), tags = { "Item Relationship Service" },
               description = "Removes a policy that should no longer be accepted in EDC negotiation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200"),
                            @ApiResponse(responseCode = "400", description = "Policy deletion failed.",
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-400"))
                                         }),
                            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-401"))
                                         }),
                            @ApiResponse(responseCode = "403", description = FORBIDDEN_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-403"))
                                         }),
    })
    @DeleteMapping("/policies/{policyId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('" + IrsRoles.ADMIN_IRS + "')")
    public void deleteAllowedPolicy(@ValidPolicyId @PathVariable("policyId") final String policyId) {
        service.deletePolicy(policyId);
    }

    @Operation(operationId = "removeAllowedPolicyFromBpnl",
               summary = "Removes a policy from BPNL that should no longer be accepted in EDC negotiation.",
               security = @SecurityRequirement(name = "api_key"), tags = { "Item Relationship Service" },
               description = "Removes a policy from BPNL that should no longer be accepted in EDC negotiation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200"),
                            @ApiResponse(responseCode = "400", description = "Policy deletion failed.",
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-400"))
                                         }),
                            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-401"))
                                         }),
                            @ApiResponse(responseCode = "403", description = FORBIDDEN_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-403"))
                                         }),
    })
    @DeleteMapping("/policies/{policyId}/bpnl/{bpnl}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('" + IrsRoles.ADMIN_IRS + "')")
    public void removeAllowedPolicyFromBpnl(@PathVariable("policyId") final String policyId, //
            @Pattern(regexp = BPN_REGEX, message = " Invalid BPN.") //
            @PathVariable("bpnl") final String bpnl) {
        service.deletePolicyForEachBpn(policyId, List.of(bpnl));
    }

    @Operation(operationId = "updateAllowedPolicy", summary = "Updates existing policies.",
               security = @SecurityRequirement(name = "api_key"), tags = { "Item Relationship Service" },
               description = "Updates existing policies.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200"),
                            @ApiResponse(responseCode = "500",
                                         description = "Updating policies failed due to an internal error.",
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-500"))
                                         }),
                            @ApiResponse(responseCode = "400", description = "Policy update failed.",
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-400"))
                                         }),
                            @ApiResponse(responseCode = "401", description = UNAUTHORIZED_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-401"))
                                         }),
                            @ApiResponse(responseCode = "403", description = FORBIDDEN_DESC,
                                         content = { @Content(mediaType = APPLICATION_JSON_VALUE,
                                                              schema = @Schema(implementation = ErrorResponse.class),
                                                              examples = @ExampleObject(name = "error",
                                                                                        ref = "#/components/examples/error-response-403"))
                                         }),
    })
    @PutMapping("/policies")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('" + IrsRoles.ADMIN_IRS + "')")
    public void updateAllowedPolicy(@Valid @RequestBody final UpdatePolicyRequest request) {
        service.updatePolicies(request);
    }

}
