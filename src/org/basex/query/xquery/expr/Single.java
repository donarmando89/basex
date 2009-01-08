package org.basex.query.xquery.expr;

import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.xquery.XQContext;
import org.basex.query.xquery.XQException;
import org.basex.query.xquery.item.Type;
import org.basex.query.xquery.util.Var;

/**
 * Abstract single expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public abstract class Single extends Expr {
  /** Expression list. */
  public Expr expr;

  /**
   * Constructor.
   * @param e expression
   */
  protected Single(final Expr e) {
    expr = e;
  }

  @Override
  public Expr comp(final XQContext ctx) throws XQException {
    expr = expr.comp(ctx);
    return this;
  }

  @Override
  public boolean usesPos() {
    return expr.usesPos();
  }

  @Override
  public boolean usesVar(final Var v) {
    return expr.usesVar(v);
  }
  
  @Override
  public Type returned() {
    return null;
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this);
    expr.plan(ser);
    ser.closeElement();
  }
}
