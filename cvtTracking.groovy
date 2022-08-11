import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.H1F;
import org.jlab.groot.ui.TCanvas;
import org.jlab.jnp.hipo4.io.HipoReader;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.Schema;
import java.util.HashMap;

int   nmax = 1e9;
float res = 1e-4;
// Get command line options
if (args.length < 3) {
    System.out.printf("Usage:\n\trun-groovy javaTracking.groovy file1 file2 bankname" );
    System.out.printf("[floatCompResolution] [nEventMax]\n");
    return;
}
if (args.length >= 4) {
    res = Float.parseFloat(args[3]);
    System.out.println("Resolution for float comparison set to " + res);
}
if (args.length >= 5) {
    nmax = Integer.parseInt(args[4]);
    System.out.println("Analyzing " + nmax + " events");
}

// Create histograms
GStyle.getH1FAttributes().setOptStat("1111111");
H1F hi_chi2_all = new H1F("hi_chi2_all", "#chi2", "Counts", 100, 0.0, 1000.0);
H1F hi_chi2_bad = new H1F("hi_chi2_bad", "#chi2", "Counts", 100, 0.0, 1000.0);
hi_chi2_bad.setLineColor(2);
H1F hi_ndf_all = new H1F("hi_ndf_all", "NDF", "Counts", 100, 0.0, 100.0);
H1F hi_ndf_bad = new H1F("hi_ndf_bad", "NDF", "Counts", 100, 0.0, 100.0);
hi_ndf_bad.setLineColor(2);
H1F hi_dpx = new H1F("hi_dpx", "#Deltapx(GeV)", "Counts", 101, -0.1, 0.1);
H1F hi_dpy = new H1F("hi_dpy", "#Deltapy(GeV)", "Counts", 101, -0.1, 0.1);
H1F hi_dpz = new H1F("hi_dpz", "#Deltapz(GeV)", "Counts", 101, -0.1, 0.1);
H1F hi_dvx = new H1F("hi_dvx", "#Deltavx(cm)", "Counts", 101, -0.5, 0.5);
H1F hi_dvy = new H1F("hi_dvy", "#Deltavy(cm)", "Counts", 101, -0.5, 0.5);
H1F hi_dvz = new H1F("hi_dvz", "#Deltavz(cm)", "Counts", 101, -0.5, 0.5);

// First bank.
HipoReader reader0 = new HipoReader();
reader0.open(args[0]);

// Second bank.
HipoReader reader1 = new HipoReader();
reader1.open(args[1]);

HipoReader[] readers = [reader0, reader1];

Schema runConfig = reader0.getSchemaFactory().getSchema("RUN::config");
Schema schema    = reader0.getSchemaFactory().getSchema(args[2]);

int nevent    = -1;
int nrow      =  0;
int nentry    =  0;
int nbadevent =  0;
int nbadrow   =  0;
int nbadentry =  0;
Map<String, Integer> badEntries = new HashMap<>();

