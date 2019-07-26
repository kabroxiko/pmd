/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.plsql.rule.codestyle;

import static net.sourceforge.pmd.properties.PropertyFactory.booleanProperty;
import static net.sourceforge.pmd.properties.constraints.NumericConstraints.inRange;
import static org.apache.commons.lang3.StringUtils.split;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jaxen.JaxenException;

import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.plsql.ast.ASTArgument;
import net.sourceforge.pmd.lang.plsql.ast.ASTArgumentList;
import net.sourceforge.pmd.lang.plsql.ast.ASTBulkCollectIntoClause;
import net.sourceforge.pmd.lang.plsql.ast.ASTCaseExpression;
import net.sourceforge.pmd.lang.plsql.ast.ASTCursorUnit;
import net.sourceforge.pmd.lang.plsql.ast.ASTDatatype;
import net.sourceforge.pmd.lang.plsql.ast.ASTDirectory;
import net.sourceforge.pmd.lang.plsql.ast.ASTElseCaseExpression;
import net.sourceforge.pmd.lang.plsql.ast.ASTEqualityExpression;
import net.sourceforge.pmd.lang.plsql.ast.ASTExceptionDeclaration;
import net.sourceforge.pmd.lang.plsql.ast.ASTExceptionHandler;
import net.sourceforge.pmd.lang.plsql.ast.ASTExceptionHandlersBegin;
import net.sourceforge.pmd.lang.plsql.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.plsql.ast.ASTFormalParameter;
import net.sourceforge.pmd.lang.plsql.ast.ASTFormalParameters;
import net.sourceforge.pmd.lang.plsql.ast.ASTFromClause;
import net.sourceforge.pmd.lang.plsql.ast.ASTID;
import net.sourceforge.pmd.lang.plsql.ast.ASTInput;
import net.sourceforge.pmd.lang.plsql.ast.ASTIntoClause;
import net.sourceforge.pmd.lang.plsql.ast.ASTJoinClause;
import net.sourceforge.pmd.lang.plsql.ast.ASTMethodDeclarator;
import net.sourceforge.pmd.lang.plsql.ast.ASTNextCondition;
import net.sourceforge.pmd.lang.plsql.ast.ASTOutOfLineConstraint;
import net.sourceforge.pmd.lang.plsql.ast.ASTPackageBody;
import net.sourceforge.pmd.lang.plsql.ast.ASTPackageSpecification;
import net.sourceforge.pmd.lang.plsql.ast.ASTParameterName;
import net.sourceforge.pmd.lang.plsql.ast.ASTParameterType;
import net.sourceforge.pmd.lang.plsql.ast.ASTProgramUnit;
import net.sourceforge.pmd.lang.plsql.ast.ASTQueryBlock;
import net.sourceforge.pmd.lang.plsql.ast.ASTSearchedCaseExpression;
import net.sourceforge.pmd.lang.plsql.ast.ASTSelectIntoStatement;
import net.sourceforge.pmd.lang.plsql.ast.ASTSelectList;
import net.sourceforge.pmd.lang.plsql.ast.ASTSelectStatement;
import net.sourceforge.pmd.lang.plsql.ast.ASTSequence;
import net.sourceforge.pmd.lang.plsql.ast.ASTSimpleCaseExpression;
import net.sourceforge.pmd.lang.plsql.ast.ASTSqlExpression;
import net.sourceforge.pmd.lang.plsql.ast.ASTStatement;
import net.sourceforge.pmd.lang.plsql.ast.ASTSubTypeDefinition;
import net.sourceforge.pmd.lang.plsql.ast.ASTSubqueryOperation;
import net.sourceforge.pmd.lang.plsql.ast.ASTSynonym;
import net.sourceforge.pmd.lang.plsql.ast.ASTTable;
import net.sourceforge.pmd.lang.plsql.ast.ASTTableName;
import net.sourceforge.pmd.lang.plsql.ast.ASTTableReference;
import net.sourceforge.pmd.lang.plsql.ast.ASTTriggerUnit;
import net.sourceforge.pmd.lang.plsql.ast.ASTTypeSpecification;
import net.sourceforge.pmd.lang.plsql.ast.ASTUnqualifiedID;
import net.sourceforge.pmd.lang.plsql.ast.ASTVariableName;
import net.sourceforge.pmd.lang.plsql.ast.ASTVariableOrConstantDeclarator;
import net.sourceforge.pmd.lang.plsql.ast.ASTView;
import net.sourceforge.pmd.lang.plsql.ast.ASTWhereClause;
import net.sourceforge.pmd.lang.plsql.ast.ConstraintType;
import net.sourceforge.pmd.lang.plsql.rule.AbstractPLSQLRule;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyFactory;


public class CodeFormatRule extends AbstractPLSQLRule {

