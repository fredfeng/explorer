package test.util;

import soot.jimple.spark.pag.*;
import soot.jimple.*;
import soot.*;
import soot.toolkits.scalar.Pair;
import soot.shimple.*;

public class myMethodNodeFactory {
	public myMethodNodeFactory(myPAG mypag, myMethodPAG mympag) {
		this.mypag = mypag;
		this.mympag = mympag;
		setCurrentMethod(mympag.getMethod());
	}

	/** Sets the method for which a graph is currently being built. */
	private void setCurrentMethod(SootMethod m) {
		method = m;
		if (!m.isStatic()) {
			SootClass c = m.getDeclaringClass();
			if (c == null) {
				throw new RuntimeException("Method " + m
						+ " has no declaring class");
			}
			caseThis();
		}
		for (int i = 0; i < m.getParameterCount(); i++) {
			if (m.getParameterType(i) instanceof RefLikeType) {
				caseParm(i);
			}
		}
		Type retType = m.getReturnType();
		if (retType instanceof RefLikeType) {
			caseRet();
		}
	}

	public Node getNode(Value v) {
		v.apply(this);
		return getNode();
	}

	/** Adds the edges required for this statement to the graph. */
	final public void handleStmt(Stmt s) {
		if (s.containsInvokeExpr()) {
			return;
		}
		s.apply(new AbstractStmtSwitch() {
			final public void caseAssignStmt(AssignStmt as) {
				Value l = as.getLeftOp();
				Value r = as.getRightOp();
				if (!(l.getType() instanceof RefLikeType))
					return;
				l.apply(myMethodNodeFactory.this);
				Node dest = getNode();
				r.apply(myMethodNodeFactory.this);
				Node src = getNode();
				if (l instanceof InstanceFieldRef) {
					((InstanceFieldRef) l).getBase().apply(
							myMethodNodeFactory.this);
					mypag.addDereference((VarNode) getNode());
				}
				if (r instanceof InstanceFieldRef) {
					((InstanceFieldRef) r).getBase().apply(
							myMethodNodeFactory.this);
					mypag.addDereference((VarNode) getNode());
				}
				if (r instanceof StaticFieldRef) {
					StaticFieldRef sfr = (StaticFieldRef) r;
					SootFieldRef s = sfr.getFieldRef();
					if (mypag.getOpts().empties_as_allocs()) {
						if (s.declaringClass().getName()
								.equals("java.util.Collections")) {
							if (s.name().equals("EMPTY_SET")) {
								src = mypag.makeAllocNode(
										RefType.v("java.util.HashSet"),
										RefType.v("java.util.HashSet"), method);
							} else if (s.name().equals("EMPTY_MAP")) {
								src = mypag.makeAllocNode(
										RefType.v("java.util.HashMap"),
										RefType.v("java.util.HashMap"), method);
							} else if (s.name().equals("EMPTY_LIST")) {
								src = mypag.makeAllocNode(
										RefType.v("java.util.LinkedList"),
										RefType.v("java.util.LinkedList"),
										method);
							}
						} else if (s.declaringClass().getName()
								.equals("java.util.Hashtable")) {
							if (s.name().equals("emptyIterator")) {
								src = mypag.makeAllocNode(
										RefType.v("java.util.Hashtable$EmptyIterator"),
										RefType.v("java.util.Hashtable$EmptyIterator"),
										method);
							} else if (s.name().equals("emptyEnumerator")) {
								src = mypag.makeAllocNode(
										RefType.v("java.util.Hashtable$EmptyEnumerator"),
										RefType.v("java.util.Hashtable$EmptyEnumerator"),
										method);
							}
						}
					}
				}
				mympag.addInternalEdge(src, dest);
			}

			final public void caseReturnStmt(ReturnStmt rs) {
				if (!(rs.getOp().getType() instanceof RefLikeType))
					return;
				rs.getOp().apply(myMethodNodeFactory.this);
				Node retNode = getNode();
				mympag.addInternalEdge(retNode, caseRet());
			}

			final public void caseIdentityStmt(IdentityStmt is) {
				if (!(is.getLeftOp().getType() instanceof RefLikeType))
					return;
				is.getLeftOp().apply(MethodNodeFactory.this);
				Node dest = getNode();
				is.getRightOp().apply(MethodNodeFactory.this);
				Node src = getNode();
				mpag.addInternalEdge(src, dest);
			}

			final public void caseThrowStmt(ThrowStmt ts) {
				ts.getOp().apply(MethodNodeFactory.this);
				mpag.addOutEdge(getNode(), pag.nodeFactory().caseThrow());
			}
		});
	}

	final public Node getNode() {
		return (Node) getResult();
	}

	final public Node caseThis() {
		VarNode ret = pag.makeLocalVarNode(new Pair(method,
				PointsToAnalysis.THIS_NODE), method.getDeclaringClass()
				.getType(), method);
		ret.setInterProcTarget();
		return ret;
	}

	final public Node caseParm(int index) {
		VarNode ret = pag.makeLocalVarNode(
				new Pair(method, new Integer(index)),
				method.getParameterType(index), method);
		ret.setInterProcTarget();
		return ret;
	}

	final public void casePhiExpr(PhiExpr e) {
		Pair phiPair = new Pair(e, PointsToAnalysis.PHI_NODE);
		Node phiNode = pag.makeLocalVarNode(phiPair, e.getType(), method);
		for (Value op : e.getValues()) {
			op.apply(MethodNodeFactory.this);
			Node opNode = getNode();
			mpag.addInternalEdge(opNode, phiNode);
		}
		setResult(phiNode);
	}

