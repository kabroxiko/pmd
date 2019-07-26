/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.plsql.ast;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.sourceforge.pmd.lang.plsql.AbstractPLSQLParserTst;


public class ASTCompoundConditionTest extends AbstractPLSQLParserTst {

    @Test
    public void testParseType() {
        ASTInput input = parsePLSQL("BEGIN SELECT COUNT(1) INTO MY_TABLE FROM USERS_TABLE WHERE user_id = 1 AnD user_id = 2; END;");
        List<ASTNextCondition> nextConditions = input.findDescendantsOfType(ASTNextCondition.class);
        Assert.assertFalse(nextConditions.isEmpty());
        Assert.assertEquals("AND", nextConditions.get(0).getType());
    }
}