while (reader0.hasNext() && reader1.hasNext() && nevent < nmax) {
    nevent++;
    if (nevent%10000 == 0) System.out.println("Analyzed " + nevent + " events");

    // read hipo4 banks
    Event event  = new Event();
    Bank    run  = new Bank(runConfig);
    Bank[] banks = [new Bank(schema),new Bank(schema)];
    for (int i = 0; i < 2; i++) {
        readers[i].nextEvent(event);
        event.read(banks[i]);
    }
    event.read(run);

    if (banks[0].getRows() != banks[1].getRows()) {
        // System.out.println("different number of rows");
        // run.show(); banks[0].show(); banks[1].show();
        nbadevent++;
        continue;
    }
    for (int i = 0; i < banks[0].getRows(); i++) {
        boolean mismatch = false;
        nrow++;
        // TODO. See why checkTrack is not working anymore...
        // if (!(checkTrack(banks[0], i) && checkTrack(banks[1], i))) continue;

        // Check tracks.
        for (int j = 0; j < schema.getElements(); j++) {
            int      type = schema.getType(j);
            String   name = schema.getElementName(j);
            int   element = -1;
            nentry++;
            if (name.equals("c_ux"))
                hi_dpx.fill(banks[0].getFloat(name, i) - banks[1].getFloat(name, i));
            if (name.equals("c_uy"))
                hi_dpy.fill(banks[0].getFloat(name, i) - banks[1].getFloat(name, i));
            if (name.equals("c_uz"))
                hi_dpz.fill(banks[0].getFloat(name, i) - banks[1].getFloat(name, i));
            if (name.equals("c_x"))
                hi_dvx.fill(banks[0].getFloat(name, i) - banks[1].getFloat(name, i));
            if (name.equals("c_y"))
                hi_dvy.fill(banks[0].getFloat(name, i) - banks[1].getFloat(name, i));
            if (name.equals("c_z"))
                hi_dvz.fill(banks[0].getFloat(name, i) - banks[1].getFloat(name, i));
            switch (type) {
                case 1:
                    if (banks[0].getByte(name, i) != banks[1].getByte(name, i))
                        element = j;
                    break;
                case 2:
                    if (banks[0].getShort(name, i) != banks[1].getShort(name, i))
                        element = j;
                    break;
                case 3:
                    if (banks[0].getInt(name, i) != banks[1].getInt(name, i))
                        element = j;
                    break;
                case 4:
                    if (Math.abs(banks[0].getFloat(name, i) - banks[1].getFloat(name, i)) > res)
                        element = j;
                    break;
            }
            if (element >= 0) {
                // System.out.printf("value mismatch at row " + i + ", ")
                // System.out.printf(schema.getElementName(element) + "\n");
                mismatch = true;
                nbadentry++;
                if (badEntries.containsKey(schema.getElementName(element))) {
                    int nbad = badEntries.get(schema.getElementName(element)) + 1;
                    badEntries.replace(schema.getElementName(element), nbad);
                }
                else {
                    badEntries.put(schema.getElementName(element), 1);
                }
            }
        }
        if (mismatch) {
            nbadrow++;
            // run.show(); banks[0].show(); banks[1].show();
            hi_chi2_bad.fill(Math.max(
                    banks[0].getFloat("chi2", i) / banks[0].getShort("ndf", i),
                    banks[1].getFloat("chi2", i) / banks[1].getShort("ndf", i)));
            hi_ndf_bad.fill(banks[0].getShort("ndf", i));
        }
        hi_chi2_all.fill(Math.max(
                banks[0].getFloat("chi2", i) / banks[0].getShort("ndf", i),
                banks[1].getFloat("chi2", i) / banks[1].getShort("ndf", i)));
        hi_ndf_all.fill(banks[0].getShort("ndf", i));
    }
}

// Print out summary.
System.out.println("Analyzed " + nevent + " with " + nbadevent + " bad banks");
System.out.println(nbadrow + "/" + nrow + " mismtached rows");
System.out.println(nbadentry + "/" + nentry + " mismtached entry");
for (String name : badEntries.keySet()) {
    System.out.println(name + " " + badEntries.get(name));
}

// Draw plots.
TCanvas plots = new TCanvas("plots", 1200, 800);
plots.divide(4, 2);
plots.getCanvas().setGridX(false); plots.getCanvas().setGridY(false);
plots.getCanvas().setAxisFontSize(18);
plots.getCanvas().setAxisTitleSize(24);
plots.cd(0);
plots.getCanvas().getPad(0).getAxisY().setLog(true);
plots.draw(hi_dpx);
plots.cd(1);
plots.getCanvas().getPad(1).getAxisY().setLog(true);
plots.draw(hi_dpy);
plots.cd(2);
plots.getCanvas().getPad(2).getAxisY().setLog(true);
plots.draw(hi_dpz);
plots.cd(3);
plots.getCanvas().getPad(3).getAxisY().setLog(true);
plots.draw(hi_chi2_all);
plots.draw(hi_chi2_bad,"same");
plots.cd(4);
plots.getCanvas().getPad(4).getAxisY().setLog(true);
plots.draw(hi_dvx);
plots.cd(5);
plots.getCanvas().getPad(5).getAxisY().setLog(true);
plots.draw(hi_dvy);
plots.cd(6);
plots.getCanvas().getPad(6).getAxisY().setLog(true);
plots.draw(hi_dvz);
plots.cd(7);
plots.getCanvas().getPad(7).getAxisY().setLog(true);
plots.draw(hi_ndf_all);
plots.draw(hi_ndf_bad,"same");

public boolean checkTrack(Bank tbank, int row) {
    double px   = tbank.getFloat("u_x",row);
    double py   = tbank.getFloat("u_y",row);
    double pz   = tbank.getFloat("u_z",row);
    double chi2 = tbank.getFloat("chi2",row);
    int ndf     = tbank.getShort("ndf",row);
    double p = Math.sqrt(px*px+py*py+pz*pz);
    if (p > 1 && p < 10 && chi2 < 2000 && ndf > 25) return true;
    else                                            return false;
}
