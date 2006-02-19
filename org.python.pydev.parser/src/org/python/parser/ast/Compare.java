// Autogenerated AST node
package org.python.parser.ast;
import org.python.parser.SimpleNode;
import java.io.DataOutputStream;
import java.io.IOException;

public class Compare extends exprType implements cmpopType {
    public exprType left;
    public int[] ops;
    public exprType[] comparators;

    public Compare(exprType left, int[] ops, exprType[] comparators) {
        this.left = left;
        this.ops = ops;
        this.comparators = comparators;
    }

    public Compare(exprType left, int[] ops, exprType[] comparators,
    SimpleNode parent) {
        this(left, ops, comparators);
        this.beginLine = parent.beginLine;
        this.beginColumn = parent.beginColumn;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Compare[");
        sb.append("left=");
        sb.append(dumpThis(this.left));
        sb.append(", ");
        sb.append("ops=");
        sb.append(dumpThis(this.ops, cmpopType.cmpopTypeNames));
        sb.append(", ");
        sb.append("comparators=");
        sb.append(dumpThis(this.comparators));
        sb.append("]");
        return sb.toString();
    }

    public void pickle(DataOutputStream ostream) throws IOException {
        pickleThis(37, ostream);
        pickleThis(this.left, ostream);
        pickleThis(this.ops, ostream);
        pickleThis(this.comparators, ostream);
    }

    public Object accept(VisitorIF visitor) throws Exception {
        return visitor.visitCompare(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (left != null)
            left.accept(visitor);
        if (comparators != null) {
            for (int i = 0; i < comparators.length; i++) {
                if (comparators[i] != null)
                    comparators[i].accept(visitor);
            }
        }
    }

}
