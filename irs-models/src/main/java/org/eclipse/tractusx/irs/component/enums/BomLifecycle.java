/********************************************************************************
 * Copyright (c) 2021,2022
 *       2022: Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *       2022: ZF Friedrichshafen AG
 *       2022: ISTOS GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.irs.component.enums;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/***
 * API type for the view of the items tree to be returned by a query.
 *
 */
@JsonSerialize(using = ToStringSerializer.class)
@Schema(description = "The lifecycle context in which the child part was assembled into the parent part.")
@Getter
public enum BomLifecycle {
    AS_BUILT("asBuilt", "AsBuilt", AspectType.SERIAL_PART_TYPIZATION),
    AS_PLANNED("asPlanned", "AsPlanned", AspectType.PART_AS_PLANNED);

    private final String name;
    private final String lifecycleContextCharacteristicValue;
    private final AspectType defaultAspect;

    BomLifecycle(final String name, final String lifecycleContextCharacteristicValue, final AspectType aspectType) {
        this.name = name;
        this.lifecycleContextCharacteristicValue = lifecycleContextCharacteristicValue;
        this.defaultAspect = aspectType;
    }

    /**
     * of as a substitute/alias for valueOf handling the default value
     *
     * @param value see {@link #name}
     * @return the corresponding BomLifecycle
     */
    public static BomLifecycle value(final String value) {
        return BomLifecycle.valueOf(value);
    }

    @JsonCreator
    public static BomLifecycle fromValue(final String value) {
        return Stream.of(BomLifecycle.values())
                     .filter(bomLifecycle -> bomLifecycle.name.equals(value))
                     .findFirst()
                     .orElseThrow(() -> new NoSuchElementException("Unsupported BomLifecycle: " + value
                             + ". Must be one of: " + supportedBomLifecycles()));
    }

    private static String supportedBomLifecycles() {
        return Stream.of(BomLifecycle.values()).map(bomLifecycle -> bomLifecycle.name).collect(Collectors.joining(", "));
    }

    public static BomLifecycle fromLifecycleContextCharacteristic(final String value) {
        return Stream.of(BomLifecycle.values())
                     .filter(bomLifecycle -> bomLifecycle.lifecycleContextCharacteristicValue.equals(value))
                     .findFirst()
                     .orElseThrow();
    }

    /**
     * @return convert BomLifecycle to string value
     */
    @Override
    public String toString() {
        return name;
    }
}
