/********************************************************************************
 * Copyright (c) 2021,2022,2023
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 *       2022,2023: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022,2023: BOSCH AG
 * Copyright (c) 2021,2022,2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0. *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.irs.policystore.controllers;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.eclipse.tractusx.irs.policystore.models.Constraints;
import org.eclipse.tractusx.irs.policystore.models.CreatePolicyRequest;
import org.eclipse.tractusx.irs.policystore.models.LeftOperand;
import org.eclipse.tractusx.irs.policystore.models.LogicalConstraintType;
import org.eclipse.tractusx.irs.policystore.models.OperatorType;
import org.eclipse.tractusx.irs.policystore.models.Permission;
import org.eclipse.tractusx.irs.policystore.models.Policy;
import org.eclipse.tractusx.irs.policystore.models.PolicyType;
import org.eclipse.tractusx.irs.policystore.models.RightOperand;
import org.eclipse.tractusx.irs.policystore.models.UpdatePolicyRequest;
import org.eclipse.tractusx.irs.policystore.services.PolicyStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicyStoreControllerTest {

    private PolicyStoreController testee;

    @Mock
    private PolicyStoreService service;

    @BeforeEach
    void setUp() {
        testee = new PolicyStoreController(service);
    }

    @Test
    void registerAllowedPolicy() {
        // arrange
        final CreatePolicyRequest request = new CreatePolicyRequest("policyId", OffsetDateTime.now(), createPermissions());

        // act
        testee.registerAllowedPolicy(request);

        // assert
        verify(service).registerPolicy(request);
    }

    @Test
    void getPolicies() {
        // arrange
        final List<Policy> policies = List.of(new Policy("testId", OffsetDateTime.now(), OffsetDateTime.now(), createPermissions()));
        when(service.getStoredPolicies()).thenReturn(policies);

        // act
        final var returnedPolicies = testee.getPolicies();

        // assert
        assertThat(returnedPolicies).isEqualTo(policies);
    }

    @Test
    void deleteAllowedPolicy() {
        // act
        testee.deleteAllowedPolicy("testId");

        // assert
        verify(service).deletePolicy("testId");
    }

    @Test
    void updateAllowedPolicy() {
        // arrange
        final UpdatePolicyRequest request = new UpdatePolicyRequest(OffsetDateTime.now());

        // act
        final String policyId = "policyId";
        testee.updateAllowedPolicy(policyId, request);

        // assert
        verify(service).updatePolicy(policyId, request);
    }

    private List<Permission> createPermissions() {
        return List.of(
                new Permission(PolicyType.USE, LogicalConstraintType.AND, List.of(createConstraints())),
                new Permission(PolicyType.ACCESS, LogicalConstraintType.OR, List.of(createConstraints()))
        );
    }

    private Constraints createConstraints() {
        return new Constraints(LeftOperand.BUSINESS_PARTNER_NUMBER, OperatorType.GT, new RightOperand("active", "1.0"));
    }

}