	final public Node caseRet() {
		VarNode ret = pag.makeLocalVarNode(
				Parm.v(method, PointsToAnalysis.RETURN_NODE),
				method.getReturnType(), method);
		ret.setInterProcSource();
		return ret;
	}

	final public Node caseArray(VarNode base) {
		return pag.makeFieldRefNode(base, ArrayElement.v());
	}

	/* End of public methods. */
	/* End of package methods. */

	// OK, these ones are public, but they really shouldn't be; it's just
	// that Java requires them to be, because they override those other
	// public methods.
	final public void caseArrayRef(ArrayRef ar) {
		caseLocal((Local) ar.getBase());
		setResult(caseArray((VarNode) getNode()));
	}

	final public void caseCastExpr(CastExpr ce) {
		Pair castPair = new Pair(ce, PointsToAnalysis.CAST_NODE);
		ce.getOp().apply(this);
		Node opNode = getNode();
		Node castNode = pag
				.makeLocalVarNode(castPair, ce.getCastType(), method);
		mpag.addInternalEdge(opNode, castNode);
		setResult(castNode);
	}

	final public void caseCaughtExceptionRef(CaughtExceptionRef cer) {
		setResult(pag.nodeFactory().caseThrow());
	}

	final public void caseInstanceFieldRef(InstanceFieldRef ifr) {
		if (pag.getOpts().field_based() || pag.getOpts().vta()) {
			setResult(pag.makeGlobalVarNode(ifr.getField(), ifr.getField()
					.getType()));
		} else {
			setResult(pag.makeLocalFieldRefNode(ifr.getBase(), ifr.getBase()
					.getType(), ifr.getField(), method));
		}
	}

	final public void caseLocal(Local l) {
		setResult(pag.makeLocalVarNode(l, l.getType(), method));
	}

	final public void caseNewArrayExpr(NewArrayExpr nae) {
		setResult(pag.makeAllocNode(nae, nae.getType(), method));
	}

	private boolean isStringBuffer(Type t) {
		if (!(t instanceof RefType))
			return false;
		RefType rt = (RefType) t;
		String s = rt.toString();
		if (s.equals("java.lang.StringBuffer"))
			return true;
		if (s.equals("java.lang.StringBuilder"))
			return true;
		return false;
	}

	final public void caseNewExpr(NewExpr ne) {
		if (pag.getOpts().merge_stringbuffer() && isStringBuffer(ne.getType())) {
			setResult(pag.makeAllocNode(ne.getType(), ne.getType(), null));
		} else {
			setResult(pag.makeAllocNode(ne, ne.getType(), method));
		}
	}

	final public void caseNewMultiArrayExpr(NewMultiArrayExpr nmae) {
		ArrayType type = (ArrayType) nmae.getType();
		AllocNode prevAn = pag.makeAllocNode(new Pair(nmae, new Integer(
				type.numDimensions)), type, method);
		VarNode prevVn = pag.makeLocalVarNode(prevAn, prevAn.getType(), method);
		mpag.addInternalEdge(prevAn, prevVn);
		setResult(prevAn);
		while (true) {
			Type t = type.getElementType();
			if (!(t instanceof ArrayType))
				break;
			type = (ArrayType) t;
			AllocNode an = pag.makeAllocNode(new Pair(nmae, new Integer(
					type.numDimensions)), type, method);
			VarNode vn = pag.makeLocalVarNode(an, an.getType(), method);
			mpag.addInternalEdge(an, vn);
			mpag.addInternalEdge(vn,
					pag.makeFieldRefNode(prevVn, ArrayElement.v()));
			prevAn = an;
			prevVn = vn;
		}
	}

	final public void caseParameterRef(ParameterRef pr) {
		setResult(caseParm(pr.getIndex()));
	}

	final public void caseStaticFieldRef(StaticFieldRef sfr) {
		setResult(pag.makeGlobalVarNode(sfr.getField(), sfr.getField()
				.getType()));
	}

	final public void caseStringConstant(StringConstant sc) {
		AllocNode stringConstant;
		if (pag.getOpts().string_constants()
				|| Scene.v().containsClass(sc.value)
				|| (sc.value.length() > 0 && sc.value.charAt(0) == '[')) {
			stringConstant = pag.makeStringConstantNode(sc.value);
		} else {
			stringConstant = pag.makeAllocNode(PointsToAnalysis.STRING_NODE,
					RefType.v("java.lang.String"), null);
		}
		VarNode stringConstantLocal = pag.makeGlobalVarNode(stringConstant,
				RefType.v("java.lang.String"));
		pag.addEdge(stringConstant, stringConstantLocal);
		setResult(stringConstantLocal);
	}

	final public void caseThisRef(ThisRef tr) {
		setResult(caseThis());
	}

	final public void caseNullConstant(NullConstant nr) {
		setResult(null);
	}

	final public void caseClassConstant(ClassConstant cc) {
		AllocNode classConstant = mypag.makeClassConstantNode(cc);
		VarNode classConstantLocal = mypag.makeGlobalVarNode(classConstant,
				RefType.v("java.lang.Class"));
		mypag.addEdge(classConstant, classConstantLocal);
		setResult(classConstantLocal);
	}

	final public void defaultCase(Object v) {
		throw new RuntimeException("failed to handle " + v);
	}

	protected myPAG mypag;
	protected myMethodPAG mympag;
	protected SootMethod method;
}
