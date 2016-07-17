package deepdoop.datalog.expr;

import deepdoop.actions.IVisitor;

public class VariableExpr implements IExpr {

	public final String  name;
	public final boolean isDontCare;

	public VariableExpr(String name) {
		this.name       = name;
		this.isDontCare = "_".equals(name);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof VariableExpr) && ((VariableExpr)o).name.equals(name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}


	@Override
	public <T> T accept(IVisitor<T> v) {
		return v.visit(this);
	}
}
