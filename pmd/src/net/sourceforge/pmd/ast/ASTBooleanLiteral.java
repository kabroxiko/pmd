/* Generated By:JJTree: Do not edit this line. ASTBooleanLiteral.java */

package net.sourceforge.pmd.ast;

public class ASTBooleanLiteral extends SimpleNode {
    public ASTBooleanLiteral(int id) {
        super(id);
    }

    public ASTBooleanLiteral(JavaParser p, int id) {
        super(p, id);
    }


    /** Accept the visitor. **/
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
