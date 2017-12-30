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

package io.buildpal.core.query;

import io.buildpal.core.query.operand.Operand;
import io.buildpal.core.query.operation.BooleanOperation;
import io.buildpal.core.query.operation.DoubleOperation;
import io.buildpal.core.query.operation.LongOperation;
import io.buildpal.core.query.operation.NullOperation;
import io.buildpal.core.query.operation.Operation;
import io.buildpal.core.query.operation.StringOperation;
import io.vertx.core.json.JsonObject;

class QueryEvaluator extends QueryBaseVisitor<Boolean> {

    private static final String AND = "and";
    private static final String OR = "or";

    private JsonObject currentItem;

    private Operation currentOperation;
    private Operand leftOperand = new Operand();
    private Operand rightOperand = new Operand();

    public QueryEvaluator setCurrentItem(JsonObject item) {
        this.currentItem = item;
        return this;
    }

    @Override
    public Boolean visitParenExp(QueryParser.ParenExpContext ctx) {
        Boolean result = visit(ctx.filter());
        return ctx.NOT() != null ? !result : result;
    }

    @Override
    public Boolean visitLogicalExp(QueryParser.LogicalExpContext ctx) {
        Boolean leftExp = visit(ctx.filter(0));

        if (leftExp) {
            if (OR.equals(ctx.LOGICAL_OPERATOR().getText())) {
                // Short circuit "or"
                return leftExp;

            } else {
                return leftExp && visit(ctx.filter(1));
            }

        } else {
            if (AND.equals(ctx.LOGICAL_OPERATOR().getText())) {
                // Short circuit "and"
                return leftExp;

            } else {
                return leftExp || visit(ctx.filter(1));
            }
        }
    }

    @Override
    public Boolean visitPresentExp(QueryParser.PresentExpContext ctx) {
        visit(ctx.attrPath());
        return leftOperand.getValue() != null;
    }

    @Override
    public Boolean visitCompareExp(QueryParser.CompareExpContext ctx) {
        // Get the left operand.
        visit(ctx.attrPath());

        // Get the right operand.
        visit(ctx.value());

        try {

            switch (ctx.op.getType()) {
                case QueryParser.EQ:
                    return currentOperation.eq(leftOperand, rightOperand);

                case QueryParser.NE:
                    return currentOperation.ne(leftOperand, rightOperand);

                case QueryParser.GT:
                    return currentOperation.gt(leftOperand, rightOperand);

                case QueryParser.LT:
                    return currentOperation.lt(leftOperand, rightOperand);

                case QueryParser.GE:
                    return currentOperation.ge(leftOperand, rightOperand);

                case QueryParser.LE:
                    return currentOperation.le(leftOperand, rightOperand);

                case QueryParser.CO:
                    return currentOperation.co(leftOperand, rightOperand);

                case QueryParser.SW:
                    return currentOperation.sw(leftOperand, rightOperand);

                case QueryParser.EW:
                    return currentOperation.ew(leftOperand, rightOperand);

                default:
                    throw new IllegalStateException("Unsupported operator detected.");
            }


        } catch (Exception ex) {
            // TODO: Should we return false since the grammar is not wrong here?
            throw new IllegalStateException("Unable to execute the query.", ex);
        }
    }

    @Override
    public Boolean visitAttrPath(QueryParser.AttrPathContext ctx) {
        if (ctx.subAttr() == null || ctx.subAttr().isEmpty()) {
            leftOperand.setValue(currentItem.getValue(ctx.ATTRNAME().getText()));

        } else {
            throw new UnsupportedOperationException("Sub attribute path is not implemented yet.");
        }

        return true;
    }

    @Override
    public Boolean visitBoolean(QueryParser.BooleanContext ctx) {
        currentOperation = BooleanOperation.SELF;
        rightOperand.setBoolValue(Boolean.parseBoolean(ctx.getText()));

        return true;
    }

    @Override
    public Boolean visitNull(QueryParser.NullContext ctx) {
        currentOperation = NullOperation.SELF;
        rightOperand.setValue(null);
        return true;
    }

    @Override
    public Boolean visitString(QueryParser.StringContext ctx) {
        currentOperation = StringOperation.SELF;

        // Remove quotes. At a minimum the string length will be 2 (two quotes).
        if (ctx.getText().length() > 2) {
            rightOperand.setStrValue(ctx.getText().substring(1, ctx.getText().length() - 1));
        } else {
            rightOperand.setStrValue("");
        }

        return true;
    }

    @Override
    public Boolean visitDouble(QueryParser.DoubleContext ctx) {
        currentOperation = DoubleOperation.SELF;
        rightOperand.setDoubleValue(Double.parseDouble(ctx.getText()));
        return true;
    }

    @Override
    public Boolean visitLong(QueryParser.LongContext ctx) {
        currentOperation = LongOperation.SELF;
        rightOperand.setLongValue(Long.parseLong(ctx.getText()));
        return true;
    }
}
