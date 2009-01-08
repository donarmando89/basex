package org.basex.query.xquery.func;

import static org.basex.query.xquery.XQText.*;
import org.basex.BaseX;
import org.basex.query.xquery.XQException;
import org.basex.query.xquery.XQContext;
import org.basex.query.xquery.expr.Expr;
import org.basex.query.xquery.item.Bln;
import org.basex.query.xquery.item.Item;
import org.basex.query.xquery.iter.Iter;
import org.basex.query.xquery.iter.SeqIter;
import org.basex.query.xquery.util.Err;

/**
 * Simple functions.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class FNSimple extends Fun {
  @Override
  public Iter iter(final XQContext ctx, final Iter[] arg) throws XQException {
    final Iter iter = arg.length != 0 ? arg[0] : null;
    
    switch(func) {
      case TRUE:    return Bln.TRUE.iter();
      case FALSE:   return Bln.FALSE.iter();
      case BOOL:    return Bln.get(iter.ebv().bool()).iter();
      case NOT:     return Bln.get(!iter.ebv().bool()).iter();
      case EMPTY:   return Bln.get(iter.next() == null).iter();
      case EXISTS:  return Bln.get(iter.next() != null).iter();
      case ZEROONE:
        Item it = iter.next();
        if(it == null) return Iter.EMPTY;
        if(iter.next() != null) Err.or(ZEROONE);
        return it.iter();
      case EXONE:
        it = iter.next();
        if(it == null || iter.next() != null) Err.or(EXONE);
        return it.iter();
      case ONEMORE:
        final SeqIter seq = SeqIter.get(iter);
        if(seq.size < 1) Err.or(ONEMORE);
        return seq;
      case UNORDER:
        return iter;
      default:
        BaseX.notexpected(func); return null;
    }
  }

  @Override
  public Expr c(final XQContext ctx) throws XQException {
    final Item it = args.length != 0 && args[0].i() ? (Item) args[0] : null;
    final boolean e = args.length != 0 && args[0].e();
    
    switch(func) {
      case TRUE:
        return Bln.TRUE;
      case FALSE:
        return Bln.FALSE;
      case EMPTY:
        args[0] = addPos(ctx, args[0]);
        return it != null ? Bln.FALSE : e ? Bln.TRUE : this;
      case EXISTS:
        args[0] = addPos(ctx, args[0]);
        return it != null ? Bln.TRUE : e ? Bln.FALSE : this;
      case BOOL:
        args[0] = addPos(ctx, args[0]);
        return it != null ? Bln.get(it.bool()) : e ? Bln.FALSE : this;
      case NOT:
        if(it != null) return Bln.get(!it.bool());
        if(args[0] instanceof Fun) {
          final Fun fs = (Fun) args[0];
          if(fs.func == FunDef.EMPTY) {
            final Fun f = new FNSimple();
            f.args = fs.args;
            f.func = FunDef.EXISTS;
            return f;
          }
        }
        args[0] = addPos(ctx, args[0]);
        return this;
      case ZEROONE: return e || it != null ? args[0] : this;
      case EXONE:   return it != null ? args[0] : this;
      case ONEMORE: return it != null ? args[0] : this;
      case UNORDER: return args[0];
      default:      return this;
    }
  }
}
