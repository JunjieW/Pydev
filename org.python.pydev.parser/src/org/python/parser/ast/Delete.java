// Autogenerated AST node
package org.python.parser.ast;
import org.python.parser.SimpleNode;
import java.io.DataOutputStream;
import java.io.IOException;

public class Delete extends stmtType {
    public exprType[] targets;

    public Delete(exprType[] targets) {
        this.targets = targets;
    }

    public Delete(exprType[] targets, SimpleNode parent) {
        this(targets);
        this.beginLine = parent.beginLine;
        this.beginColumn = parent.beginColumn;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Delete[");
        sb.append("targets=");
        sb.append(dumpThis(this.targets));
        sb.append("]");
        return sb.toString();
    }

    public void pickle(DataOutputStream ostream) throws IOException {
        pickleThis(11, ostream);
        pickleThis(this.targets, ostream);
    }

    public Object accept(VisitorIF visitor) throws Exception {
        return visitor.visitDelete(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (targets != null) {
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] != null)
                    targets[i].accept(visitor);
            }
        }
    }

}
