package edu.illinois.cs.cogcomp.nlp.util;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 12/21/16.
 */
public class TransitivityTriplets{
    private Triplet<TlinkType,TlinkType,TlinkType[]> triplet;
    public TransitivityTriplets(TlinkType first, TlinkType second, TlinkType[] third) {
        triplet = new Triplet<TlinkType,TlinkType,TlinkType[]>(first,second,third);
    }

    public TlinkType getFirst(){
        return triplet.getFirst();
    }
    public TlinkType getSecond(){
        return triplet.getSecond();
    }
    public TlinkType[] getThird(){
        return triplet.getThird();
    }
    public static List<TransitivityTriplets> transTriplets(){
        List<TransitivityTriplets> triplets = new ArrayList<>();
        triplets.add(new TransitivityTriplets(TlinkType.BEFORE,TlinkType.BEFORE,new TlinkType[]{TlinkType.BEFORE}));
        //triplets.add(new TransitivityTriplets(TlinkType.BEFORE,TlinkType.AFTER,new TlinkType[]{TlinkType.BEFORE}));//all possible
        triplets.add(new TransitivityTriplets(TlinkType.BEFORE,TlinkType.INCLUDES,new TlinkType[]{TlinkType.BEFORE,TlinkType.INCLUDES,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.BEFORE,TlinkType.IS_INCLUDED,new TlinkType[]{TlinkType.BEFORE,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.BEFORE,TlinkType.EQUAL,new TlinkType[]{TlinkType.BEFORE}));
        triplets.add(new TransitivityTriplets(TlinkType.BEFORE,TlinkType.UNDEF,new TlinkType[]{TlinkType.BEFORE,TlinkType.INCLUDES,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));

        //triplets.add(new TransitivityTriplets(TlinkType.AFTER,TlinkType.BEFORE,new TlinkType[]{TlinkType.BEFORE}));//all possible
        triplets.add(new TransitivityTriplets(TlinkType.AFTER,TlinkType.AFTER,new TlinkType[]{TlinkType.AFTER}));
        triplets.add(new TransitivityTriplets(TlinkType.AFTER,TlinkType.INCLUDES,new TlinkType[]{TlinkType.AFTER,TlinkType.INCLUDES,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.AFTER,TlinkType.IS_INCLUDED,new TlinkType[]{TlinkType.AFTER,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.AFTER,TlinkType.EQUAL,new TlinkType[]{TlinkType.AFTER}));
        triplets.add(new TransitivityTriplets(TlinkType.AFTER,TlinkType.UNDEF,new TlinkType[]{TlinkType.AFTER,TlinkType.INCLUDES,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));

        triplets.add(new TransitivityTriplets(TlinkType.INCLUDES,TlinkType.BEFORE,new TlinkType[]{TlinkType.BEFORE,TlinkType.INCLUDES,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.INCLUDES,TlinkType.AFTER,new TlinkType[]{TlinkType.AFTER,TlinkType.INCLUDES,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.INCLUDES,TlinkType.INCLUDES,new TlinkType[]{TlinkType.INCLUDES}));
        //triplets.add(new TransitivityTriplets(TlinkType.INCLUDES,TlinkType.IS_INCLUDED,new TlinkType[]{TlinkType.BEFORE}));//all possible
        triplets.add(new TransitivityTriplets(TlinkType.INCLUDES,TlinkType.EQUAL,new TlinkType[]{TlinkType.INCLUDES}));
        triplets.add(new TransitivityTriplets(TlinkType.INCLUDES,TlinkType.UNDEF,new TlinkType[]{TlinkType.BEFORE,TlinkType.AFTER,TlinkType.INCLUDES,TlinkType.UNDEF}));

        triplets.add(new TransitivityTriplets(TlinkType.IS_INCLUDED,TlinkType.BEFORE,new TlinkType[]{TlinkType.BEFORE,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.IS_INCLUDED,TlinkType.AFTER,new TlinkType[]{TlinkType.AFTER,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));
        //triplets.add(new TransitivityTriplets(TlinkType.IS_INCLUDED,TlinkType.INCLUDES,new TlinkType[]{TlinkType.BEFORE}));//all possible
        triplets.add(new TransitivityTriplets(TlinkType.IS_INCLUDED,TlinkType.IS_INCLUDED,new TlinkType[]{TlinkType.IS_INCLUDED}));
        triplets.add(new TransitivityTriplets(TlinkType.IS_INCLUDED,TlinkType.EQUAL,new TlinkType[]{TlinkType.IS_INCLUDED}));
        triplets.add(new TransitivityTriplets(TlinkType.IS_INCLUDED,TlinkType.UNDEF,new TlinkType[]{TlinkType.BEFORE,TlinkType.AFTER,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));

        triplets.add(new TransitivityTriplets(TlinkType.EQUAL,TlinkType.BEFORE,new TlinkType[]{TlinkType.BEFORE}));
        triplets.add(new TransitivityTriplets(TlinkType.EQUAL,TlinkType.AFTER,new TlinkType[]{TlinkType.AFTER}));
        triplets.add(new TransitivityTriplets(TlinkType.EQUAL,TlinkType.INCLUDES,new TlinkType[]{TlinkType.INCLUDES}));
        triplets.add(new TransitivityTriplets(TlinkType.EQUAL,TlinkType.IS_INCLUDED,new TlinkType[]{TlinkType.IS_INCLUDED}));
        triplets.add(new TransitivityTriplets(TlinkType.EQUAL,TlinkType.EQUAL,new TlinkType[]{TlinkType.EQUAL}));
        triplets.add(new TransitivityTriplets(TlinkType.EQUAL,TlinkType.UNDEF,new TlinkType[]{TlinkType.UNDEF}));

        triplets.add(new TransitivityTriplets(TlinkType.UNDEF,TlinkType.BEFORE,new TlinkType[]{TlinkType.BEFORE,TlinkType.INCLUDES,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.UNDEF,TlinkType.AFTER,new TlinkType[]{TlinkType.AFTER,TlinkType.INCLUDES,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.UNDEF,TlinkType.INCLUDES,new TlinkType[]{TlinkType.BEFORE,TlinkType.AFTER,TlinkType.INCLUDES,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.UNDEF,TlinkType.IS_INCLUDED,new TlinkType[]{TlinkType.BEFORE,TlinkType.AFTER,TlinkType.IS_INCLUDED,TlinkType.UNDEF}));
        triplets.add(new TransitivityTriplets(TlinkType.UNDEF,TlinkType.EQUAL,new TlinkType[]{TlinkType.UNDEF}));
        //triplets.add(new TransitivityTriplets(TlinkType.UNDEF,TlinkType.UNDEF,new TlinkType[]{TlinkType.UNDEF}));//all possible
        return triplets;
    }
}
