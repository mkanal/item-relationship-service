//
// Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
//
// See the AUTHORS file(s) distributed with this work for additional
// information regarding authorship.
//
// See the LICENSE file(s) distributed with this work for
// additional information regarding license terms.
//
package net.catenax.irs.aaswrapper.registry.domain;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import net.catenax.irs.dto.NodeType;
import net.catenax.irs.dto.ProcessingError;
import net.catenax.irs.dto.SubmodelType;
import org.springframework.stereotype.Service;

/**
 * Public API Facade for digital twin registry domain
 */
@RequiredArgsConstructor
@Service
public class DigitalTwinRegistryFacade {

    private final DigitalTwinRegistryClient digitalTwinRegistryClient;

    /**
     * Combines required data from Digital Twin Registry Service
     *
     * @param aasIdentifier The Asset Administration Shell's unique id
     * @return list of submodel addresses
     */
    public List<AbstractAAS> getAASSubmodelEndpoint(final String aasIdentifier) {
        List<AbstractAAS> result;
        try {
            final List<SubmodelDescriptor> submodelDescriptors = getSubmodelDescriptors(aasIdentifier);
            result = submodelDescriptors.stream()
                                        .filter(this::isAssemblyPartRelationship)
                                        .map(submodelDescriptor -> new AasSubmodelDescriptor(
                                                submodelDescriptor.getIdShort(), submodelDescriptor.getIdentification(),
                                                NodeType.NODE, SubmodelType.ASSEMBLY_PART_RELATIONSHIP,
                                                submodelDescriptor.getEndpoints()
                                                                  .get(0)
                                                                  .getProtocolInformation()
                                                                  .getEndpointAddress()))
                                        .collect(Collectors.toList());
            if (result.isEmpty()) {
                result = List.of(
                        getResponseTombStoneForResponse(aasIdentifier, "No AssemblyPartRelationship Descriptor found",
                                "Unknown"));
            }
        } catch (FeignException e) {
            result = List.of(
                    getResponseTombStoneForResponse(aasIdentifier, e.getMessage(), e.getClass().getSimpleName()));
        }
        return result;
    }

    private AbstractAAS getResponseTombStoneForResponse(final String catenaXId, final String errorDetail,
            final String exception) {
        final ProcessingError processingError = ProcessingError.builder()
                                                               .withException(exception)
                                                               .withErrorDetail(errorDetail)
                                                               .withLastAttempt(Instant.now())
                                                               .withRetryCounter(0)
                                                               .build();
        final String idShort = "";
        return new AasTombstone(idShort, catenaXId, processingError);
    }

    private List<SubmodelDescriptor> getSubmodelDescriptors(final String aasIdentifier) {
        return digitalTwinRegistryClient.getAssetAdministrationShellDescriptor(aasIdentifier).getSubmodelDescriptors();
    }

    /**
     * TODO: Adjust when we will know how to distinguish assembly part relationships
     *
     * @param submodelDescriptor the submodel descriptor
     * @return True, if AssemblyPartRelationship
     */
    private boolean isAssemblyPartRelationship(final SubmodelDescriptor submodelDescriptor) {
        final String assemblyPartRelationshipIdentifier = SubmodelType.ASSEMBLY_PART_RELATIONSHIP.getValue();
        return assemblyPartRelationshipIdentifier.equals(
                submodelDescriptor.getSemanticId().getValue().stream().findFirst().orElse(null));
    }
}
