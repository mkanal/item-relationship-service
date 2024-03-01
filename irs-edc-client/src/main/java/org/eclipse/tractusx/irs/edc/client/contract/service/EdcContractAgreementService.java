/********************************************************************************
 * Copyright (c) 2022,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
package org.eclipse.tractusx.irs.edc.client.contract.service;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.tractusx.irs.edc.client.EdcConfiguration;
import org.eclipse.tractusx.irs.edc.client.EdcConfiguration.ControlplaneConfig.EndpointConfig;
import org.eclipse.tractusx.irs.edc.client.contract.model.EdcContractAgreementsResponse;
import org.eclipse.tractusx.irs.edc.client.contract.model.exception.ContractAgreementException;
import org.eclipse.tractusx.irs.edc.client.contract.model.exception.EdcContractAgreementRequest;
import org.eclipse.tractusx.irs.edc.client.contract.model.exception.EdcContractAgreementRequest.EdcContractAgreementFilterExpression;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * EdcContractAgreementService used for contract agreements and contract agreement negotiation details
 */
@Slf4j
@RequiredArgsConstructor
@Service("irsEdcClientEdcContractAgreementService")
public class EdcContractAgreementService {

    public static final String EDC_REQUEST_SUFFIX = "/request";
    public static final String EDC_ASSET_ID = "https://w3id.org/edc/v0.0.1/ns/assetId";
    private final EdcConfiguration config;
    private final @Qualifier("edcClientRestTemplate") RestTemplate edcRestTemplate;

    public List<ContractAgreement> getContractAgreements(final String... contractAgreementIds)
            throws ContractAgreementException {

        final EdcContractAgreementRequest edcContractAgreementRequest = buildContractAgreementRequest(
                contractAgreementIds);

        final EndpointConfig endpoint = config.getControlplane().getEndpoint();
        final String contractAgreements = endpoint.getContractAgreements();
        final ResponseEntity<EdcContractAgreementsResponse> edcContractAgreementListResponseEntity = edcRestTemplate.exchange(
                endpoint.getData() + contractAgreements + EDC_REQUEST_SUFFIX, HttpMethod.POST,
                new HttpEntity<>(edcContractAgreementRequest, headers()), EdcContractAgreementsResponse.class);

        final EdcContractAgreementsResponse contractAgreementListWrapper = edcContractAgreementListResponseEntity.getBody();
        if (contractAgreementListWrapper != null) {
            return contractAgreementListWrapper.getContractAgreementList();
        } else {
            throw new ContractAgreementException(
                    "Empty message body on edc response: " + edcContractAgreementListResponseEntity);
        }

    }

    public ContractNegotiation getContractAgreementNegotiation(final String contractAgreementId) {
        final EndpointConfig endpoint = config.getControlplane().getEndpoint();
        final String contractAgreements = endpoint.getContractAgreements();
        final ResponseEntity<ContractNegotiation> contractNegotiationResponseEntity = edcRestTemplate.exchange(
                endpoint.getData() + contractAgreements + "/" + contractAgreementId + "/negotiation", HttpMethod.GET,
                new HttpEntity<>(headers()), ContractNegotiation.class);
        return contractNegotiationResponseEntity.getBody();
    }

    private EdcContractAgreementRequest buildContractAgreementRequest(final String... contractAgreementIds) {

        final List<EdcContractAgreementFilterExpression> list = Arrays.stream(contractAgreementIds)
                                                                      .map(s -> new EdcContractAgreementFilterExpression(
                                                                              EDC_ASSET_ID, "=", s))
                                                                      .toList();

        return new EdcContractAgreementRequest(list);
    }

    private HttpHeaders headers() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        final String apiKeyHeader = config.getControlplane().getApiKey().getHeader();
        if (apiKeyHeader != null) {
            headers.add(apiKeyHeader, config.getControlplane().getApiKey().getSecret());
        }
        return headers;
    }

}