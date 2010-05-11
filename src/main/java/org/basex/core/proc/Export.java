package org.basex.core.proc;

import static org.basex.core.Text.*;
import java.io.IOException;
import org.basex.core.Context;
import org.basex.core.Main;
import org.basex.core.Proc;
import org.basex.core.Prop;
import org.basex.core.User;
import org.basex.data.Data;
import org.basex.data.SerializerProp;
import org.basex.data.XMLSerializer;
import org.basex.io.IO;
import org.basex.io.PrintOutput;
import org.basex.util.Token;

/**
 * Evaluates the 'export' command and saves the currently opened database
 * to disk.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class Export extends Proc {
  /**
   * Default constructor.
   * @param target export target
   */
  public Export(final String target) {
    this(target, null);
  }

  /**
   * Default constructor, specifying an optional output filename.
   * @param target export path
   * @param name optional name of output file
   */
  public Export(final String target, final String name) {
    super(DATAREF | User.READ | User.CREATE, target, name);
  }

  @Override
  protected boolean run() {
    try {
      final Data data = context.data;
      export(context.prop, data, args[0], args[1]);
      return info(DBEXPORTED, data.meta.name, perf);
    } catch(final IOException ex) {
      Main.debug(ex);
      return error(ex.getMessage());
    }
  }

  /**
   * Exports the specified database.
   * @param context context
   * @param data data reference
   * @throws IOException I/O exception
   */
  public static void export(final Context context, final Data data)
      throws IOException {
    export(context.prop, data, data.meta.file.path(), null);
  }

  /**
   * Exports the current database to the specified path and file.
   * @param prop property
   * @param data data reference
   * @param target file path
   * @param name optional file name
   * @throws IOException I/O exception
   */
  private static void export(final Prop prop, final Data data,
      final String target, final String name) throws IOException {

    final int[] docs = data.doc();
    final IO root = IO.get(target);
    
    for(final int pre : docs) {
      // create file path
      final IO file = docs.length == 1 && name != null ? root.merge(name) :
        IO.get(root + "/" + Token.string(data.text(pre, true)));

      // create dir if necessary
      final IO dir = IO.get(file.dir());
      if(!dir.exists()) dir.md();

      // serialize nodes
      final PrintOutput po = new PrintOutput(file.path());
      final SerializerProp sp = new SerializerProp(prop.get(Prop.EXPORTER));
      final XMLSerializer xml = new XMLSerializer(po, sp);
      xml.node(data, pre);
      xml.close();
      po.close();
    }
  }
}
