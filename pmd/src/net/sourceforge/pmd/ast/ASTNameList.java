/* Generated By:JJTree: Do not edit this line. ASTNameList.java */

package net.sourceforge.pmd.ast;

public class ASTNameList extends SimpleNode {
    public ASTNameList(int id) {
        super(id);
    }

    public ASTNameList(JavaParser p, int id) {
        super(p, id);
    }


    /** Accept the visitor. **/
    public Object jjtAccept(JavaParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
