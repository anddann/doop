package deepdoop.datalog.expr;

import deepdoop.actions.IVisitor;
import deepdoop.datalog.BinOperator;

public class BinaryExpr implements IExpr {

	public final IExpr       left;
	public final BinOperator op;
	public final IExpr       right;

	public BinaryExpr(IExpr left, BinOperator op, IExpr right) {
		this.left  = left;
		this.op    = op;
		this.right = right;
	}

	@Override
	public String toString() {
		return left + " " + op + " " + right;
	}


	@Override
	public <T> T accept(IVisitor<T> v) {
		return v.visit(this);
	}
}