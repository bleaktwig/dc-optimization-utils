import java.util.HashMap;

import org.jlab.jnp.hipo4.io.HipoReader;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.Schema;

int         nmax = 1e9;
float resolution = 1e-4;

// Process args.
if (args.length < 3) {
    System.out.printf("Usage:\n\trun-groovy compareBanks.groovy file1 file2 bankname ");
    System.out.printf("[floatCompResolution] [nEventMax]\n");
    return 1;
}
if (args.length >= 4) resolution = Float.parseFloat(args[3]);
if (args.length >= 5) nmax = Integer.parseInteger(args[4]);

// First file.
HipoReader reader0 = new HipoReader();
reader0.open(args[0]);

// Second file.
HipoReader reader1 = new HipoReader();
reader1.open(args[1]);

HipoReader[] readers = [reader0, reader1];
Schema runConfig = reader0.getSchemaFactory().getSchema("RUN::config");
Schema schema    = reader0.getSchemaFactory().getSchema(args[2]);

int    nevent = -1;
int      nrow =  0;
int    nentry =  0;
int nbadevent =  0;
int   nbadrow =  0;
int nbadentry =  0;
Map<String, Integer> badEntries = new HashMap<>();

while (reader0.hasNext() && reader1.hasNext() && nevent < nmax) {
    nevent++;
    if (nevent % 10000 == 0) System.out.printf("Analyzed %10d events\n", nevent);

    // Read hipo4 banks
    Event  event = new Event();
    Bank     run = new Bank(runConfig);
    Bank[] banks = [new Bank(schema), new Bank(schema)];
    for (int i = 0; i < 2; ++i) {
        readers[i].nextEvent(event);
        event.read(banks[i]);
    }
    event.read(run);
    if (banks[0].getRows() != banks[1].getRows()) {
        // System.out.printf("Different number of rows.\n");
        nbadevent++;
        continue;
    }
    for (int i = 0; i < banks[0].getRows(); ++i) {
        boolean mismatch = false;
	    nrow++;
        for (int j = 0; j < schema.getElements(); ++j) {
            int      type = schema.getType(j);
            String   name = schema.getElementName(j);
            int   bidx = -1;
            nentry++;
            switch (type) {
            case 1:
                if (banks[0].getByte(name,i) != banks[1].getByte(name,i)) bidx = j;
                break;
            case 2:
                if (banks[0].getShort(name,i) != banks[1].getShort(name,i)) bidx = j;
                break;
            case 3:
                if (banks[0].getInt(name,i) != banks[1].getInt(name,i)) bidx = j;
                break;
            case 4:
                if (Math.abs(banks[0].getFloat(name,i) - banks[1].getFloat(name,i)) > resolution)
                    bidx = j;
                break;
            }
            if (bidx >= 0) {
                // System.out.printf("Value mismatch at row %d, %s\n", i, schema.getElementName(bidx));
                mismatch = true;
                nbadentry++;
                if (badEntries.containsKey(schema.getElementName(bidx))) {
                    int nbad = badEntries.get(schema.getElementName(bidx)) + 1;
                    badEntries.replace(schema.getElementName(bidx), nbad);
                }
                else {
                    badEntries.put(schema.getElementName(bidx), 1);
                }
            }
        }
        if (mismatch) {
            nbadrow++;
            // run.show(); banks[0].show(); banks[1].show();
        }
    }
}

// Print results.
System.out.printf("\nAnalyzed %d events. Results:\n", nevent);
System.out.printf("  * %10d/%10d malformed events.\n", nbadevent, nevent);
System.out.printf("  * %10d/%10d mismatched rows.\n", nbadrow, nrow);
System.out.printf("  * %10d/%10d mismatched entries.\n", nbadentry, nentry);
if (nbadentry > 0) {
    System.out.printf("Details on mismatches entries:\n");
    for (String name : badEntries.keySet())
        System.out.printf("    * %12s: %d\n", name, badEntries.get(name));
}