    private static final PropertyDescriptor<Integer> INDENTATION_PROPERTY = PropertyFactory.intProperty("indentation")
            .desc("Indentation to be used for blocks").defaultValue(2).require(inRange(0, 32)).build();
    private static final PropertyDescriptor<Boolean> FILENAME_CHECK_PROPERTY = booleanProperty("filenameCheck").defaultValue(false)
            .desc("Check if filename and object have the same name").build();
    private static final PropertyDescriptor<String> QUERY_ALIGNMENT = PropertyFactory.stringProperty("queryAlignment")
            .desc("Indentation of the query keywords. Possible values: \\[Left, Indent\\]").defaultValue("Indent").build();
    private static final PropertyDescriptor<Boolean> PARAMETER_ON_NEW_LINE_PROPERTY = booleanProperty("parameterOnNewLine").defaultValue(true)
            .desc("Check parameters on new line").build();
    private static final PropertyDescriptor<Integer> MAX_PARAMETERS_IN_LINE_PROPERTY = PropertyFactory.intProperty("maxParametersInLine").defaultValue(3)
            .desc("Max number of parameters in the same line").build();
    private static final PropertyDescriptor<Boolean> JOIN_CONDITION_ON_NEW_LINE_PROPERTY = booleanProperty("joinConditionOnNewLine").defaultValue(false)
            .desc("Join Conditions on new line").build();
    private static final PropertyDescriptor<String> PARAMETER_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("parameterNameFormat").defaultValue(".*;.*;.*;.*")
            .desc("Parameter name format").build();
    private static final PropertyDescriptor<String> GLOBAL_VARIABLE_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("globalVariableNameFormat").defaultValue(".*")
            .desc("Global variable name format").build();
    private static final PropertyDescriptor<String> VARIABLE_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("localVariableNameFormat").defaultValue(".*")
            .desc("Local variable name format").build();
    private static final PropertyDescriptor<String> CURSOR_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("cursorNameFormat").defaultValue(".*")
            .desc("Cursor name format").build();
    private static final PropertyDescriptor<String> TYPE_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("typeDefinitionNameFormat").defaultValue(".*")
            .desc("Type definition name format").build();
    private static final PropertyDescriptor<String> EXCEPTION_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("exceptionDeclarationNameFormat").defaultValue(".*")
            .desc("Exception definition name format").build();
    private static final PropertyDescriptor<String> SEQUENCE_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("sequenceNameFormat").defaultValue(".*")
            .desc("Sequence name format").build();
    private static final PropertyDescriptor<String> PROCEDURE_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("procedureNameFormat").defaultValue(".*")
            .desc("Procedure name format").build();
    private static final PropertyDescriptor<String> FUNCTION_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("functionNameFormat").defaultValue(".*")
            .desc("Function name format").build();
    private static final PropertyDescriptor<String> PACKAGE_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("packageNameFormat").defaultValue(".*")
            .desc("Package name format").build();
    private static final PropertyDescriptor<String> TYPE_SPEC_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("typeSpecNameFormat").defaultValue(".*")
            .desc("Type Specification name format").build();
    private static final PropertyDescriptor<String> VIEW_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("viewNameFormat").defaultValue(".*")
            .desc("View name format").build();
    private static final PropertyDescriptor<String> TRIGGER_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("triggerNameFormat").defaultValue(".*")
            .desc("Trigger name format").build();
    private static final PropertyDescriptor<String> DIRECTORY_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("directoryNameFormat").defaultValue(".*")
            .desc("Directory name format").build();
    private static final PropertyDescriptor<String> SYNONYM_NAME_FORMAT_PROPERTY = PropertyFactory.stringProperty("synonymNameFormat").defaultValue(".*")
            .desc("Synonym name format").build();

    private int indentation = INDENTATION_PROPERTY.defaultValue();
    private boolean filenameCheck = FILENAME_CHECK_PROPERTY.defaultValue();
    private String queryAlignment = QUERY_ALIGNMENT.defaultValue();
    private boolean parameterOnNewLine = PARAMETER_ON_NEW_LINE_PROPERTY.defaultValue();
    private int maxParametersInLine = MAX_PARAMETERS_IN_LINE_PROPERTY.defaultValue();
    private boolean joinConditionOnNewLine = JOIN_CONDITION_ON_NEW_LINE_PROPERTY.defaultValue();
    private String sequenceNameFormat = split(SEQUENCE_NAME_FORMAT_PROPERTY.defaultValue(), ";")[0];
    private String inputWithNoOutputParameterNameFormat = split(PARAMETER_NAME_FORMAT_PROPERTY.defaultValue(), ";")[0];
    private String inputParameterNameFormat = split(PARAMETER_NAME_FORMAT_PROPERTY.defaultValue(), ";")[1];
    private String outputParameterNameFormat = split(PARAMETER_NAME_FORMAT_PROPERTY.defaultValue(), ";")[2];
    private String inputOutputParameterNameFormat = split(PARAMETER_NAME_FORMAT_PROPERTY.defaultValue(), ";")[3];
    private String globalVariableNameFormat = GLOBAL_VARIABLE_NAME_FORMAT_PROPERTY.defaultValue();
    private String localVariableNameFormat = VARIABLE_NAME_FORMAT_PROPERTY.defaultValue();
    private String cursorNameFormat = CURSOR_NAME_FORMAT_PROPERTY.defaultValue();
    private String typeDefinitionNameFormat = TYPE_NAME_FORMAT_PROPERTY.defaultValue();
    private String exceptionDeclarationNameFormat = EXCEPTION_NAME_FORMAT_PROPERTY.defaultValue();
    private String procedureNameFormat = PROCEDURE_NAME_FORMAT_PROPERTY.defaultValue();
    private String functionNameFormat = FUNCTION_NAME_FORMAT_PROPERTY.defaultValue();
    private String packageNameFormat = PACKAGE_NAME_FORMAT_PROPERTY.defaultValue();
    private String typeSpecNameFormat = TYPE_SPEC_NAME_FORMAT_PROPERTY.defaultValue();
    private String viewNameFormat = VIEW_NAME_FORMAT_PROPERTY.defaultValue();
    private String triggerNameFormat = TRIGGER_NAME_FORMAT_PROPERTY.defaultValue();
    private String directoryNameFormat = DIRECTORY_NAME_FORMAT_PROPERTY.defaultValue();
    private String synonymNameFormat = SYNONYM_NAME_FORMAT_PROPERTY.defaultValue();

