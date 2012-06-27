package org.basex.core;

import static org.basex.core.Text.*;

import java.io.*;
import java.util.*;

import org.basex.core.cmd.*;
import org.basex.core.parse.*;
import org.basex.data.*;
import org.basex.io.out.*;
import org.basex.util.*;
import org.basex.util.list.*;
import org.xml.sax.*;

/**
 * This class provides the architecture for all internal command
 * implementations. It evaluates queries that are sent by the GUI, the client or
 * the standalone version.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public abstract class Command extends Progress {
  /** Command arguments. */
  public final String[] args;

  /** Performance measurements. */
  protected Performance perf;
  /** Database context. */
  protected Context context;
  /** Output stream. */
  protected PrintOutput out;
  /** Optional input source. */
  protected InputSource in;
  /** Database properties. */
  protected Prop prop;
  /** Main properties. */
  protected MainProp mprop;

  /** Container for query information. */
  private final TokenBuilder info = new TokenBuilder();
  /** Permission required to execute this command. */
  private final Perm perm;
  /** Indicates if the command requires an opened database. */
  private final boolean data;

  /**
   * Constructor for commands requiring no opened database.
   * @param p required permission
   * @param arg arguments
   */
  protected Command(final Perm p, final String... arg) {
    this(p, false, arg);
  }

  /**
   * Constructor.
   * @param p required permission
   * @param d requires opened database
   * @param arg arguments
   */
  protected Command(final Perm p, final boolean d, final String... arg) {
    perm = p;
    data = d;
    args = arg;
  }

  /**
   * Executes the command and prints the result to the specified output
   * stream. If an exception occurs, a {@link BaseXException} is thrown.
   * @param ctx database context
   * @param os output stream reference
   * @throws BaseXException command exception
   */
  public final void execute(final Context ctx, final OutputStream os)
      throws BaseXException {
    if(!exec(ctx, os)) throw new BaseXException(info());
  }

  /**
   * Executes the command and returns the result as string.
   * If an exception occurs, a {@link BaseXException} is thrown.
   * @param ctx database context
   * @return string result
   * @throws BaseXException command exception
   */
  public final String execute(final Context ctx) throws BaseXException {
    final ArrayOutput ao = new ArrayOutput();
    execute(ctx, ao);
    return ao.toString();
  }

  /**
   * Attaches an input stream.
   * @param is input stream
   */
  public void setInput(final InputStream is) {
    in = new InputSource(is);
  }

  /**
   * Attaches an input source.
   * @param is input source
   */
  public void setInput(final InputSource is) {
    in = is;
  }

  /**
   * Runs the command without permission, data and concurrency checks.
   * Should be called with care, and only by other database commands.
   * @param ctx database context
   * @return result of check
   */
  public final boolean run(final Context ctx) {
    return run(ctx, new NullOutput());
  }

  /**
   * Returns command information.
   * @return info string
   */
  public final String info() {
    return info.toString();
  }

  /**
   * Returns the result set, generated by a query command. Will only yield results if
   * {@link Prop#CACHEQUERY} is set, and can only be called once.
   * @return result set
   */
  public Result result() {
    return null;
  }

  /**
   * Checks if the command performs updates/write operations.
   * @param ctx database context
   * @return result of check
   */
  @SuppressWarnings("unused")
  public boolean updating(final Context ctx) {
    return createWrite();
  }

  /**
   * Returns the names of the databases that will be touched by the command.
   * {@code null} is returned if the touched databases cannot be statically determined.
   * @param ctx database context
   * @return databases
   * @see #databases(StringList)
   */
  String[] databases(final Context ctx) {
    // get touched databases
    final StringList sl = new StringList();
    if(!databases(sl)) return null;

    // replace empty string with currently opened database and return array
    final Data dt = ctx.data();
    final String[] tmp = new String[sl.size()];
    for(int d = 0; d < tmp.length; d++) {
      tmp[d] = dt != null && sl.get(d).isEmpty() ? dt.meta.name : sl.get(d);
    }
    return tmp;
  }

  /**
   * Checks if the command has updated any data.
   * If this method is called before command execution, it always returns {@code true}.
   * @return result of check
   */
  public boolean updated() {
    return true;
  }

  /**
   * Closes an open data reference and returns {@code true} if this command will change
   * the {@link Context#data} reference. This method is required by the progress dialog
   * in the frontend.
   * @param ctx database context
   * @return result of check
   */
  @SuppressWarnings("unused")
  public boolean newData(final Context ctx) {
    return false;
  }

  /**
   * Returns true if this command returns a progress value.
   * This method is required by the progress dialog in the frontend.
   * @return result of check
   */
  public boolean supportsProg() {
    return false;
  }

  /**
   * Returns true if this command can be stopped.
   * This method is required by the progress dialog in the frontend.
   * @return result of check
   */
  public boolean stoppable() {
    return false;
  }

  @Override
  public final String toString() {
    final CmdBuilder cb = new CmdBuilder(this);
    build(cb);
    return cb.toString();
  }

  // PROTECTED METHODS ========================================================

  /**
   * Executes the command and serializes the result (internal call).
   * @return success of operation
   * @throws IOException I/O exception
   */
  protected abstract boolean run() throws IOException;

  /**
   * Builds a string representation from the command. This string must be
   * correctly built, as commands are sent to the server as strings.
   * @param cb command builder
   */
  protected void build(final CmdBuilder cb) {
    cb.init().args();
  }

  /**
   * Adds the error message to the message buffer {@link #info}.
   * @param msg error message
   * @param ext error extension
   * @return {@code false}
   */
  protected final boolean error(final String msg, final Object... ext) {
    info.reset();
    info.addExt(msg == null ? "" : msg, ext);
    return false;
  }

  /**
   * Adds information on command execution.
   * @param str information to be added
   * @param ext extended info
   * @return {@code true}
   */
  protected final boolean info(final String str, final Object... ext) {
    info.addExt(str, ext).add(NL);
    return true;
  }

  /**
   * Returns the specified command option.
   * @param typ options enumeration
   * @param <E> token type
   * @return option
   */
  protected final <E extends Enum<E>> E getOption(final Class<E> typ) {
    final E e = getOption(args[0], typ);
    if(e == null) error(UNKNOWN_TRY_X, args[0]);
    return e;
  }

  /**
   * Returns the specified command option.
   * @param s string to be found
   * @param typ options enumeration
   * @param <E> token type
   * @return option
   */
  protected static <E extends Enum<E>> E getOption(final String s, final Class<E> typ) {
    try {
      return Enum.valueOf(typ, s.toUpperCase(Locale.ENGLISH));
    } catch(final Exception ex) {
      return null;
    }
  }

  /**
   * Closes the specified database if it is currently opened and only pinned once.
   * @param ctx database context
   * @param db database to be closed
   * @return closed flag
   */
  protected static boolean close(final Context ctx, final String db) {
    final boolean close = ctx.data() != null &&
        db.equals(ctx.data().meta.name) && ctx.datas.pins(db) == 1;
    return close && new Close().run(ctx);
  }

  // PRIVATE METHODS ==========================================================

  /**
   * Checks if the command demands write or create permissions.
   * @return result of check
   */
  private boolean createWrite() {
    return perm == Perm.CREATE || perm == Perm.WRITE;
  }

  /**
   * Executes the command, prints the result to the specified output stream
   * and returns a success flag.
   * @param ctx database context
   * @param os output stream
   * @return success flag. The {@link #info()} method returns information
   * on a potential exception
   */
  private boolean exec(final Context ctx, final OutputStream os) {
    // check if data reference is available
    final Data dt = ctx.data();
    if(dt == null && data) return error(NO_DB_OPENED);

    // check permissions
    if(!ctx.perm(perm, dt != null ? dt.meta : null)) return error(PERM_REQUIRED_X, perm);

    // set updating flag
    updating = updating(ctx);

    /* [JE] get touched databases
    final String[] db = databases(ctx);
    if(db != null) {
      System.out.println("Touched Databases:");
      for(final String d : databases(ctx)) {
        System.out.println("- " + d);
      }
    }*/

    try {
      // register process
      ctx.register(this);
      // run command and return success flag
      return run(ctx, os);
    } finally {
      // guarantee that process will be unregistered
      ctx.unregister(this);
    }
  }

  /**
   * Runs the command without permission, data and concurrency checks.
   * @param ctx database context
   * @param os output stream
   * @return result of check
   */
  private boolean run(final Context ctx, final OutputStream os) {
    perf = new Performance();
    context = ctx;
    prop = ctx.prop;
    mprop = ctx.mprop;
    out = PrintOutput.get(os);

    try {
      return run();
    } catch(final ProgressException ex) {
      // process was interrupted by the user or server
      abort();
      return error(INTERRUPTED);
    } catch(final Throwable ex) {
      // unexpected error
      Performance.gc(2);
      abort();
      if(ex instanceof OutOfMemoryError) {
        Util.debug(ex);
        return error(OUT_OF_MEM + (createWrite() ? H_OUT_OF_MEM : ""));
      }
      return error(Util.bug(ex) + NL + info.toString());
    } finally {
      // flushes the output
      try { if(out != null) out.flush(); } catch(final IOException ex) { }
    }
  }
}
