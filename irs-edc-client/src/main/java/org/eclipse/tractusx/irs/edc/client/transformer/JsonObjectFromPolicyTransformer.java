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
package org.eclipse.tractusx.irs.edc.client.transformer;

import static java.util.UUID.randomUUID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_TYPE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_AND_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNEE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNER_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSEQUENCE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_DUTY_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_INCLUDED_IN_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OR_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_AGREEMENT;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_REFINEMENT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_XONE_CONSTRAINT_ATTRIBUTE;

import java.util.Optional;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.policy.model.Prohibition;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.policy.model.XoneConstraint;
import org.eclipse.edc.spi.agent.ParticipantIdMapper;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Transforms a {@link Policy} to an ODRL type as a {@link JsonObject} in expanded JSON-LD form.
 */
@SuppressWarnings({ "PMD.TooManyStaticImports",
                    "PMD.ExcessiveImports",
                    "PMD.TooManyMethods"
})
public class JsonObjectFromPolicyTransformer extends AbstractJsonLdTransformer<Policy, JsonObject> {
    private final JsonBuilderFactory jsonFactory;
    private final ParticipantIdMapper participantIdMapper;

    public JsonObjectFromPolicyTransformer(final JsonBuilderFactory jsonFactory,
            final ParticipantIdMapper participantIdMapper) {
        super(Policy.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
        this.participantIdMapper = participantIdMapper;
    }

    @Override
    public @Nullable JsonObject transform(final @NotNull Policy policy, final @NotNull TransformerContext context) {
        return policy.accept(new Visitor(jsonFactory, participantIdMapper));
    }

    /**
     * Walks the policy object model, transforming it to a JsonObject.
     */
    private static class Visitor
            implements Policy.Visitor<JsonObject>, Rule.Visitor<JsonObject>, Constraint.Visitor<JsonObject>,
            Expression.Visitor<JsonObject> {
        private final JsonBuilderFactory jsonFactory;
        private final ParticipantIdMapper participantIdMapper;

        /* default */ Visitor(final JsonBuilderFactory jsonFactory, final ParticipantIdMapper participantIdMapper) {
            this.jsonFactory = jsonFactory;
            this.participantIdMapper = participantIdMapper;
        }

        @Override
        public JsonObject visitAndConstraint(final AndConstraint andConstraint) {
            return visitMultiplicityConstraint(ODRL_AND_CONSTRAINT_ATTRIBUTE, andConstraint);
        }

        @Override
        public JsonObject visitOrConstraint(final OrConstraint orConstraint) {
            return visitMultiplicityConstraint(ODRL_OR_CONSTRAINT_ATTRIBUTE, orConstraint);
        }

        @Override
        public JsonObject visitXoneConstraint(final XoneConstraint xoneConstraint) {
            return visitMultiplicityConstraint(ODRL_XONE_CONSTRAINT_ATTRIBUTE, xoneConstraint);
        }

        private JsonObject visitMultiplicityConstraint(final String operandType,
                final MultiplicityConstraint multiplicityConstraint) {
            final var constraintsBuilder = jsonFactory.createArrayBuilder();
            for (final var constraint : multiplicityConstraint.getConstraints()) {
                Optional.of(constraint).map(c -> c.accept(this)).ifPresent(constraintsBuilder::add);
            }

            return jsonFactory.createObjectBuilder().add(operandType, constraintsBuilder.build()).build();
        }

        @Override
        public JsonObject visitAtomicConstraint(final AtomicConstraint atomicConstraint) {
            final var constraintBuilder = jsonFactory.createObjectBuilder();

            constraintBuilder.add(ODRL_LEFT_OPERAND_ATTRIBUTE, atomicConstraint.getLeftExpression().accept(this));
            final var operator = atomicConstraint.getOperator().getOdrlRepresentation();
            constraintBuilder.add(ODRL_OPERATOR_ATTRIBUTE,
                    jsonFactory.createArrayBuilder().add(jsonFactory.createObjectBuilder().add(ID, operator)));
            constraintBuilder.add(ODRL_RIGHT_OPERAND_ATTRIBUTE, atomicConstraint.getRightExpression().accept(this));

            return constraintBuilder.build();
        }

        @Override
        public JsonObject visitLiteralExpression(final LiteralExpression expression) {
            return jsonFactory.createObjectBuilder()
                              .add(VALUE, Json.createValue(expression.getValue().toString()))
                              .build();
        }

        @Override
        public JsonObject visitPolicy(final Policy policy) {
            final var permissionsBuilder = jsonFactory.createArrayBuilder();
            policy.getPermissions().forEach(permission -> permissionsBuilder.add(permission.accept(this)));

            final var prohibitionsBuilder = jsonFactory.createArrayBuilder();
            policy.getProhibitions().forEach(prohibition -> prohibitionsBuilder.add(prohibition.accept(this)));

            final var obligationsBuilder = jsonFactory.createArrayBuilder();
            policy.getObligations().forEach(duty -> obligationsBuilder.add(duty.accept(this)));

            final var builder = jsonFactory.createObjectBuilder()
                                           .add(ID, randomUUID().toString())
                                           .add(TYPE, getTypeAsString(policy.getType()))
                                           .add(ODRL_PERMISSION_ATTRIBUTE, permissionsBuilder)
                                           .add(ODRL_PROHIBITION_ATTRIBUTE, prohibitionsBuilder)
                                           .add(ODRL_OBLIGATION_ATTRIBUTE, obligationsBuilder);

            Optional.ofNullable(policy.getAssignee())
                    .map(participantIdMapper::toIri)
                    .ifPresent(target -> builder.add(ODRL_ASSIGNEE_ATTRIBUTE,
                            jsonFactory.createArrayBuilder().add(jsonFactory.createObjectBuilder().add(ID, target))));
            Optional.ofNullable(policy.getAssigner())
                    .map(participantIdMapper::toIri)
                    .ifPresent(target -> builder.add(ODRL_ASSIGNER_ATTRIBUTE,
                            jsonFactory.createArrayBuilder().add(jsonFactory.createObjectBuilder().add(ID, target))));
            Optional.ofNullable(policy.getTarget())
                    .ifPresent(target -> builder.add(ODRL_TARGET_ATTRIBUTE,
                            jsonFactory.createArrayBuilder().add(jsonFactory.createObjectBuilder().add(ID, target))));

            return builder.build();
        }

        @Override
        public JsonObject visitPermission(final Permission permission) {
            final var permissionBuilder = visitRule(permission);

            if (permission.getDuties() != null && !permission.getDuties().isEmpty()) {
                final var dutiesBuilder = jsonFactory.createArrayBuilder();
                for (final var duty : permission.getDuties()) {
                    dutiesBuilder.add(visitDuty(duty));
                }
                permissionBuilder.add(ODRL_DUTY_ATTRIBUTE, dutiesBuilder.build());
            }

            return permissionBuilder.build();
        }

        @Override
        public JsonObject visitProhibition(final Prohibition prohibition) {
            final var prohibitionBuilder = visitRule(prohibition);

            return prohibitionBuilder.build();
        }

        @Override
        public JsonObject visitDuty(final Duty duty) {
            final var obligationBuilder = visitRule(duty);

            if (duty.getConsequence() != null) {
                final var consequence = visitDuty(duty.getConsequence());
                obligationBuilder.add(ODRL_CONSEQUENCE_ATTRIBUTE, consequence);
            }

            return obligationBuilder.build();
        }

        private JsonObjectBuilder visitRule(final Rule rule) {
            final var ruleBuilder = jsonFactory.createObjectBuilder();

            ruleBuilder.add(ODRL_ACTION_ATTRIBUTE, visitAction(rule.getAction()));
            if (rule.getConstraints() != null && !rule.getConstraints().isEmpty()) {
                ruleBuilder.add(ODRL_CONSTRAINT_ATTRIBUTE, visitConstraints(rule));
            }

            return ruleBuilder;
        }

        private JsonArray visitConstraints(final Rule rule) {
            final var constraintsBuilder = jsonFactory.createArrayBuilder();

            for (final var constraint : rule.getConstraints()) {
                Optional.of(constraint).map(c -> c.accept(this)).ifPresent(constraintsBuilder::add);
            }

            return constraintsBuilder.build();
        }

        private JsonObject visitAction(final @Nullable Action action) {
            final var actionBuilder = jsonFactory.createObjectBuilder();
            if (action == null) {
                return actionBuilder.build();
            }
            actionBuilder.add(ODRL_ACTION_TYPE_ATTRIBUTE, action.getType());
            if (action.getIncludedIn() != null) {
                actionBuilder.add(ODRL_INCLUDED_IN_ATTRIBUTE, action.getIncludedIn());
            }
            if (action.getConstraint() != null) {
                actionBuilder.add(ODRL_REFINEMENT_ATTRIBUTE, action.getConstraint().accept(this));
            }
            return actionBuilder.build();
        }

        private String getTypeAsString(final PolicyType type) {
            return switch (type) {
                case SET -> ODRL_POLICY_TYPE_SET;
                case OFFER -> ODRL_POLICY_TYPE_OFFER;
                case CONTRACT -> ODRL_POLICY_TYPE_AGREEMENT;
            };
        }

    }

}