    public CodeFormatRule() {
        definePropertyDescriptor(INDENTATION_PROPERTY);
        definePropertyDescriptor(FILENAME_CHECK_PROPERTY);
        definePropertyDescriptor(QUERY_ALIGNMENT);
        definePropertyDescriptor(PARAMETER_ON_NEW_LINE_PROPERTY);
        definePropertyDescriptor(MAX_PARAMETERS_IN_LINE_PROPERTY);
        definePropertyDescriptor(JOIN_CONDITION_ON_NEW_LINE_PROPERTY);
        definePropertyDescriptor(SEQUENCE_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(PARAMETER_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(GLOBAL_VARIABLE_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(VARIABLE_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(CURSOR_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(TYPE_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(EXCEPTION_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(PROCEDURE_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(FUNCTION_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(PACKAGE_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(TYPE_SPEC_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(VIEW_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(TRIGGER_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(DIRECTORY_NAME_FORMAT_PROPERTY);
        definePropertyDescriptor(SYNONYM_NAME_FORMAT_PROPERTY);
    }

    @Override
    public Object visit(ASTInput node, Object data) {
        indentation = getProperty(INDENTATION_PROPERTY);
        filenameCheck = getProperty(FILENAME_CHECK_PROPERTY);
        queryAlignment = getProperty(QUERY_ALIGNMENT);
        parameterOnNewLine = getProperty(PARAMETER_ON_NEW_LINE_PROPERTY);
        maxParametersInLine = getProperty(MAX_PARAMETERS_IN_LINE_PROPERTY);
        joinConditionOnNewLine = getProperty(JOIN_CONDITION_ON_NEW_LINE_PROPERTY);
        sequenceNameFormat = getProperty(SEQUENCE_NAME_FORMAT_PROPERTY);
        inputWithNoOutputParameterNameFormat = split(getProperty(PARAMETER_NAME_FORMAT_PROPERTY), ";")[0];
        inputParameterNameFormat = split(getProperty(PARAMETER_NAME_FORMAT_PROPERTY), ";")[1];
        outputParameterNameFormat = split(getProperty(PARAMETER_NAME_FORMAT_PROPERTY), ";")[2];
        inputOutputParameterNameFormat = split(getProperty(PARAMETER_NAME_FORMAT_PROPERTY), ";")[3];
        globalVariableNameFormat = getProperty(GLOBAL_VARIABLE_NAME_FORMAT_PROPERTY);
        localVariableNameFormat = getProperty(VARIABLE_NAME_FORMAT_PROPERTY);
        cursorNameFormat = getProperty(CURSOR_NAME_FORMAT_PROPERTY);
        typeDefinitionNameFormat = getProperty(TYPE_NAME_FORMAT_PROPERTY);
        exceptionDeclarationNameFormat = getProperty(EXCEPTION_NAME_FORMAT_PROPERTY);
        procedureNameFormat = getProperty(PROCEDURE_NAME_FORMAT_PROPERTY);
        functionNameFormat = getProperty(FUNCTION_NAME_FORMAT_PROPERTY);
        packageNameFormat = getProperty(PACKAGE_NAME_FORMAT_PROPERTY);
        typeSpecNameFormat = getProperty(TYPE_SPEC_NAME_FORMAT_PROPERTY);
        viewNameFormat = getProperty(VIEW_NAME_FORMAT_PROPERTY);
        triggerNameFormat = getProperty(TRIGGER_NAME_FORMAT_PROPERTY);
        directoryNameFormat = getProperty(DIRECTORY_NAME_FORMAT_PROPERTY);
        synonymNameFormat = getProperty(SYNONYM_NAME_FORMAT_PROPERTY);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTSelectList node, Object data) {
        if (parameterOnNewLine) {
            Node parent = node.jjtGetParent();
            checkEachChildOnNextLine(data, node, parent.getBeginLine(), parent.getBeginColumn() + 7);
        } else {
            int line = node.jjtGetParent().getBeginLine();
            try {
                List<ASTSqlExpression> sqlExpressions = convertList(node.findChildNodesWithXPath("SqlExpression"), ASTSqlExpression.class);
                for (ASTSqlExpression sqlExpression : sqlExpressions) {
                    checkLineAndIndentation(data, sqlExpression, line, node.getBeginColumn(), sqlExpression.getImage());
                    line = sqlExpression.getEndLine() + 1;
                }
            } catch (JaxenException e) {
                return false;
            }
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTBulkCollectIntoClause node, Object data) {
        Node parent = node.jjtGetParent();
        if ("Left".equals(queryAlignment)) {
            checkIndentation(data, node, parent.getBeginColumn(), "BULK COLLECT INTO");
        } else {
            checkIndentation(data, node, parent.getBeginColumn() + indentation, "BULK COLLECT INTO");
        }
        checkEachChildOnNextLine(data, node, node.getBeginLine(), parent.getBeginColumn() + 7);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTIntoClause node, Object data) {
        if ("Left".equals(queryAlignment)) {
            checkIndentation(data, node, node.jjtGetParent().getBeginColumn(), "INTO");
            List<ASTVariableName> variableNames = node.findDescendantsOfType(ASTVariableName.class);
            for (ASTVariableName variableName : variableNames) {
                checkIndentation(data, variableName, node.getBeginColumn() + 7, variableName.getImage());
            }
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTCaseExpression node, Object data) {
        int line = node.getBeginLine();
        List<ASTSimpleCaseExpression> simpleCaseExpressions = node.findChildrenOfType(ASTSimpleCaseExpression.class);
        for (ASTSimpleCaseExpression simpleCaseExpression : simpleCaseExpressions) {
            checkLineAndIndentation(data, simpleCaseExpression, line + 1, node.getBeginColumn() + indentation, "WHEN");
            line++;
        }

        List<ASTSearchedCaseExpression> searchedCaseExpressions = node.findChildrenOfType(ASTSearchedCaseExpression.class);
        for (ASTSearchedCaseExpression when : searchedCaseExpressions) {
            line++;
            checkLineAndIndentation(data, when, line, node.getBeginColumn() + indentation, "WHEN");
            List<ASTNextCondition> conditions = when.findDescendantsOfType(ASTNextCondition.class);
            for (ASTNextCondition condition : conditions) {
                ASTCaseExpression parent = condition.getFirstParentOfType(ASTCaseExpression.class);
                if (Objects.equals(parent, node)) {
                    line++;
                    checkLineAndIndentation(data, condition.jjtGetChild(0), line, when.getBeginColumn() + 5, "Condition");
                } else {
                    line = parent.getEndLine();
                }
            }
            line = when.getEndLine();
        }

        ASTElseCaseExpression elseCaseExpression = node.getFirstChildOfType(ASTElseCaseExpression.class);
        if (elseCaseExpression != null) {
            checkLineAndIndentation(data, elseCaseExpression, line + 1, node.getBeginColumn() + indentation, "ELSE");
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTFromClause node, Object data) {
        if ("Left".equals(queryAlignment)) {
            Node parent = node.getFirstParentOfAnyType(ASTSelectStatement.class, ASTSelectIntoStatement.class, ASTQueryBlock.class);
            if (parent != null) {
                Node previousClause = parent.getFirstDescendantOfType(ASTBulkCollectIntoClause.class);
                if (previousClause == null) {
                    previousClause = parent.getFirstDescendantOfType(ASTIntoClause.class);
                    if (previousClause == null) {
                        previousClause = parent.getFirstDescendantOfType(ASTSelectList.class);
                    }
                }
                checkLineAndIndentation(data, node, previousClause.getEndLine() + 1, node.jjtGetParent().getBeginColumn(), "FROM");
            }
            try {
                List<ASTTableReference> tableReferences = convertList(node.findChildNodesWithXPath("TableReference"), ASTTableReference.class);
                for (ASTTableReference tableReference : tableReferences) {
                    ASTTableName tableName = tableReference.getFirstChildOfType(ASTTableName.class);
                    if (tableName != null) {
                        checkIndentation(data, tableReference, node.getBeginColumn() + 7, tableName.getImage() + " table reference");
                    }
                    List<ASTQueryBlock> queryBlocks = convertList(tableReference.findChildNodesWithXPath("QueryBlock"), ASTQueryBlock.class);
                    for (ASTQueryBlock queryBlock : queryBlocks) {
                        checkIndentation(data, queryBlock, node.getBeginColumn() + 8, "Subquery");
                    }
                }
            } catch (JaxenException e) {
                return false;
            }
        } else if ("Indent".equals(queryAlignment)) {
            checkIndentation(data, node, node.jjtGetParent().getBeginColumn() + indentation, "FROM");
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTWhereClause node, Object data) {
        if ("Left".equals(queryAlignment)) {
            checkIndentation(data, node, node.jjtGetParent().getBeginColumn(), "WHERE");
            try {
                List<ASTNextCondition> nextConditions = convertList(node
                        .findChildNodesWithXPath("Condition/CompoundCondition/NextCondition"), ASTNextCondition.class);
                for (ASTNextCondition nextCondition : nextConditions) {
                    checkIndentation(data, nextCondition, node.getBeginColumn(), nextCondition.getType());
                }
            } catch (JaxenException e) {
                return false;
            }
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTExceptionHandlersBegin node, Object data) {
        List<ASTExceptionHandler> exceptionHandlers = node.findDescendantsOfType(ASTExceptionHandler.class);
        for (ASTExceptionHandler exceptionHandler : exceptionHandlers) {
            if (exceptionHandler.getBeginColumn() != node.getBeginColumn() + this.indentation) {
                addViolationWithMessage(data, exceptionHandler, "WHEN should begin at column " + (node.getBeginColumn() + this.indentation));
            }
        }
        try {
            List<ASTStatement> statements = convertList(node.findChildNodesWithXPath("Statement"), ASTStatement.class);
            for (ASTStatement statement : statements) {
                checkIndentation(data, statement, node.getBeginColumn() + this.indentation * 2, "Statement");
            }
        } catch (JaxenException e) {
            return false;
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTJoinClause node, Object data) {
        // first child is the table reference
        Node tableReference = node.jjtGetChild(0);

        // remaining children are joins
        int lineNumber = tableReference.getBeginLine();
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            lineNumber++;
            Node child = node.jjtGetChild(i);
            if (child.getBeginLine() != lineNumber) {
                addViolationWithMessage(data, child, child.getXPathNodeName() + " should be on line " + lineNumber);
            }
            List<ASTEqualityExpression> conditions = child.findDescendantsOfType(ASTEqualityExpression.class);

            if (conditions.size() == 1 && !joinConditionOnNewLine) {
                // one condition should be on the same line
                ASTEqualityExpression singleCondition = conditions.get(0);
                if (singleCondition.getBeginLine() != lineNumber) {
                    addViolationWithMessage(data, child,
                            "Join condition \"" + singleCondition.getImage() + "\" should be on line " + lineNumber);
                }
            } else {
                // each condition on a separate line
                for (ASTEqualityExpression singleCondition : conditions) {
                    lineNumber++;
                    if (singleCondition.getBeginLine() != lineNumber) {
                        addViolationWithMessage(data, child,
                                "Join condition \"" + singleCondition.getImage() + "\" should be on line "
                                        + lineNumber);
                    }
                }
            }
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTSubqueryOperation node, Object data) {
        // get previous sibling
        int thisIndex = node.jjtGetChildIndex();
        Node prevSibling = node.jjtGetParent().jjtGetChild(thisIndex - 1);

        checkIndentation(data, node, prevSibling.getBeginColumn(), node.getImage());

        // it should also be on the next line
        if (node.getBeginLine() != prevSibling.getEndLine() + 1) {
            addViolationWithMessage(data, node,
                    node.getImage() + " should be on line " + (prevSibling.getEndLine() + 1));
        }

        return super.visit(node, data);
    }

    private void checkEachChildOnNextLine(Object data, Node parent, int firstLine, int indentation) {
        int currentLine = firstLine;
        for (int i = 0; i < parent.jjtGetNumChildren(); i++) {
            Node child = parent.jjtGetChild(i);
            String image = child.getImage();
            if (image == null && child.jjtGetNumChildren() > 0) {
                image = child.jjtGetChild(0).getImage();
            }
            if (child.getBeginLine() != currentLine) {
                addViolationWithMessage(data, child, image + " should be on line " + currentLine);
            } else if (i > 0 && child.getBeginColumn() != indentation) {
                addViolationWithMessage(data, child, image + " should begin at column " + indentation);
            }
            // next entry needs to be on the next line
            currentLine++;
        }
    }

    private void checkNameFormat(Object data, Node parent, String name, String format) {
        if (!(name.toUpperCase(Locale.ROOT).matches(format))) {
            addViolationWithMessage(data, parent, name + " invalid name format (" + format + ")");
        }
    }

    private void checkParametersName(Object data, Node parent) {
        boolean haveOutParameters = false;
        for (int i = 0; i < parent.jjtGetNumChildren(); i++) {
            ASTFormalParameter formalParameter = (ASTFormalParameter) parent.jjtGetChild(i);
            ASTParameterType parameterType = (ASTParameterType) formalParameter.jjtGetChild(1);
            if ("OUT".equals(parameterType.getType())) {
                haveOutParameters = true;
            }
        }
        for (int i = 0; i < parent.jjtGetNumChildren(); i++) {
            ASTFormalParameter formalParameter = (ASTFormalParameter) parent.jjtGetChild(i);
            ASTParameterName parameterName = (ASTParameterName) formalParameter.jjtGetChild(0);
            ASTParameterType parameterType = (ASTParameterType) formalParameter.jjtGetChild(1);
            if (haveOutParameters) {
                if ("IN".equals(parameterType.getType())) {
                    checkNameFormat(data, formalParameter, parameterName.getImage(), inputParameterNameFormat);
                } else if ("OUT".equals(parameterType.getType())) {
                    checkNameFormat(data, formalParameter, parameterName.getImage(), outputParameterNameFormat);
                } else if ("IN OUT".equals(parameterType.getType())) {
                    checkNameFormat(data, formalParameter, parameterName.getImage(), inputOutputParameterNameFormat);
                }
            } else {
                checkNameFormat(data, formalParameter, parameterName.getImage(), inputWithNoOutputParameterNameFormat);
            }
        }
    }

    private void checkLineAndIndentation(Object data, Node node, int line, int indentation, String name) {
        if (node.getBeginLine() != line) {
            addViolationWithMessage(data, node, name + " should be on line " + line);
        } else if (node.getBeginColumn() != indentation) {
            checkIndentation(data, node, indentation, name);
        }
    }

    private void checkIndentation(Object data, Node node, int indentation, String name) {
        if (node.getBeginColumn() != indentation) {
            addViolationWithMessage(data, node, name + " should begin at column " + indentation);
        }
    }

    @Override
    public Object visit(ASTFormalParameters node, Object data) {
        // check the data type alignment
        List<ASTFormalParameter> parameters = node.findChildrenOfType(ASTFormalParameter.class);
        checkParametersName(data, node);
        if (parameters.size() > 1) {
            if (parameterOnNewLine) {
                checkEachChildOnNextLine(data, node, node.getBeginLine() + 1, node.jjtGetParent().getBeginColumn() + indentation);
                ASTDatatype first = parameters.get(0).findDescendantsOfType(ASTDatatype.class).get(0);
                for (int i = 1; first != null && i < parameters.size(); i++) {
                    ASTDatatype nextType = parameters.get(i).findDescendantsOfType(ASTDatatype.class).get(0);
                    if (nextType != null) {
                        checkIndentation(data, nextType, first.getBeginColumn(), "Type " + nextType.getImage());
                    }
                }
            } else {
                checkEachChildOnNextLine(data, node, node.getBeginLine(), node.jjtGetChild(0).getBeginColumn());
                ASTDatatype first = parameters.get(0).getFirstChildOfType(ASTDatatype.class);
                for (int i = 1; first != null && i < parameters.size(); i++) {
                    ASTDatatype nextType = parameters.get(i).getFirstChildOfType(ASTDatatype.class);
                    if (nextType != null) {
                        checkIndentation(data, nextType, first.getBeginColumn(), "Type " + nextType.getImage());
                    }
                }
            }
        }
        return super.visit(node, data);
    }

    private static <T> List<T> convertList(List<Node> nodes, Class<T> target) {
        List<T> converted = new ArrayList<>();
        for (Node n : nodes) {
            converted.add(target.cast(n));
        }
        return converted;
    }

    @Override
    public Object visit(ASTCursorUnit node, Object data) {
        ASTSelectStatement query = node.getFirstChildOfType(ASTSelectStatement.class);
        checkIndentation(data, query, node.getBeginColumn() + indentation, "SELECT");
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTOutOfLineConstraint node, Object data) {
        String name = node.getFirstChildOfType(ASTID.class).getImage();
        if (node.getType() == ConstraintType.UNIQUE) {
            checkNameFormat(data, node, name, ".*_UK");
        } else if (node.getType() == ConstraintType.PRIMARY) {
            checkNameFormat(data, node, name, ".*_PK");
        } else if (node.getType() == ConstraintType.FOREIGN) {
            checkNameFormat(data, node, name, ".*_FK");
        } else if (node.getType() == ConstraintType.CHECK) {
            checkNameFormat(data, node, name, ".*_CHK");
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTTable node, Object data) {
        checkFilename(node.jjtGetChild(0), data, ".tab");
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTTypeSpecification node, Object data) {
        checkFilename(node, data, "(.typ|.tps)");
        checkNameFormat(data, node, node.getImage(), typeSpecNameFormat);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTView node, Object data) {
        checkFilename(node, data, ".vw");
        checkNameFormat(data, node, node.getImage(), viewNameFormat);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTSequence node, Object data) {
        checkFilename(node, data, ".seq");
        checkNameFormat(data, node, node.getImage(), sequenceNameFormat);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTTriggerUnit node, Object data) {
        checkFilename(node, data, ".trg");
        checkNameFormat(data, node, node.getImage(), triggerNameFormat);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTDirectory node, Object data) {
        checkFilename(node, data, ".dir");
        checkNameFormat(data, node, node.getImage(), directoryNameFormat);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTSynonym node, Object data) {
        checkFilename(node, data, ".syn");
        if (!node.jjtGetChild(0).getImage().equals(node.jjtGetChild(1).getImage().substring(node.jjtGetChild(1).getImage().indexOf('.') + 1) + "_SYN")) {
            addViolationWithMessage(data, node, "Synonym " + node.jjtGetChild(0).getImage() + " must be renamed to "
                    + node.jjtGetChild(1).getImage().substring(node.jjtGetChild(1).getImage().indexOf('.') + 1).toUpperCase(Locale.ROOT) + "_SYN");
        }
        checkNameFormat(data, node, node.getImage(), synonymNameFormat);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTPackageSpecification node, Object data) {
        checkFilename(node, data, "(.pks|.spc|.pkg|.pkh|pck)");
        checkNameFormat(data, node, node.getImage(), packageNameFormat);
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTPackageBody node, Object data) {
        checkFilename(node, data, "(.pkb|.bdy|.pkg|.pls|.plh)");
        int line = 2;
        checkNameFormat(data, node, node.getImage(), packageNameFormat);

        List<ASTVariableOrConstantDeclarator> variables;
        try {
            variables = convertList(node
                    .findChildNodesWithXPath("DeclarativeSection/DeclarativeUnit/VariableOrConstantDeclaration/VariableOrConstantDeclarator"), ASTVariableOrConstantDeclarator.class);
        } catch (JaxenException e) {
            return false;
        }
        if (!variables.isEmpty()) {
            ASTDatatype datatype = variables.get(0).getFirstChildOfType(ASTDatatype.class);
            int datatypeIndentation = 0;
            if (datatype != null) {
                datatypeIndentation = datatype.getBeginColumn();
            }
            for (ASTVariableOrConstantDeclarator variable : variables) {
                checkNameFormat(data, node, variable.getImage(), globalVariableNameFormat);
                if (line < variable.getBeginLine()) {
                    line = variable.getBeginLine();
                    datatypeIndentation = variable.getFirstChildOfType(ASTDatatype.class).getBeginColumn();
                }
                checkLineAndIndentation(data, variable, line, indentation + 1, variable.getImage());
                datatype = variable.getFirstChildOfType(ASTDatatype.class);
                checkIndentation(data, datatype, datatypeIndentation, "Type " + datatype.getImage());
                line++;
            }
            line++;
        }

        List<ASTProgramUnit> programUnits;
        try {
            programUnits = convertList(node
                    .findChildNodesWithXPath("DeclarativeSection/DeclarativeUnit/ProgramUnit"), ASTProgramUnit.class);
        } catch (JaxenException e) {
            return false;
        }
        assert programUnits != null;
        for (ASTProgramUnit programUnit : programUnits) {
            if (line < programUnit.getBeginLine()) {
                line = programUnit.getBeginLine() - 1;
            }
            checkLineAndIndentation(data, programUnit, line + 1, indentation + 1, "Program Unit " + programUnit.getName());
            line++;
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTSubTypeDefinition node, Object data) {
        checkNameFormat(data, node, node.getImage(), typeDefinitionNameFormat);
        Node parent = node.getFirstParentOfAnyType(ASTPackageBody.class, ASTPackageSpecification.class, ASTProgramUnit.class);
        if (parent != null) {
            checkIndentation(data, node, parent.getBeginColumn() + indentation, "TYPE " + node.getImage());
        }
        List<ASTFieldDeclaration> fieldDeclarations = node.findDescendantsOfType(ASTFieldDeclaration.class);
        int line = node.getBeginLine();
        if (!fieldDeclarations.isEmpty()) {
            for (ASTFieldDeclaration fieldDeclaration : fieldDeclarations) {
                checkLineAndIndentation(data, fieldDeclaration, line, fieldDeclarations.get(0).getBeginColumn(), fieldDeclaration.getImage());
                line++;
            }
        }
        return super.visit(node, data);
    }

    private void checkFilename(Node node, Object data, String extension) {
        String filepath = ((RuleContext) data).getSourceCodeFilename();
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String filename;
        if (os.contains("win")) {
            filename = filepath.substring(filepath.lastIndexOf('\\') + 1);
        } else {
            filename = filepath.substring(filepath.lastIndexOf('/') + 1);
        }
        if (filenameCheck && !"n/a".equals(filepath)) {
            String objectName = node.getImage().toLowerCase(Locale.ROOT);
            if (!filename.matches(objectName + extension)) {
                addViolationWithMessage(data, node, "Filename \"" + filename + "\" doesn't match object \"" + objectName + "\"");
            }
        }
    }

    @Override
    public Object visit(ASTProgramUnit node, Object data) {
        int variableIndentation;
        if (parameterOnNewLine) {
            variableIndentation = node.getBeginColumn() + 2 * indentation;
        } else {
            variableIndentation = node.getBeginColumn() + indentation;
        }
        int line = node.getFirstChildOfType(ASTMethodDeclarator.class).getEndLine() + 1;
        ASTMethodDeclarator programUnit = node.getFirstDescendantOfType(ASTMethodDeclarator.class);
        String name = programUnit.getImage();
        if (node.getFirstParentOfType(ASTPackageBody.class) == null && node.getFirstParentOfType(ASTPackageSpecification.class) == null) {
            if ("PROCEDURE".equals(programUnit.getType())) {
                checkFilename(programUnit, data, "(.sql|.prc)");
                checkNameFormat(data, node, programUnit.getImage(), procedureNameFormat);
            } else if ("FUNCTION".equals(programUnit.getType())) {
                checkFilename(programUnit, data, "(.sql|.fnc)");
                checkNameFormat(data, node, programUnit.getImage(), functionNameFormat);
            }
        } else {
            if ("PROCEDURE".equals(programUnit.getType()) && name.toUpperCase(Locale.ROOT).matches(procedureNameFormat)) {
                addViolationWithMessage(data, node, "Procedure " + name + " inside package must not match (" + procedureNameFormat + ") name format");
            } else if ("FUNCTION".equals(programUnit.getType()) && name.toUpperCase(Locale.ROOT).matches(procedureNameFormat)) {
                addViolationWithMessage(data, node, "Function " + name + "  inside package must not match (" + functionNameFormat + ") name format");
            }
        }

        List<ASTVariableOrConstantDeclarator> variables = node.findDescendantsOfType(ASTVariableOrConstantDeclarator.class);
        int datatypeIndentation = 0;
        if (!variables.isEmpty()) {
            ASTDatatype datatype = variables.get(0).getFirstChildOfType(ASTDatatype.class);
            if (datatype != null) {
                datatypeIndentation = datatype.getBeginColumn();
            }
            for (ASTVariableOrConstantDeclarator variable : variables) {
                checkNameFormat(data, variable, variable.getImage(), localVariableNameFormat);
                if (line < variable.getBeginLine()) {
                    line = variable.getBeginLine();
                    datatypeIndentation = variable.getFirstChildOfType(ASTDatatype.class).getBeginColumn();
                }
                checkLineAndIndentation(data, variable, line, variableIndentation, variable.getImage());
                datatype = variable.getFirstChildOfType(ASTDatatype.class);
                checkIndentation(data, datatype, datatypeIndentation, "Type " + datatype.getImage());
                line++;
            }
        }

        List<ASTExceptionDeclaration> exceptions = node.findDescendantsOfType(ASTExceptionDeclaration.class);
        if (!exceptions.isEmpty()) {
            for (ASTExceptionDeclaration exception : exceptions) {
                checkNameFormat(data, exception, exception.getImage(), exceptionDeclarationNameFormat);
                if (line < exception.getBeginLine()) {
                    line = exception.getBeginLine();
                }
                checkLineAndIndentation(data, exception, line, variableIndentation, exception.getImage());
                line++;
            }
        }

        List<ASTCursorUnit> cursors = node.findDescendantsOfType(ASTCursorUnit.class);
        if (!cursors.isEmpty()) {
            for (ASTCursorUnit cursor : cursors) {
                checkNameFormat(data, cursor, cursor.getImage(), cursorNameFormat);
                checkIndentation(data, cursor, variableIndentation, "CURSOR");
            }
        }
        return super.visit(node, data);
    }

    @Override
    public Object visit(ASTArgumentList node, Object data) {
        List<ASTArgument> arguments = node.findChildrenOfType(ASTArgument.class);

        if (node.getEndColumn() > 120) {
            addViolationWithMessage(data, node, "Line is too long, please split parameters on separate lines");
            return super.visit(node, data);
        }

        if (arguments.size() > maxParametersInLine) {
            // procedure calls with more than 3 parameters should use named parameters
            if (usesSimpleParameters(arguments)) {
                addViolationWithMessage(data, node,
                        "Procedure call with more than three parameters should use named parameters.");
            }

            // more than three parameters -> each parameter on a separate line
            int line;
            if (parameterOnNewLine) {
                line = node.getBeginLine();
            } else {
                line = node.getNthParent(1).getBeginLine();
            }
            int indentation = node.getBeginColumn();
            int longestParameterEndColumn = 0;
            for (ASTArgument argument : arguments) {
                if (argument.getImage() != null || parameterOnNewLine) {
                    checkLineAndIndentation(data, argument, line, indentation, "Parameter " + argument.getImage());
                } else {
                    checkLineAndIndentation(data, argument, line, indentation, "Parameter " + argument.jjtGetChild(0).getImage());
                }
                line++;

                if (argument.jjtGetChild(0) instanceof ASTUnqualifiedID) {
                    if (argument.jjtGetChild(0).getEndColumn() > longestParameterEndColumn) {
                        longestParameterEndColumn = argument.jjtGetChild(0).getEndColumn();
                    }
                }
            }

            // now check for the indentation of the expressions
            int expectedBeginColumn;
            if (parameterOnNewLine) {
                expectedBeginColumn = longestParameterEndColumn + 3 + "=> ".length();
            } else {
                expectedBeginColumn = longestParameterEndColumn + 3 + "=>".length();
            }
            // take the indentation from the first one, if it is greater
            if (!arguments.isEmpty() && arguments.get(0).jjtGetNumChildren() == 2
                    && arguments.get(0).jjtGetChild(1).getBeginColumn() > expectedBeginColumn) {
                expectedBeginColumn = arguments.get(0).jjtGetChild(1).getBeginColumn();
            }
            for (ASTArgument argument : arguments) {
                if (argument.jjtGetNumChildren() == 2 && argument.jjtGetChild(0) instanceof ASTUnqualifiedID) {
                    Node expr = argument.jjtGetChild(1);
                    checkIndentation(data, expr, expectedBeginColumn, expr.getImage());
                }
            }

            // closing paranthesis should be on a new line
            Node primaryExpression = node.getNthParent(3);
            if (primaryExpression.getEndLine() != node.getEndLine() + 1 && parameterOnNewLine) {
                addViolationWithMessage(data, primaryExpression, "Closing paranthesis should be on a new line.");
            }
            if (primaryExpression.getEndLine() != node.getEndLine() && !parameterOnNewLine) {
                addViolationWithMessage(data, primaryExpression, "Closing paranthesis should be on same line.");
            }
        }

        return super.visit(node, data);
    }

    private boolean usesSimpleParameters(List<ASTArgument> arguments) {
        for (ASTArgument argument : arguments) {
            if (argument.jjtGetNumChildren() == 1) {
                return true;
            }
        }
        return false;
    }
}
