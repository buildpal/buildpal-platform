/*
 * Copyright 2017 Buildpal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.buildpal.core.query.operation;

import io.buildpal.core.query.operand.Operand;

import java.time.Instant;

public class InstantOperation implements Operation {

    private InstantOperation() {}

    public static final InstantOperation SELF = new InstantOperation();

    public Boolean eq(Operand leftOperand, Operand rightOperand) {
        if (leftOperand.getValue() == null) return false;

        return leftOperand.toInstant().equals(rightOperand.getInstantValue());
    }

    public Boolean ne(Operand leftOperand, Operand rightOperand) {
        if (leftOperand.getValue() == null) return false;

        return !leftOperand.toInstant().equals(rightOperand.getInstantValue());
    }

    public Boolean gt(Operand leftOperand, Operand rightOperand) {
        if (leftOperand.getValue() == null) return false;

        return leftOperand.toInstant().isAfter(rightOperand.getInstantValue());
    }

    public Boolean lt(Operand leftOperand, Operand rightOperand) {
        if (leftOperand.getValue() == null) return false;

        return leftOperand.toInstant().isBefore(rightOperand.getInstantValue());
    }

    public Boolean ge(Operand leftOperand, Operand rightOperand) {
        if (leftOperand.getValue() == null) return false;

        Instant left = leftOperand.toInstant();

        boolean isEqual = left.equals(rightOperand.getInstantValue());

        return isEqual || left.isAfter(rightOperand.getInstantValue());
    }

    public Boolean le(Operand leftOperand, Operand rightOperand) {
        if (leftOperand.getValue() == null) return false;

        Instant left = leftOperand.toInstant();

        boolean isEqual = left.equals(rightOperand.getInstantValue());

        return isEqual || left.isBefore(rightOperand.getInstantValue());
    }

    public Boolean co(Operand leftOperand, Operand rightOperand) {
        throw new UnsupportedOperationException("co is not a supported operator on instant.");
    }

    public Boolean sw(Operand leftOperand, Operand rightOperand) {
        throw new UnsupportedOperationException("sw is not a supported operator on instant.");
    }

    public Boolean ew(Operand leftOperand, Operand rightOperand) {
        throw new UnsupportedOperationException("ew is not a supported operator on instant.");
    }
}

