/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.plsql.ast;

import java.util.Locale;

public class ASTParameterType extends net.sourceforge.pmd.lang.plsql.ast.AbstractPLSQLNode {
    private String type;

    public ASTParameterType(int id) {
        super(id);
    }

    public ASTParameterType(PLSQLParser p, int id) {
        super(p, id);
    }

    @Override
    public Object jjtAccept(PLSQLParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
        if (this.type != null) {
            this.type = this.type.toUpperCase(Locale.ROOT);
        }
    }
}
/* JavaCC - OriginalChecksum=830aa91d2222f7a339f8f8bc4b355110 (do not edit this line) */
