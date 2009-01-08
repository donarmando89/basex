package org.basex.query.xquery.iter;

import org.basex.query.xquery.XQException;
import org.basex.query.xquery.item.FTNodeItem;

/**
 * Node iterator interface.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Sebastian Gath
 */
public abstract class FTNodeIter extends Iter {
  @Override
  public abstract FTNodeItem next() throws XQException;
}
