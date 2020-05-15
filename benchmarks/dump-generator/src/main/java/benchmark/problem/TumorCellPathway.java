package benchmark.problem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A simple Boolean network with 20 variables based on a model of a tumor cell pathway (http://ginsim.org/node/191).
 */
public class TumorCellPathway extends BooleanNetwork {

    // Indices of individual variables:

    private static final int GF = 0;
    private static final int EMTreg = 1;
    private static final int Ecadh = 2;
    private static final int Notch_pthw = 3;
    private static final int WNT_pthw = 4;
    private static final int p53 = 5;
    private static final int miRNA = 6;
    private static final int AKT2 = 7;
    private static final int Migration = 8;
    private static final int Invasion = 9;
    private static final int EMT = 10;
    private static final int p63_73 = 11;
    private static final int AKT1 = 12;
    private static final int ERK_pthw = 13;
    private static final int Metastasis = 14;
    private static final int TGFb_pthw = 15;
    private static final int DNAdamage = 16;
    private static final int ECMicroenv = 17;
    private static final int Apoptosis = 18;
    private static final int CCA = 19;

    protected TumorCellPathway() {
        super(20);
    }

    @Override
    boolean getNewValueForVariable(State state, int variable) {
        switch (variable) {
            case GF:
                return (((!state.getValue(GF) && state.getValue(EMTreg)) && !state.getValue(Ecadh)) || (state.getValue(GF) && !state.getValue(Ecadh)));
            case EMTreg:
                return ((((((!state.getValue(Notch_pthw) && !state.getValue(WNT_pthw)) && !state.getValue(p53)) && state.getValue(EMTreg)) && !state.getValue(miRNA)) || (((!state.getValue(Notch_pthw) && state.getValue(WNT_pthw)) && !state.getValue(p53)) && !state.getValue(miRNA))) || ((state.getValue(Notch_pthw) && !state.getValue(p53)) && !state.getValue(miRNA)));
            case Ecadh:
                return (!state.getValue(EMTreg) && !state.getValue(AKT2));
            case Migration:
                return ((((((state.getValue(Invasion) && state.getValue(EMT)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && state.getValue(AKT2)) && state.getValue(ERK_pthw)) && !state.getValue(miRNA));
            case Metastasis:
                return state.getValue(Migration);
            case Invasion:
                return (((!state.getValue(TGFb_pthw) && state.getValue(WNT_pthw)) || ((state.getValue(TGFb_pthw) && !state.getValue(WNT_pthw)) && state.getValue(EMTreg))) || (state.getValue(TGFb_pthw) && state.getValue(WNT_pthw)));
            case EMT:
                return (state.getValue(EMTreg) && !state.getValue(Ecadh));
            case p63_73:
                return (((((state.getValue(DNAdamage) && !state.getValue(Notch_pthw)) && !state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(AKT1)) && !state.getValue(AKT2));
            case AKT1:
                return ((((((((((!state.getValue(GF) && !state.getValue(TGFb_pthw)) && !state.getValue(Notch_pthw)) && state.getValue(WNT_pthw)) && !state.getValue(p53)) && state.getValue(EMTreg)) && !state.getValue(miRNA)) && !state.getValue(Ecadh)) || ((((((!state.getValue(GF) && !state.getValue(TGFb_pthw)) && state.getValue(Notch_pthw)) && state.getValue(WNT_pthw)) && !state.getValue(p53)) && !state.getValue(miRNA)) && !state.getValue(Ecadh))) || (((((!state.getValue(GF) && state.getValue(TGFb_pthw)) && state.getValue(WNT_pthw)) && !state.getValue(p53)) && !state.getValue(miRNA)) && !state.getValue(Ecadh))) || ((((state.getValue(GF) && state.getValue(WNT_pthw)) && !state.getValue(p53)) && !state.getValue(miRNA)) && !state.getValue(Ecadh)));
            case AKT2:
                return ((!state.getValue(p53) && state.getValue(EMTreg)) && !state.getValue(miRNA));
            case ERK_pthw:
                return (((((((!state.getValue(GF) && !state.getValue(TGFb_pthw)) && !state.getValue(Notch_pthw)) && state.getValue(EMTreg)) && !state.getValue(AKT1)) || (((!state.getValue(GF) && !state.getValue(TGFb_pthw)) && state.getValue(Notch_pthw)) && !state.getValue(AKT1))) || ((!state.getValue(GF) && state.getValue(TGFb_pthw)) && !state.getValue(AKT1))) || (state.getValue(GF) && !state.getValue(AKT1)));
            case miRNA:
                return (((((!state.getValue(p53) && !state.getValue(EMTreg)) && state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(AKT2)) || (((state.getValue(p53) && !state.getValue(EMTreg)) && !state.getValue(AKT1)) && !state.getValue(AKT2)));
            case TGFb_pthw:
                return ((((!state.getValue(ECMicroenv) && state.getValue(Notch_pthw)) && !state.getValue(WNT_pthw)) && !state.getValue(miRNA)) || ((state.getValue(ECMicroenv) && !state.getValue(WNT_pthw)) && !state.getValue(miRNA)));
            case WNT_pthw:
                return (((((((!state.getValue(Notch_pthw) && !state.getValue(WNT_pthw)) && !state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(miRNA)) && !state.getValue(Ecadh));
            case p53:
                return ((((((((!state.getValue(DNAdamage) && !state.getValue(Notch_pthw)) && state.getValue(WNT_pthw)) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(AKT2)) || (((((!state.getValue(DNAdamage) && state.getValue(Notch_pthw)) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(AKT2))) || ((((state.getValue(DNAdamage) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(AKT2)));
            case Apoptosis:
                return (((((((!state.getValue(p53) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(ERK_pthw)) && state.getValue(miRNA)) || ((((!state.getValue(p53) && !state.getValue(EMTreg)) && state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(ERK_pthw))) || (((state.getValue(p53) && !state.getValue(EMTreg)) && !state.getValue(AKT1)) && !state.getValue(ERK_pthw)));
            case CCA:
                return ((((((((((((((((((((((((((!state.getValue(TGFb_pthw) && !state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(AKT2)) && state.getValue(miRNA)) || ((((((!state.getValue(TGFb_pthw) && !state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && state.getValue(AKT2)) && !state.getValue(ERK_pthw))) || (((((((!state.getValue(TGFb_pthw) && !state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && state.getValue(AKT2)) && state.getValue(ERK_pthw)) && state.getValue(miRNA))) || (((((!state.getValue(TGFb_pthw) && !state.getValue(p53)) && !state.getValue(EMTreg)) && state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(ERK_pthw))) || ((((((!state.getValue(TGFb_pthw) && !state.getValue(p53)) && !state.getValue(EMTreg)) && state.getValue(p63_73)) && !state.getValue(AKT1)) && state.getValue(ERK_pthw)) && state.getValue(miRNA))) || (((!state.getValue(TGFb_pthw) && !state.getValue(p53)) && state.getValue(EMTreg)) && !state.getValue(AKT1))) || ((((!state.getValue(TGFb_pthw) && state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(AKT1)) && !state.getValue(ERK_pthw))) || (((((!state.getValue(TGFb_pthw) && state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(AKT1)) && state.getValue(ERK_pthw)) && state.getValue(miRNA))) || (((!state.getValue(TGFb_pthw) && state.getValue(p53)) && state.getValue(EMTreg)) && !state.getValue(AKT1))) || (((((((state.getValue(TGFb_pthw) && !state.getValue(Notch_pthw)) && !state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(AKT2)) && state.getValue(miRNA))) || (((((((state.getValue(TGFb_pthw) && !state.getValue(Notch_pthw)) && !state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && state.getValue(AKT2)) && !state.getValue(ERK_pthw))) || ((((((((state.getValue(TGFb_pthw) && !state.getValue(Notch_pthw)) && !state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(p63_73)) && !state.getValue(AKT1)) && state.getValue(AKT2)) && state.getValue(ERK_pthw)) && state.getValue(miRNA))) || ((((((state.getValue(TGFb_pthw) && !state.getValue(Notch_pthw)) && !state.getValue(p53)) && !state.getValue(EMTreg)) && state.getValue(p63_73)) && !state.getValue(AKT1)) && !state.getValue(ERK_pthw))) || (((((((state.getValue(TGFb_pthw) && !state.getValue(Notch_pthw)) && !state.getValue(p53)) && !state.getValue(EMTreg)) && state.getValue(p63_73)) && !state.getValue(AKT1)) && state.getValue(ERK_pthw)) && state.getValue(miRNA))) || ((((state.getValue(TGFb_pthw) && !state.getValue(Notch_pthw)) && !state.getValue(p53)) && state.getValue(EMTreg)) && !state.getValue(AKT1))) || (((((state.getValue(TGFb_pthw) && !state.getValue(Notch_pthw)) && state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(AKT1)) && !state.getValue(ERK_pthw))) || ((((((state.getValue(TGFb_pthw) && !state.getValue(Notch_pthw)) && state.getValue(p53)) && !state.getValue(EMTreg)) && !state.getValue(AKT1)) && state.getValue(ERK_pthw)) && state.getValue(miRNA))) || ((((state.getValue(TGFb_pthw) && !state.getValue(Notch_pthw)) && state.getValue(p53)) && state.getValue(EMTreg)) && !state.getValue(AKT1))) || ((((state.getValue(TGFb_pthw) && state.getValue(Notch_pthw)) && !state.getValue(EMTreg)) && !state.getValue(AKT1)) && !state.getValue(ERK_pthw))) || (((((state.getValue(TGFb_pthw) && state.getValue(Notch_pthw)) && !state.getValue(EMTreg)) && !state.getValue(AKT1)) && state.getValue(ERK_pthw)) && state.getValue(miRNA))) || (((state.getValue(TGFb_pthw) && state.getValue(Notch_pthw)) && state.getValue(EMTreg)) && !state.getValue(AKT1)));
            case Notch_pthw:
                return (((state.getValue(ECMicroenv) && !state.getValue(p53)) && !state.getValue(p63_73)) && !state.getValue(miRNA));
            default:
                return state.getValue(variable);    // default behaviour is no change
        }
    }

}
